//! librepodsd — the LibrePods Windows daemon. Owns the exclusive AAP driver
//! handle, the AAP session, and the hi-res mic pipeline, and serves the tray /
//! full app over a named-pipe IPC (NDJSON). See ../../../docs/windows/daemon-ipc/PLAN.md.
//! Runs headless — no console window (it's spawned by the tray/app).

#![windows_subsystem = "windows"]
#![allow(dead_code)]

mod a2dp;
mod aap;
mod bt;
mod driver;
mod eld;
mod hr;
mod le;
mod media;
mod micpipe;
mod rename;
mod volume;

use std::ptr;
use std::sync::atomic::{AtomicBool, AtomicU16, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::{Duration, Instant};

use librepods_ipc as ipc;
use librepods_ipc::{
    from_line, to_line, Command, Event, Snapshot, PIPE_CMDS, PIPE_EVENTS, PIPE_L2CAP_RX,
    PIPE_L2CAP_TX,
};

use windows_sys::Win32::Foundation::{
    CloseHandle, GetLastError, ERROR_ALREADY_EXISTS, ERROR_PIPE_CONNECTED, HANDLE,
    INVALID_HANDLE_VALUE,
};
use windows_sys::Win32::Security::Authorization::ConvertStringSecurityDescriptorToSecurityDescriptorW;
use windows_sys::Win32::Security::{PSECURITY_DESCRIPTOR, SECURITY_ATTRIBUTES};
use windows_sys::Win32::Storage::FileSystem::{ReadFile, WriteFile, PIPE_ACCESS_DUPLEX};
use windows_sys::Win32::System::Pipes::{
    ConnectNamedPipe, CreateNamedPipeW, PIPE_READMODE_BYTE, PIPE_TYPE_BYTE, PIPE_UNLIMITED_INSTANCES,
    PIPE_WAIT,
};
use windows_sys::Win32::System::Threading::CreateMutexW;

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// Frame a raw AAP packet for the L2CAP proxy: u16 LE length, then the bytes.
fn frame(packet: &[u8]) -> Vec<u8> {
    let mut f = Vec::with_capacity(packet.len() + 2);
    f.extend_from_slice(&(packet.len() as u16).to_le_bytes());
    f.extend_from_slice(packet);
    f
}

fn log(s: &str) {
    use std::io::Write;
    if let Ok(la) = std::env::var("LOCALAPPDATA") {
        if let Ok(mut f) = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(format!("{la}\\LibrePods\\daemon.log"))
        {
            let _ = writeln!(f, "{s}");
        }
    }
}

fn battery_text(b: &librepods_ipc::Battery, connected: bool) -> String {
    if !connected {
        return "Disconnected".to_string();
    }
    let f = |v: Option<u8>| v.map(|p| format!("{p}%")).unwrap_or_else(|| "—".into());
    format!("Left {}   Right {}   Case {}", f(b.left), f(b.right), f(b.case))
}

/// Owns a client's pipe HANDLE; closes it once both the reader and writer
/// threads have dropped their `Arc<Pipe>`. Duplex pipes allow the reader and
/// writer to use the same handle concurrently.
struct Pipe(HANDLE);
impl Drop for Pipe {
    fn drop(&mut self) {
        unsafe { CloseHandle(self.0) };
    }
}
unsafe impl Send for Pipe {}
unsafe impl Sync for Pipe {}

/// Outgoing queue to one client (drained by its writer thread — so a slow client
/// never blocks the session/broadcast, i.e. async delivery).
type ClientTx = std::sync::mpsc::Sender<Vec<u8>>;

/// Everything the session, poll and IPC threads share.
#[derive(Clone)]
struct Ctx {
    state: Arc<Mutex<Snapshot>>,
    clients: Arc<Mutex<Vec<ClientTx>>>,
    /// Raw-L2CAP proxy clients (the full app): each incoming AAP packet is
    /// forwarded to them (length-prefixed) so the app runs its session over us.
    l2cap_clients: Arc<Mutex<Vec<ClientTx>>>,
    /// The last battery + ANC packets (raw), keyed by kind (0=battery, 1=ANC),
    /// replayed to a newly-attached app so it shows the current state without us
    /// re-requesting (which cuts audio).
    replay: Arc<Mutex<std::collections::HashMap<u8, Vec<u8>>>>,
    driver_cell: Arc<Mutex<Option<driver::Driver>>>,
    mic_on: Arc<AtomicBool>,
    auto_mode: Arc<AtomicBool>,
    /// AirPods Pro 3 heart-rate monitoring is on (opt-in — off by default). When
    /// set, `run_receiver` feeds each recv chunk into the RTBuddy HR decoder.
    hr_on: Arc<AtomicBool>,
    /// Set by `run_receiver` the moment the decoder yields its first BPM sample.
    /// The HR retry thread polls this to know an enable attempt actually took
    /// (the RTBuddy stream almost never starts on the first try) — cleared before
    /// each attempt. This is the run_receiver↔retry-thread rendezvous.
    hr_got_sample: Arc<AtomicBool>,
    /// Set by `run_receiver` when an HR-prefixed frame arrives (the HEARTRATE
    /// service is streaming, even if it carries no reading payload yet). Once the
    /// service is live the retry campaign stops churning STOP/re-init and just
    /// keeps the stream open, so a reading can land whenever the sensor produces
    /// one (it may take real sustained activity). Cleared before each attempt.
    hr_stream_live: Arc<AtomicBool>,
    /// True while an HR retry thread is live. A one-thread guard so rapid on/off
    /// never stacks two retry campaigns over the one driver.
    hr_retrying: Arc<AtomicBool>,
    /// The user accepted the "connect?" prompt — the session may start.
    connect_requested: Arc<AtomicBool>,
    pipe: Option<Arc<micpipe::MicPipe>>,
    /// Conversational Awareness volume duck — shared so `apply_command` can
    /// restore the volume if the user turns CA off mid-duck (no end event comes).
    conv_duck: Arc<Mutex<volume::ConvDuck>>,
    /// Latest rename seen on the app→driver proxy + when. Flushed to an overlay
    /// once it settles (the app may send several as you type), so there's a
    /// visible "Renamed to X" confirmation.
    pending_rename: Arc<Mutex<Option<(String, Instant)>>>,
    /// The ANC mode last commanded by the user + when. Used to ignore the
    /// transitional ANC echoes the AirPods emit while switching modes quickly
    /// (they briefly report Off), so the UI/toasts don't flicker through Off.
    anc_cmd: Arc<Mutex<Option<(u8, Instant)>>>,
    dev_name: String,
    mac: u64,
}

impl Ctx {
    /// Queue one NDJSON event for every client (never blocks — each client's
    /// writer thread drains its own queue). Drops clients whose writer has gone.
    fn send_event(&self, ev: &Event) {
        let bytes = to_line(ev).into_bytes();
        let mut clients = self.clients.lock().unwrap();
        clients.retain(|tx| tx.send(bytes.clone()).is_ok());
    }

    /// Broadcast the current state (with the live mic/auto flags folded in).
    fn push_state(&self) {
        let snap = {
            let mut s = self.state.lock().unwrap();
            s.mic_recording = self.mic_on.load(Ordering::Relaxed);
            s.auto_mode = self.auto_mode.load(Ordering::Relaxed);
            s.dev_name = self.dev_name.clone();
            s.clone()
        };
        self.send_event(&Event::State(snap));
    }

    /// Broadcast a notification for clients to render.
    fn overlay(&self, body: &str) {
        self.send_event(&Event::Overlay {
            title: self.dev_name.clone(),
            body: body.to_string(),
        });
    }

    /// Forward one raw AAP packet (length-prefixed: u16 LE + bytes) to every
    /// L2CAP-proxy client (the app). Never blocks — per-client writer threads.
    fn forward_l2cap(&self, packet: &[u8]) {
        let mut clients = self.l2cap_clients.lock().unwrap();
        if clients.is_empty() {
            return;
        }
        let f = frame(packet);
        clients.retain(|tx| tx.send(f.clone()).is_ok());
    }

    /// Remember a state packet (kind 0=battery, 1=ANC) to replay to new apps.
    fn cache_replay(&self, kind: u8, packet: &[u8]) {
        self.replay.lock().unwrap().insert(kind, packet.to_vec());
    }

    /// Read the live WASAPI volume/mute into the snapshot; push only on change so
    /// we don't spam clients. The caller's thread must have COM initialized.
    fn sync_volume(&self) {
        let vol = volume::get().unwrap_or(0);
        let muted = volume::is_muted();
        let changed = {
            let mut s = self.state.lock().unwrap();
            let c = s.volume != vol || s.muted != muted;
            s.volume = vol;
            s.muted = muted;
            c
        };
        if changed {
            self.push_state();
        }
    }
}

/// Write the whole buffer to a pipe handle. Returns false if the client is gone.
unsafe fn write_all(h: HANDLE, buf: &[u8]) -> bool {
    let mut off = 0usize;
    while off < buf.len() {
        let mut written = 0u32;
        let ok = WriteFile(
            h,
            buf[off..].as_ptr(),
            (buf.len() - off) as u32,
            &mut written,
            ptr::null_mut(),
        );
        if ok == 0 || written == 0 {
            return false;
        }
        off += written as usize;
    }
    true
}

/// Per-client reader: parse NDJSON commands and apply them until it disconnects.
fn client_reader(pipe: Arc<Pipe>, ctx: Ctx) {
    volume::init(); // this thread applies StepVolume/ToggleMute (WASAPI needs COM)
    let h = pipe.0;
    let mut buf = [0u8; 4096];
    let mut acc = String::new();
    loop {
        let mut read = 0u32;
        let ok =
            unsafe { ReadFile(h, buf.as_mut_ptr(), buf.len() as u32, &mut read, ptr::null_mut()) };
        if ok == 0 || read == 0 {
            break; // disconnected
        }
        acc.push_str(&String::from_utf8_lossy(&buf[..read as usize]));
        while let Some(nl) = acc.find('\n') {
            let line: String = acc.drain(..=nl).collect();
            if let Some(cmd) = from_line::<Command>(&line) {
                apply_command(&ctx, cmd);
            }
        }
    }
    // The pipe closes once this Arc and the writer thread's Arc both drop.
}

/// Build a security descriptor that lets same-user clients connect (the default
/// null descriptor denies them). Leaks one small SD per server — negligible.
unsafe fn pipe_sa(psd: &mut PSECURITY_DESCRIPTOR) -> SECURITY_ATTRIBUTES {
    let sddl = wide("D:(A;;GA;;;AU)(A;;GA;;;SY)");
    let ok = ConvertStringSecurityDescriptorToSecurityDescriptorW(
        sddl.as_ptr(),
        1, // SDDL_REVISION_1
        psd,
        ptr::null_mut(),
    );
    SECURITY_ATTRIBUTES {
        nLength: std::mem::size_of::<SECURITY_ATTRIBUTES>() as u32,
        lpSecurityDescriptor: if ok != 0 { *psd } else { ptr::null_mut() },
        bInheritHandle: 0,
    }
}

/// Create one pipe instance and block until a client connects; returns its handle.
unsafe fn accept(name: &[u16], sa: *const SECURITY_ATTRIBUTES) -> Option<HANDLE> {
    let h = CreateNamedPipeW(
        name.as_ptr(),
        PIPE_ACCESS_DUPLEX,
        PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT,
        PIPE_UNLIMITED_INSTANCES,
        4096,
        4096,
        0,
        sa,
    );
    if h == INVALID_HANDLE_VALUE {
        thread::sleep(Duration::from_secs(1));
        return None;
    }
    // ERROR_PIPE_CONNECTED = the client beat us to it (still a success).
    let ok = ConnectNamedPipe(h, ptr::null_mut());
    if ok == 0 && GetLastError() != ERROR_PIPE_CONNECTED {
        CloseHandle(h);
        return None;
    }
    Some(h)
}

/// Events pipe: the daemon only WRITES here (one direction → no sync-handle
/// serialization). Each client gets a queue drained by its own writer thread.
unsafe fn events_server(ctx: Ctx) {
    let name = wide(PIPE_EVENTS);
    let mut psd: PSECURITY_DESCRIPTOR = ptr::null_mut();
    let sa = pipe_sa(&mut psd);
    loop {
        let h = match accept(&name, &sa) {
            Some(h) => h,
            None => continue,
        };
        let pipe = Arc::new(Pipe(h));
        let (tx, rx) = std::sync::mpsc::channel::<Vec<u8>>();
        let n = {
            let mut cl = ctx.clients.lock().unwrap();
            cl.push(tx);
            cl.len()
        };
        log(&format!("events: client connected ({n} total)"));
        let p = pipe.clone();
        thread::spawn(move || {
            for msg in rx {
                if !unsafe { write_all(p.0, &msg) } {
                    break;
                }
            }
        });
        ctx.push_state(); // greet the newcomer
    }
}

/// Commands pipe: the daemon only READS here (one direction). Commands are
/// global, so any client's commands just apply to the daemon.
unsafe fn cmds_server(ctx: Ctx) {
    let name = wide(PIPE_CMDS);
    let mut psd: PSECURITY_DESCRIPTOR = ptr::null_mut();
    let sa = pipe_sa(&mut psd);
    loop {
        let h = match accept(&name, &sa) {
            Some(h) => h,
            None => continue,
        };
        log("cmds: client connected");
        let pipe = Arc::new(Pipe(h));
        let c = ctx.clone();
        thread::spawn(move || client_reader(pipe, c));
    }
}

/// L2CAP-RX pipe: the daemon only WRITES forwarded AAP packets here (→ the app).
unsafe fn l2cap_rx_server(ctx: Ctx) {
    let name = wide(PIPE_L2CAP_RX);
    let mut psd: PSECURITY_DESCRIPTOR = ptr::null_mut();
    let sa = pipe_sa(&mut psd);
    loop {
        let h = match accept(&name, &sa) {
            Some(h) => h,
            None => continue,
        };
        let pipe = Arc::new(Pipe(h));
        let (tx, rx) = std::sync::mpsc::channel::<Vec<u8>>();
        // Replay the cached battery/ANC packets so the app shows current state
        // immediately (without us re-requesting, which would cut audio).
        for pkt in ctx.replay.lock().unwrap().values() {
            let _ = tx.send(frame(pkt));
        }
        ctx.l2cap_clients.lock().unwrap().push(tx);
        log("l2cap-rx: app attached");
        let p = pipe.clone();
        thread::spawn(move || {
            for msg in rx {
                if !unsafe { write_all(p.0, &msg) } {
                    break;
                }
            }
        });
    }
}

/// L2CAP-TX pipe: the daemon only READS the app's outgoing AAP packets and sends
/// them to the driver — dropping the setup packets it already sent itself.
unsafe fn l2cap_tx_server(ctx: Ctx) {
    let name = wide(PIPE_L2CAP_TX);
    let mut psd: PSECURITY_DESCRIPTOR = ptr::null_mut();
    let sa = pipe_sa(&mut psd);
    loop {
        let h = match accept(&name, &sa) {
            Some(h) => h,
            None => continue,
        };
        log("l2cap-tx: app attached");
        let pipe = Arc::new(Pipe(h));
        let c = ctx.clone();
        thread::spawn(move || l2cap_reader(pipe, c));
    }
}

/// A setup packet the daemon already sent — re-sending it re-negotiates the audio
/// profile and cuts sound, so we drop the app's copy.
fn is_setup(p: &[u8]) -> bool {
    p == aap::HANDSHAKE.as_slice()
        || p == aap::SET_FEATURES.as_slice()
        || p == aap::REQUEST_NOTIFS.as_slice()
}

/// Read length-prefixed ([u16 LE len][bytes]) AAP packets from the app → driver.
fn l2cap_reader(pipe: Arc<Pipe>, ctx: Ctx) {
    let h = pipe.0;
    let mut acc: Vec<u8> = Vec::new();
    let mut buf = [0u8; 4096];
    loop {
        let mut read = 0u32;
        let ok =
            unsafe { ReadFile(h, buf.as_mut_ptr(), buf.len() as u32, &mut read, ptr::null_mut()) };
        if ok == 0 || read == 0 {
            break;
        }
        acc.extend_from_slice(&buf[..read as usize]);
        while acc.len() >= 2 {
            let len = u16::from_le_bytes([acc[0], acc[1]]) as usize;
            if acc.len() < 2 + len {
                break;
            }
            let packet = acc[2..2 + len].to_vec();
            acc.drain(..2 + len);
            if !is_setup(&packet) {
                // The app renames over the proxy; remember the latest name so we
                // can show a "Renamed to X" confirmation once typing settles.
                if let Some(name) = aap::parse_rename(&packet) {
                    *ctx.pending_rename.lock().unwrap() = Some((name, Instant::now()));
                }
                if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                    let _ = drv.send(&packet);
                }
            }
        }
    }
}

