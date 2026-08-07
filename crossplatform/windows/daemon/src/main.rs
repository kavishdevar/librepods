//! librepodsd — the LibrePods Windows daemon. Owns the exclusive AAP driver
//! handle, the AAP session, and the hi-res mic pipeline, and serves the tray /
//! full app over a named-pipe IPC (NDJSON). See ../../daemon-ipc/PLAN.md.
//! Runs headless — no console window (it's spawned by the tray/app).

#![windows_subsystem = "windows"]
#![allow(dead_code)]

mod a2dp;
mod aap;
mod bt;
mod driver;
mod eld;
mod le;
mod media;
mod micpipe;
mod rename;

use std::ptr;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::{Duration, Instant};

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
    /// The user accepted the "connect?" prompt — the session may start.
    connect_requested: Arc<AtomicBool>,
    pipe: Option<Arc<micpipe::MicPipe>>,
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

fn apply_command(ctx: &Ctx, cmd: Command) {
    log(&format!("cmd received: {cmd:?}"));
    match cmd {
        Command::Hello { .. } | Command::GetState => ctx.push_state(),
        Command::SetAnc { mode } => {
            if (1..=4).contains(&mode) {
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
        Command::Connect => {
            // The user accepted the prompt — let the session start.
            ctx.connect_requested.store(true, Ordering::Relaxed);
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
    media::init(); // COM (MTA) for the SMTC ear-detection auto-pause
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

        let mut we_paused = false;
        let mut last_status = Instant::now();
        let mut status_fails = 0u32;
        loop {
            let mut got_data = false;
            if let Ok(n) = driver.recv(2000, &mut buf) {
                if n > 0 {
                    got_data = true;
                    let data = &buf[..n];
                    // Forward the raw packet to the full app (if attached) so it
                    // runs its own AAP session over us.
                    ctx.forward_l2cap(data);
                    // Hi-res mic: decode the 0x58 uplink AUs → feed the virtual mic.
                    if ctx.mic_on.load(Ordering::Relaxed) {
                        if aap::is_audio_packet(data) {
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
                            }
                            if b.right.is_some() {
                                s.battery.right = b.right;
                            }
                            if b.case.is_some() {
                                s.battery.case = b.case;
                            }
                            if b.headphone.is_some() {
                                s.battery.headphone = b.headphone;
                            }
                            let present = s.battery.case.is_some();
                            (battery_text(&s.battery, s.connected), present)
                        };
                        ctx.push_state();
                        if pending_card {
                            ctx.overlay(&batt_text);
                            pending_card = false;
                        } else if last_case_present.is_some_and(|prev| prev != present) {
                            let ev = if present { "Case opened" } else { "Case closed" };
                            ctx.overlay(&format!("{ev}  ·  {batt_text}"));
                        }
                        last_case_present = Some(present);
                    }
                    if let Some(m) = aap::parse_anc_mode(data) {
                        ctx.cache_replay(1, data); // replay to a newly-attached app
                        ctx.state.lock().unwrap().anc = m;
                        ctx.push_state();
                        if last_anc != 0 && m != last_anc {
                            ctx.overlay(aap::anc_name(m));
                        }
                        last_anc = m;
                    }
                    if let Some((primary, secondary)) = aap::parse_ear_detection(data) {
                        let wearing = primary.in_ear() || secondary.in_ear();
                        if !wearing {
                            if media::is_playing() {
                                media::pause();
                                we_paused = true;
                            }
                        } else if we_paused {
                            media::play();
                            we_paused = false;
                        }
                    }
                }
            }
            if !got_data {
                // Tight while streaming (no ring underrun), throttle hard on idle.
                let nap = if ctx.mic_on.load(Ordering::Relaxed) { 4 } else { 150 };
                thread::sleep(Duration::from_millis(nap));
            }
            if last_status.elapsed() >= Duration::from_secs(1) {
                last_status = Instant::now();
                if driver.status().map(|s| s == 2).unwrap_or(false) {
                    status_fails = 0;
                } else {
                    status_fails += 1;
                    if status_fails >= 3 {
                        ctx.state.lock().unwrap().connected = false;
                        *ctx.driver_cell.lock().unwrap() = None;
                        last_anc = 0;
                        last_case_present = None;
                        // Released: never reconnect on our own (that would steal
                        // them back from the iPhone) — wait for a fresh prompt.
                        ctx.connect_requested.store(false, Ordering::Relaxed);
                        ctx.push_state();
                        break;
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
        connect_requested: Arc::new(AtomicBool::new(false)),
        pipe,
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

    log("threads spawned; serving events + cmds pipes");
    loop {
        thread::sleep(Duration::from_secs(3600));
    }
}