/// Enable/disable the hi-res mic stream (manual path), with the A2DP restore.
fn set_mic(ctx: &Ctx, on: bool) {
    let was = ctx.mic_on.swap(on, Ordering::Relaxed);
    if on && !was {
        if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
            let _ = drv.send(&aap::START_AUDIO);
        }
        ctx.overlay("Hi-res microphone on");
    } else if !on && was {
        if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
            let _ = drv.send(&aap::STOP_AUDIO);
        }
        ctx.overlay("Microphone released — restoring stereo…");
        a2dp::reset(ctx.mac);
        ctx.overlay("Stereo restored");
    }
    ctx.push_state();
}

// ---- HR retry constants (mirror the Android HeartRateMonitor companion) ----
/// Wait this long for the FIRST decoded sample before re-enabling.
const HR_FIRST_SAMPLE_TIMEOUT_MS: u64 = 8_000;
/// Total budget across all attempts before we give up (transport stays up).
const HR_RECONNECT_WINDOW_MS: u64 = 15_000;
/// Gap before re-sending the stream control frames on a retry.
const HR_START_COMMAND_DELAY_MS: u64 = 120;
/// Gap between the heart-rate and raw-PPG stream frames. iOS sent them 160 ms
/// apart; whether the order or the gap matters is untested.
const HR_PPG_COMMAND_DELAY_MS: u64 = 160;

/// Sequence number for sensor stream control frames. iOS increments this per
/// frame; whether the AirPods validate it is untested, so we count up too. Kept
/// above 127 so it always encodes as the two-byte varint the captures show, and
/// masked to 14 bits so it never overflows that encoding.
static HR_SEQ: AtomicU16 = AtomicU16::new(0);

fn next_hr_seq() -> u16 {
    let n = HR_SEQ.fetch_add(1, Ordering::Relaxed);
    128 + (n % (16_384 - 128))
}
/// Backoff between attempts, indexed by attempt number (last value repeats).
const HR_RETRY_BACKOFF_MS: [u64; 3] = [500, 1_000, 2_000];

/// How one retry campaign ended.
enum HrOutcome {
    /// The stream came up — a sample was decoded; run_receiver keeps decoding.
    Live,
    /// The user turned HR off mid-campaign (`hr_on` went false).
    Stopped,
    /// The 15 s window (or a lost transport) exhausted with no sample — give up.
    GiveUp,
}

/// Enable/disable AirPods Pro 3 heart-rate monitoring. On → spawn a retry thread
/// that re-sends the RTBuddy AACP 1.3 enable sequence until the first sample
/// arrives (the stream almost never starts on the first attempt). Off → send the
/// STOP frame, clear the reading, and let the retry thread notice `hr_on` and
/// exit. The decoder itself is driven in `run_receiver` off `hr_on`.
fn set_heart_rate(ctx: &Ctx, on: bool) {
    let was = ctx.hr_on.swap(on, Ordering::Relaxed);
    let has_driver = ctx.driver_cell.lock().unwrap().is_some();
    log(&format!(
        "HR: set on={on} was={was} driver={}",
        if has_driver { "connected" } else { "NONE(no session yet)" }
    ));
    if on && !was {
        spawn_hr_retry(ctx);
        ctx.overlay("Heart rate monitoring on");
    } else if !on && was {
        if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
            // Stop is the same control frame with the sampling period zeroed.
            let _ = drv.send(&aap::sensor_stream(next_hr_seq(), aap::STREAM_HEART_RATE, 0));
        }
        ctx.state.lock().unwrap().heart_rate = None;
        ctx.overlay("Heart rate monitoring off");
        // Any running retry thread observes hr_on=false on its next poll and exits.
    }
    ctx.push_state();
}

/// Start the HR retry campaign on its own thread — so the ~1 s of init sleeps and
/// the up-to-8 s sample waits never block the command reader or the recv loop.
/// Guarded by `hr_retrying` so two campaigns can't run over the one driver.
fn spawn_hr_retry(ctx: &Ctx) {
    // One-thread guard: bail if a campaign is already live.
    if ctx.hr_retrying.swap(true, Ordering::SeqCst) {
        return;
    }
    let ctx = ctx.clone();
    thread::spawn(move || {
        // Loop only to cover a rapid off→on that lost its spawn to the guard: a
        // campaign that Stopped (user off) re-runs iff hr_on is true again.
        loop {
            match hr_retry_campaign(&ctx) {
                HrOutcome::Live | HrOutcome::GiveUp => break,
                HrOutcome::Stopped => {
                    if !ctx.hr_on.load(Ordering::Relaxed) {
                        break;
                    }
                }
            }
        }
        ctx.hr_retrying.store(false, Ordering::SeqCst);
    });
}

/// One 15 s campaign: keep re-sending the enable sequence and waiting for the
/// first sample, backing off between attempts. Non-disruptive — this only
/// re-sends the AACP init + STOP frames (a service-level reset), never touching
/// the L2CAP/driver connection.
fn hr_retry_campaign(ctx: &Ctx) -> HrOutcome {
    let deadline = Instant::now() + Duration::from_millis(HR_RECONNECT_WINDOW_MS);
    let mut attempt: u32 = 0;
    while ctx.hr_on.load(Ordering::Relaxed) && Instant::now() < deadline {
        ctx.hr_got_sample.store(false, Ordering::Relaxed);
        ctx.hr_stream_live.store(false, Ordering::Relaxed);
        let drv = match ctx.driver_cell.lock().unwrap().clone() {
            Some(d) => d,
            None => {
                log("HR: no driver — retry aborted (re-arms on reconnect)");
                return HrOutcome::GiveUp;
            }
        };
        // AACP 1.3 init (connect0/caps0/connect4/caps4), then the sensor stream
        // control frames. Ground-truth captures (AAP Definitions.md → "Starting
        // and Stopping Sensor Streams") show iOS starting heart rate with a
        // `0x17` … `42 0B` frame carrying stream id 0x53 and a 1 s period, with
        // raw PPG started alongside it ~160 ms later. The init packets are kept
        // as unrefuted — every capture began with the session already open.
        let init: [(&[u8], u64); 4] = [
            (&aap::HR_CONNECT_SERVICE_0, 180),
            (&aap::HR_CAPABILITIES_SERVICE_0, 220),
            (&aap::HR_CONNECT_SERVICE_4, 180),
            (&aap::HR_CAPABILITIES_SERVICE_4, 220),
        ];
        for (pkt, delay) in init {
            if !ctx.hr_on.load(Ordering::Relaxed) {
                return HrOutcome::Stopped;
            }
            let _ = drv.send(pkt);
            thread::sleep(Duration::from_millis(delay));
        }
        // Revised enable, from the RTBuddy protobuf schema (pabloaul/apple-wireshark,
        // validated against the capture). iOS opens the Sensor Data WX service with
        // `request_all_descriptors` (a named discovery call) — twice, without then
        // with log_type — before any stream. The daemon never sent these; they are
        // the top candidate for why the computed HR (type 19) never started.
        let _ = drv.send(&aap::request_all_descriptors(next_hr_seq(), false));
        thread::sleep(Duration::from_millis(120));
        let _ = drv.send(&aap::request_all_descriptors(next_hr_seq(), true));
        thread::sleep(Duration::from_millis(220));
        // Stop head tracking (shares the sensor service), as the Android client does.
        let _ = drv.send(&aap::sensor_stream(next_hr_seq(), aap::STREAM_HEAD_TRACKING, 0));
        thread::sleep(Duration::from_millis(220));
        // Start ONLY the heart-rate stream. The 0x10 stream is DEVMOTION6 (6-axis
        // motion), NOT PPG, and unrelated to heart rate — dropped (Android doesn't
        // send it either; the ~150 frames/window we saw were motion, never PPG).
        let _ = drv.send(&aap::sensor_stream(
            next_hr_seq(),
            aap::STREAM_HEART_RATE,
            aap::PERIOD_HEART_RATE_US,
        ));

        // Wait up to FIRST_SAMPLE_TIMEOUT for the decoder to yield a sample,
        // polling so we react promptly to the user turning HR off.
        let wait_until = Instant::now() + Duration::from_millis(HR_FIRST_SAMPLE_TIMEOUT_MS);
        while Instant::now() < wait_until {
            if !ctx.hr_on.load(Ordering::Relaxed) {
                return HrOutcome::Stopped;
            }
            if ctx.hr_got_sample.load(Ordering::Relaxed) {
                log("HR: stream live (first sample decoded) — retries done");
                return HrOutcome::Live;
            }
            // Frames are arriving (HEARTRATE service is streaming) but carry no
            // reading yet — the enable worked, so stop the STOP/re-init churn and
            // keep the stream open; run_receiver keeps decoding, so a reading
            // publishes if the sensor ever produces one. NOTE: on AirPods Pro 3 the
            // sensor only emits reading frames during an active iOS HKWorkoutSession
            // (confirmed by PacketLogger RE — see HANDOFF); standalone we only ever
            // get status heartbeats. Kept correct in case a workout state carries
            // over, but do not expect readings without iOS driving the session.
            if ctx.hr_stream_live.load(Ordering::Relaxed) {
                log("HR: service streaming (frames arriving, awaiting a reading) — keeping stream open");
                return HrOutcome::Live;
            }
            thread::sleep(Duration::from_millis(200));
        }

        // No sample in 8 s — back off (capped to the remaining window) and retry.
        attempt += 1;
        log(&format!("HR retry: attempt={attempt} (no sample in 8s)"));
        let backoff = HR_RETRY_BACKOFF_MS[(attempt as usize - 1).min(HR_RETRY_BACKOFF_MS.len() - 1)];
        let remaining = deadline.saturating_duration_since(Instant::now());
        if remaining.is_zero() {
            break;
        }
        let nap_until = Instant::now() + Duration::from_millis(backoff).min(remaining);
        while Instant::now() < nap_until && ctx.hr_on.load(Ordering::Relaxed) {
            thread::sleep(Duration::from_millis(100));
        }
    }
    if !ctx.hr_on.load(Ordering::Relaxed) {
        return HrOutcome::Stopped;
    }
    log("HR: could not start after retries");
    HrOutcome::GiveUp
}

fn apply_command(ctx: &Ctx, cmd: Command) {
    log(&format!("cmd received: {cmd:?}"));
    match cmd {
        Command::Hello { .. } | Command::GetState => ctx.push_state(),
        Command::SetAnc { mode } => {
            if (1..=4).contains(&mode) {
                // Remember the target so run_receiver can ignore transitional
                // echoes (the buds briefly report Off when switching quickly), and
                // reflect the click immediately so the UI feels instant.
                *ctx.anc_cmd.lock().unwrap() = Some((mode, Instant::now()));
                ctx.state.lock().unwrap().anc = mode;
                ctx.push_state();
                if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                    let _ = drv.send(&aap::anc_command(mode));
                }
            }
        }
        Command::SetMicMode { auto, manual } => {
            ctx.auto_mode.store(auto, Ordering::Relaxed);
            if !auto {
                set_mic(ctx, manual);
            } else {
                ctx.push_state();
            }
        }
        Command::SetFeature { feature, on } => {
            if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                let _ = drv.send(&aap::feature_command(feature, on));
            }
            // Turning CA off mid-duck: no end event will arrive, so restore the
            // pre-duck volume now instead of leaving it stuck low.
            if feature == ipc::feature::CONVERSATIONAL_AWARENESS && !on {
                ctx.conv_duck.lock().unwrap().restore();
            }
            // Optimistic: reflect the toggle immediately; the AirPods echo a
            // status which run_receiver uses to correct it if it differs.
            {
                let mut s = ctx.state.lock().unwrap();
                match feature {
                    ipc::feature::CONVERSATIONAL_AWARENESS => s.conversational_awareness = on,
                    ipc::feature::ADAPTIVE_VOLUME => s.adaptive_volume = on,
                    ipc::feature::ALLOW_OFF => s.allow_off = on,
                    _ => {}
                }
            }
            ctx.push_state();
        }
        Command::SetControl { id, value } => {
            if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                let _ = drv.send(&aap::control_command(id, value));
            }
        }
        Command::StepVolume { delta } => {
            volume::step(delta);
            ctx.sync_volume();
        }
        Command::SetVolume { percent } => {
            volume::set(percent.min(100));
            ctx.sync_volume();
        }
        Command::ToggleMute => {
            volume::toggle_mute();
            ctx.sync_volume();
        }
        Command::SetHeartRate { on } => set_heart_rate(ctx, on),
        Command::Connect => {
            // The user accepted the prompt — let the session start.
            ctx.connect_requested.store(true, Ordering::Relaxed);
        }
        Command::Disconnect => {
            // Release the control session: stop the recv loop (it checks this),
            // drop the driver, mark disconnected. Never auto-reconnect (a prompt
            // is required), so we don't steal the AirPods back from the phone.
            ctx.connect_requested.store(false, Ordering::Relaxed);
            ctx.state.lock().unwrap().connected = false;
            *ctx.driver_cell.lock().unwrap() = None;
            ctx.push_state();
            ctx.overlay("Disconnected");
        }
        Command::SetName { name } => {
            if !name.is_empty() && name.len() <= 64 {
                if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                    let _ = drv.send(&aap::build_rename(&name));
                }
                ctx.overlay(&format!("Renamed to “{name}”"));
            }
        }
        Command::Shutdown => std::process::exit(0),
    }
}

/// The AAP session: keep the link up, decode the mic, track battery/ANC/ear
/// detection, and broadcast state + overlay events. (Ported from the tray.)
fn run_receiver(ctx: Ctx) {
    let mac = ctx.mac;
    log("run_receiver: entered");
    let mut buf = [0u8; 8192];
    let mut decoder: Option<eld::Decoder> = None;
    // RTBuddy heart-rate decoder (inert unless `hr_on`). Carry is reset per
    // connection so a partial frame never straddles a reconnect.
    let mut hr_decoder = hr::RtBuddyHeartRateDecoder::new();
    media::init(); // COM (MTA) for the SMTC ear-detection auto-pause
    volume::init(); // COM for the CA volume duck (same MTA)
    log("run_receiver: media init done");
    let mut last_anc = 0u8;
    let mut last_case_present: Option<bool> = None;
    let mut pending_card = false;
    // Consecutive failures to reach the AirPods. After a few (they're on the
    // iPhone / gone), we give up so we DON'T keep stealing them back — reset the
    // gate and wait for a fresh prompt/Connect.
    let mut reach_fails = 0u32;
    // The user asked to connect — keep trying for a generous window (the AirPods
    // may take a few seconds to become reachable) before giving up. We never
    // *steal* here: a failed connect() doesn't pull them, and once connected a
    // drop releases the gate (below).
    let give_up = |ctx: &Ctx, fails: &mut u32| {
        *fails += 1;
        if *fails >= 12 {
            *fails = 0;
            ctx.connect_requested.store(false, Ordering::Relaxed);
            log("run_receiver: gave up reaching AirPods — releasing");
        }
    };
    loop {
        // Gate: stay idle until the user accepts the "connect?" prompt.
        if !ctx.connect_requested.load(Ordering::Relaxed) {
            thread::sleep(Duration::from_millis(500));
            continue;
        }
        let driver = match driver::Driver::open() {
            Ok(d) => {
                log("run_receiver: driver opened");
                d
            }
            Err(_) => {
                log("run_receiver: driver open FAILED");
                ctx.state.lock().unwrap().connected = false;
                *ctx.driver_cell.lock().unwrap() = None;
                ctx.push_state();
                give_up(&ctx, &mut reach_fails);
                thread::sleep(Duration::from_millis(1500));
                continue;
            }
        };
        *ctx.driver_cell.lock().unwrap() = Some(driver.clone());
        let connected = driver.connect(mac, aap::PSM_AACP).unwrap_or(false);
        log(&format!("run_receiver: connect({mac:#x}) = {connected}"));
        if !connected {
            ctx.state.lock().unwrap().connected = false;
            ctx.push_state();
            give_up(&ctx, &mut reach_fails);
            thread::sleep(Duration::from_millis(1500));
            continue;
        }
        reach_fails = 0; // reached them — reset the give-up counter
        let _ = driver.send(&aap::HANDSHAKE);
        thread::sleep(Duration::from_millis(300));
        let _ = driver.send(&aap::SET_FEATURES);
        thread::sleep(Duration::from_millis(300));
        let _ = driver.send(&aap::REQUEST_NOTIFS);
        ctx.state.lock().unwrap().connected = true;
        pending_card = true;
        ctx.push_state();
        log("run_receiver: handshake done, connected=true");
        hr_decoder.reset(); // fresh connection — drop any stale HR carry
        // Re-arm the HR stream if the user had it on before the (re)connect —
        // through the same retry path (the stream rarely starts first try).
        if ctx.hr_on.load(Ordering::Relaxed) {
            spawn_hr_retry(&ctx);
        }

        let mut we_paused = false;
        let mut prev_ear = [false; 2]; // last [primary, secondary] in-ear state
        let mut last_status = Instant::now();
        let mut last_audio = Instant::now(); // hi-res mic watchdog (PR #655)
        let mut status_fails = 0u32;
        let mut low_warned = false; // low-battery overlay fired (hysteresis)
        let mut case_low_warned = false; // case low-battery overlay fired
        // Per-bud ear status, to notify on the transition into the case.
        let mut prev_status = [aap::EarStatus::Disconnected; 2];
        // HR diagnostics (logged every ~3s while monitoring).
        let mut hr_last_log = Instant::now();
        let (mut hr_bytes, mut hr_frames, mut hr_samples) = (0usize, 0u32, 0u32);
        // Frames carrying the type-19 heart-rate signature `08 13 1a 12` (vs the
        // 50 Hz type-16 raw-PPG flood, which shares the RTBuddy prefix).
        let mut hr_type19 = 0u32;
        let mut hr_type14 = 0u32; // head-tracking frames (sensor-service contention)
        // Diagnose stale-"connected": throttled log of the raw driver status when
        // it isn't a clean 2, so we can see what "cased" vs "both-out-resting"
        // actually report (the teardown decision hinges on them differing).
        let mut status_diag = Instant::now();
        // Last time an AAP packet actually arrived. The driver State drops to 0
        // both on a transient channel re-negotiation (e.g. both buds just left the
        // ears) and on a real disconnect (cased / on the phone) — status alone
        // can't tell them apart, so data flow is the tie-breaker.
        let mut last_data = Instant::now();
        loop {
            // The user pressed Disconnect (connect_requested cleared) — release.
            if !ctx.connect_requested.load(Ordering::Relaxed) {
                log("run_receiver: disconnect requested — releasing");
                *ctx.driver_cell.lock().unwrap() = None;
                break;
            }
            let mut got_data = false;
            if let Ok(n) = driver.recv(2000, &mut buf) {
                if n > 0 {
                    got_data = true;
                    last_data = Instant::now();
                    let data = &buf[..n];
                    // Forward the raw packet to the full app (if attached) so it
                    // runs its own AAP session over us.
                    ctx.forward_l2cap(data);
                    // Heart rate (opt-in): feed each chunk into the RTBuddy
                    // decoder; publish the latest validated BPM. Inert when off.
                    if ctx.hr_on.load(Ordering::Relaxed) {
                        hr_bytes += data.len();
                        if hr::contains_frame_prefix(data) {
                            hr_frames += 1;
                            ctx.hr_stream_live.store(true, Ordering::Relaxed);
                            // Count head-tracking (type 14: `08 0e 1a`) frames too,
                            // to see whether it's still streaming and stealing the
                            // sensor service from the computed heart rate.
                            if data.windows(3).any(|w| w == [0x08, 0x0e, 0x1a]) {
                                hr_type14 += 1;
                            }
                            // Only the 1 Hz heart-rate stream carries `08 13 1a 12`
                            // (type 19, 18-byte payload). Dump those; the 50 Hz
                            // type-16 raw-PPG frames share the prefix and would
                            // drown the log, so we only count them (hr_frames).
                            if data.windows(4).any(|w| w == [0x08, 0x13, 0x1a, 0x12]) {
                                hr_type19 += 1;
                                let dump: String = data
                                    .iter()
                                    .take(48)
                                    .map(|b| format!("{b:02x}"))
                                    .collect::<Vec<_>>()
                                    .join(" ");
                                log(&format!("HR type-19 ({} bytes): {dump}", data.len()));
                            }
                        }
                        let samples = hr_decoder.feed(data);
                        hr_samples += samples.len() as u32;
                        if !samples.is_empty() {
                            // Rendezvous with the retry thread: the stream is live.
                            ctx.hr_got_sample.store(true, Ordering::Relaxed);
                        }
                        if let Some(bpm) = samples.into_iter().last() {
                            let changed = {
                                let mut s = ctx.state.lock().unwrap();
                                let c = s.heart_rate != Some(bpm);
                                s.heart_rate = Some(bpm);
                                c
                            };
                            if changed {
                                ctx.push_state();
                            }
                        }
                        if hr_last_log.elapsed() >= Duration::from_secs(3) {
                            log(&format!(
                                "HR diag: bytes={hr_bytes} prefix={hr_frames} type14={hr_type14} type19={hr_type19} bpm_samples={hr_samples}"
                            ));
                            hr_last_log = Instant::now();
                            hr_bytes = 0;
                            hr_frames = 0;
                            hr_type19 = 0;
                            hr_type14 = 0;
                            hr_samples = 0;
                        }
                    } else if ctx.state.lock().unwrap().heart_rate.take().is_some() {
                        ctx.push_state();
                    }
                    // Hi-res mic: decode the 0x58 uplink AUs → feed the virtual mic.
                    if ctx.mic_on.load(Ordering::Relaxed) {
                        if aap::is_audio_packet(data) {
                            last_audio = Instant::now(); // watchdog: stream alive
                            if decoder.is_none() {
                                decoder = eld::Decoder::new();
                                if let Some(pp) = ctx.pipe.as_ref() {
                                    pp.write(&[0i16; 3840]); // ~80 ms cushion
                                }
                            }
                            if let (Some(dec), Some(pp)) = (decoder.as_mut(), ctx.pipe.as_ref()) {
                                let mut out: Vec<i16> = Vec::new();
                                aap::for_each_au(data, |au| out.extend_from_slice(dec.decode(au)));
                                if !out.is_empty() {
                                    pp.write(&out);
                                }
                            }
                        }
                    } else if decoder.is_some() {
                        decoder = None;
                    }
                    if let Some(b) = aap::parse_battery(data) {
                        ctx.cache_replay(0, data); // replay to a newly-attached app
                        let (batt_text, present) = {
                            let mut s = ctx.state.lock().unwrap();
                            if b.left.is_some() {
                                s.battery.left = b.left;
                                s.battery.left_charging = b.left_charging;
                            }
                            if b.right.is_some() {
                                s.battery.right = b.right;
                                s.battery.right_charging = b.right_charging;
                            }
                            if b.case.is_some() {
                                s.battery.case = b.case;
                                s.battery.case_charging = b.case_charging;
                            }
                            if b.headphone.is_some() {
                                s.battery.headphone = b.headphone;
                                s.battery.headphone_charging = b.headphone_charging;
                            }
                            let present = s.battery.case.is_some();
                            (battery_text(&s.battery, s.connected), present)
                        };
                        ctx.push_state();
                        if pending_card {
                            ctx.overlay(&format!("Connected  ·  {batt_text}"));
                            pending_card = false;
                        } else if last_case_present.is_some_and(|prev| prev != present) {
                            let ev = if present { "Case opened" } else { "Case closed" };
                            ctx.overlay(&format!("{ev}  ·  {batt_text}"));
                        }
                        last_case_present = Some(present);
                        // Low-battery notification: warn once when either bud
                        // falls to <=20%, re-arm only after it recovers above 25%
                        // (hysteresis so it doesn't spam around the threshold).
                        let low = {
                            let s = ctx.state.lock().unwrap();
                            [s.battery.left, s.battery.right].into_iter().flatten().min()
                        };
                        if let Some(min) = low {
                            if min <= 20 && !low_warned {
                                ctx.overlay(&format!("Battery low — {min}%"));
                                low_warned = true;
                            } else if min > 25 {
                                low_warned = false;
                            }
                        }
                        // Case low battery (separate, quieter threshold).
                        if let Some(cl) = { ctx.state.lock().unwrap().battery.case } {
                            if cl <= 15 && !case_low_warned {
                                ctx.overlay(&format!("Case battery low — {cl}%"));
                                case_low_warned = true;
                            } else if cl > 20 {
                                case_low_warned = false;
                            }
                        }
                    }
                    if let Some(m) = aap::parse_anc_mode(data) {
                        ctx.cache_replay(1, data); // replay to a newly-attached app
                        // Ignore transitional echoes that don't match a recent user
                        // command (the buds briefly report Off when switching modes
                        // quickly). Accept once it matches, or when nothing is
                        // pending / the window passed (an external change).
                        let accept = match *ctx.anc_cmd.lock().unwrap() {
                            Some((target, t)) if t.elapsed() < Duration::from_millis(1500) => {
                                m == target
                            }
                            _ => true,
                        };
                        if accept {
                            ctx.state.lock().unwrap().anc = m;
                            ctx.push_state();
                            if last_anc != 0 && m != last_anc {
                                ctx.overlay(aap::anc_name(m));
                            }
                            last_anc = m;
                        }
                    }
                    // Sync the feature toggles from the AirPods' own status echoes
                    // (0x01 = on, 0x02 = off), so the tray checkmarks reflect the
                    // real device state — including whatever the iPhone last set.
                    {
                        let mut changed = false;
                        let mut s = ctx.state.lock().unwrap();
                        for (id, field) in [
                            (ipc::feature::CONVERSATIONAL_AWARENESS, 0),
                            (ipc::feature::ADAPTIVE_VOLUME, 1),
                            (ipc::feature::ALLOW_OFF, 2),
                        ] {
                            if let Some(v) = aap::parse_control_value(data, id) {
                                let on = v == 0x01;
                                let slot = match field {
                                    0 => &mut s.conversational_awareness,
                                    1 => &mut s.adaptive_volume,
                                    _ => &mut s.allow_off,
                                };
                                if *slot != on {
                                    *slot = on;
                                    changed = true;
                                }
                            }
                        }
                        drop(s);
                        if changed {
                            ctx.push_state();
                        }
                    }
                    // Conversational Awareness: the AirPods signal speech start/stop;
                    // we (the host, single volume owner) duck/restore the volume.
                    if let Some(status) = aap::parse_conversational_awareness(data) {
                        // Don't duck while the hi-res mic is in use (you're on a
                        // call — you ARE talking, but the call audio shouldn't
                        // drop). Restore if a duck was already in progress.
                        if ctx.mic_on.load(Ordering::Relaxed) {
                            ctx.conv_duck.lock().unwrap().restore();
                        } else {
                            ctx.conv_duck.lock().unwrap().on_status(status);
                        }
                    }
                    if let Some((model, firmware, serial)) = aap::parse_metadata(data) {
                        // Device identity (0x1D): store model/firmware/serial once.
                        let changed = {
                            let mut s = ctx.state.lock().unwrap();
                            let c = s.model != model;
                            if c {
                                s.model = model.clone();
                                s.firmware = firmware;
                                s.serial = serial;
                            }
                            c
                        };
                        if changed {
                            log(&format!("device metadata: model={model}"));
                            ctx.push_state();
                        }
                    }
                    if let Some((primary, secondary)) = aap::parse_ear_detection(data) {
                        // "In case" notification on the transition into the case.
                        // Ear-detection reports both buds together (never partial,
                        // unlike the battery packet) and comes over AAP — so this
                        // is reliable with no BLE, hence no audio static.
                        let now = [primary, secondary];
                        for i in 0..2 {
                            if now[i] == aap::EarStatus::InCase
                                && prev_status[i] != aap::EarStatus::InCase
                            {
                                ctx.overlay("AirPod in case");
                                break; // one overlay even if both go in at once
                            }
                        }
                        prev_status = now;
                        // 0x04 is a transitional (in-motion) value — hold the prior
                        // in-ear state for that bud instead of reading it as "out",
                        // so a bud being handled doesn't trigger a false auto-pause.
                        let new_ear = [
                            if primary.is_transitional() { prev_ear[0] } else { primary.in_ear() },
                            if secondary.is_transitional() { prev_ear[1] } else { secondary.in_ear() },
                        ];
                        if new_ear != prev_ear {
                            let all_in = new_ear[0] && new_ear[1];
                            let was_wearing = prev_ear[0] || prev_ear[1];
                            if all_in {
                                // both back in the ears → resume what we paused
                                if we_paused {
                                    media::play();
                                    we_paused = false;
                                }
                            } else if was_wearing && media::is_playing() {
                                // a bud was just removed (Apple-style: pause on a
                                // single removal, not only when both are out)
                                media::pause();
                                we_paused = true;
                            }
                            prev_ear = new_ear;
                        }
                    }
                }
            }
            if !got_data {
                // Tight while streaming (no ring underrun), throttle hard on idle.
                let nap = if ctx.mic_on.load(Ordering::Relaxed) { 4 } else { 150 };
                thread::sleep(Duration::from_millis(nap));
            }
            // Hi-res mic watchdog (aligns with Linux PR #655): while capturing, the
            // AirPods should stream 0x58 uplink SDUs continuously. If they stall for
            // >2 s the mic pipe underruns into static/dropouts — re-arm the uplink
            // (STOP→START) and drop the decoder so the next packet lays a fresh
            // silence cushion. While the mic is off, keep the clock reset so it
            // never fires.
            if ctx.mic_on.load(Ordering::Relaxed) {
                if last_audio.elapsed() >= Duration::from_millis(2000) {
                    log("watchdog: mic SDUs stalled >2s — restarting uplink");
                    let _ = driver.send(&aap::STOP_AUDIO);
                    thread::sleep(Duration::from_millis(120));
                    let _ = driver.send(&aap::START_AUDIO);
                    decoder = None;
                    last_audio = Instant::now();
                }
            } else {
                last_audio = Instant::now();
            }
            if last_status.elapsed() >= Duration::from_secs(1) {
                last_status = Instant::now();
                let st = driver.status();
                if !matches!(st, Ok(2)) && status_diag.elapsed() >= Duration::from_secs(2) {
                    let both_out = !prev_ear[0] && !prev_ear[1];
                    log(&format!(
                        "status diag: st={st:?} both_out={both_out} fails={status_fails} data_age={}ms",
                        last_data.elapsed().as_millis()
                    ));
                    status_diag = Instant::now();
                }
                if matches!(st, Ok(2)) {
                    status_fails = 0;
                } else {
                    // The driver State is not "connected". That happens both on a
                    // transient channel re-negotiation (e.g. both buds just left the
                    // ears — tearing down here dropped the driver handle and churned
                    // the BT link, "disconnects and comes back") and on a real loss
                    // (cased / handed to the phone → the remote closes the channel).
                    // status can't distinguish them, so use data flow: while AAP
                    // packets are still arriving the link is alive — don't tear down.
                    // Cased/gone goes silent, so status_fails climbs to the 3s
                    // teardown.
                    let recent_data = last_data.elapsed() < Duration::from_secs(3);
                    if recent_data {
                        status_fails = 0;
                    } else {
                        status_fails += 1;
                        if status_fails >= 3 {
                            log(&format!("run_receiver: status lost 3s (st={st:?}) — releasing"));
                            ctx.overlay("Disconnected");
                            {
                                let mut s = ctx.state.lock().unwrap();
                                s.connected = false;
                                s.heart_rate = None; // stale once the link is gone
                            }
                            *ctx.driver_cell.lock().unwrap() = None;
                            hr_decoder.reset(); // drop HR carry across the reconnect
                            last_anc = 0;
                            last_case_present = None;
                            // Released: never reconnect on our own (that would
                            // steal them back from the iPhone) — wait for a prompt.
                            ctx.connect_requested.store(false, Ordering::Relaxed);
                            ctx.push_state();
                            break;
                        }
                    }
                }
            }
        }
        thread::sleep(Duration::from_secs(2));
    }
}

/// Auto-activate: enable the hi-res stream when an app records from the virtual
/// mic, disable it (debounced) when it stops, and restore A2DP stereo.
fn poll_mic(ctx: Ctx) {
    const MIC_IDLE_STOP_POLLS: u32 = 20; // 20 × 500 ms = 10 s (bridges VAD/probe gaps)
    let mut prev = ctx.pipe.as_ref().map(|p| p.status()).unwrap_or(0);
    let mut idle = 0u32;
    let mut on = false;
    loop {
        thread::sleep(Duration::from_millis(500));
        let cur = match ctx.pipe.as_ref() {
            Some(p) => p.status(),
            None => break,
        };
        let capturing = cur != prev;
        prev = cur;
        if !ctx.auto_mode.load(Ordering::Relaxed) {
            on = ctx.mic_on.load(Ordering::Relaxed);
            continue;
        }
        if capturing {
            idle = 0;
            if !on {
                on = true;
                ctx.mic_on.store(true, Ordering::Relaxed);
                if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                    let _ = drv.send(&aap::START_AUDIO);
                }
                ctx.overlay("Microphone in use — hi-res on");
                ctx.push_state();
            }
        } else {
            idle += 1;
            if on && idle >= MIC_IDLE_STOP_POLLS {
                on = false;
                ctx.mic_on.store(false, Ordering::Relaxed);
                if let Some(drv) = ctx.driver_cell.lock().unwrap().clone() {
                    let _ = drv.send(&aap::STOP_AUDIO);
                }
                ctx.overlay("Microphone released — restoring stereo…");
                a2dp::reset(ctx.mac);
                ctx.overlay("Stereo restored");
                ctx.push_state();
            }
        }
    }
}

fn main() {
    // Single instance: never run two daemons over the one exclusive driver.
    unsafe {
        let name = wide("Local\\LibrePodsDaemonSingleton");
        let _ = CreateMutexW(ptr::null(), 0, name.as_ptr());
        if GetLastError() == ERROR_ALREADY_EXISTS {
            return;
        }
    }

    log("=== librepodsd start ===");
    let (mac, dev_name) = match bt::find_airpods() {
        Some((m, n)) => (m, n),
        None => (0, "AirPods".to_string()),
    };
    log(&format!("find_airpods: mac={mac:#x} name='{dev_name}'"));

    let pipe = micpipe::MicPipe::open().map(Arc::new);
    log(&format!("mic pipe opened: {}", pipe.is_some()));
    let ctx = Ctx {
        state: Arc::new(Mutex::new(Snapshot {
            dev_name: dev_name.clone(),
            auto_mode: true,
            ..Default::default()
        })),
        clients: Arc::new(Mutex::new(Vec::new())),
        l2cap_clients: Arc::new(Mutex::new(Vec::new())),
        replay: Arc::new(Mutex::new(std::collections::HashMap::new())),
        driver_cell: Arc::new(Mutex::new(None)),
        mic_on: Arc::new(AtomicBool::new(false)),
        auto_mode: Arc::new(AtomicBool::new(true)),
        hr_on: Arc::new(AtomicBool::new(false)),
        hr_got_sample: Arc::new(AtomicBool::new(false)),
        hr_stream_live: Arc::new(AtomicBool::new(false)),
        hr_retrying: Arc::new(AtomicBool::new(false)),
        connect_requested: Arc::new(AtomicBool::new(false)),
        pipe,
        conv_duck: Arc::new(Mutex::new(volume::ConvDuck::default())),
        pending_rename: Arc::new(Mutex::new(None)),
        anc_cmd: Arc::new(Mutex::new(None)),
        dev_name: dev_name.clone(),
        mac,
    };

    // Name the virtual mic after the connected device (elevated task, no UAC).
    if mac != 0 {
        rename::apply(&dev_name);
    }

    // IPC: two one-directional pipe servers (events out, commands in).
    {
        let c = ctx.clone();
        thread::spawn(move || unsafe { events_server(c) });
    }
    {
        let c = ctx.clone();
        thread::spawn(move || unsafe { cmds_server(c) });
    }
    // Raw-L2CAP proxy for the full app (Phase 3): RX (packets → app) + TX (app → driver).
    {
        let c = ctx.clone();
        thread::spawn(move || unsafe { l2cap_rx_server(c) });
    }
    {
        let c = ctx.clone();
        thread::spawn(move || unsafe { l2cap_tx_server(c) });
    }

    // BLE proximity: when the AirPods advertise nearby and we're neither
    // connected nor already accepted, prompt "connect?" (debounced ~20 s).
    if mac != 0 {
        let c = ctx.clone();
        let c_scan = ctx.clone();
        let last_prompt: Arc<Mutex<Option<Instant>>> = Arc::new(Mutex::new(None));
        thread::spawn(move || {
            le::watch_nearby(
                move || {
                    if c.state.lock().unwrap().connected
                        || c.connect_requested.load(Ordering::Relaxed)
                    {
                        return;
                    }
                    let now = Instant::now();
                    let mut lp = last_prompt.lock().unwrap();
                    if lp.map_or(true, |t| now.duration_since(t) > Duration::from_secs(20)) {
                        *lp = Some(now);
                        drop(lp);
                        c.send_event(&Event::ConnectPrompt { name: c.dev_name.clone() });
                        log("ble: AirPods nearby → connect prompt");
                    }
                },
                // Only scan while idle (disconnected) — no BLE radio during audio.
                move || !c_scan.state.lock().unwrap().connected,
            );
        });
    }

    // AAP session + auto-activate poll (only if we have a paired device).
    if mac != 0 {
        {
            let c = ctx.clone();
            thread::spawn(move || run_receiver(c));
        }
        {
            let c = ctx.clone();
            thread::spawn(move || poll_mic(c));
        }
    }
    // Volume poller: keep the Snapshot's volume/mute fresh (the daemon owns
    // volume) so the tray renders it — runs regardless of a paired device.
    {
        let c = ctx.clone();
        thread::spawn(move || {
            volume::init();
            loop {
                c.sync_volume();
                // Flush a settled rename into a confirmation overlay.
                let ready = {
                    let mut p = c.pending_rename.lock().unwrap();
                    match p.as_ref() {
                        Some((_, t)) if t.elapsed() >= Duration::from_millis(900) => p.take(),
                        _ => None,
                    }
                };
                if let Some((name, _)) = ready {
                    c.overlay(&format!("Renamed to “{name}”"));
                }
                thread::sleep(Duration::from_millis(500));
            }
        });
    }

    log("threads spawned; serving events + cmds pipes");
    loop {
        thread::sleep(Duration::from_secs(3600));
    }
}
