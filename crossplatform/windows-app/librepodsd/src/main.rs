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

use librepods_ipc::{from_line, to_line, Command, Event, Snapshot, PIPE_CMDS, PIPE_EVENTS};

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
                thread::sleep(Duration::from_secs(3));
                continue;
            }
        };
        *ctx.driver_cell.lock().unwrap() = Some(driver.clone());
        let connected = driver.connect(mac, aap::PSM_AACP).unwrap_or(false);
        log(&format!("run_receiver: connect({mac:#x}) = {connected}"));
        if !connected {
            ctx.state.lock().unwrap().connected = false;
            ctx.push_state();
            thread::sleep(Duration::from_secs(3));
            continue;
        }
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
                        ctx.push_state();
                        break; // reconnect
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

    // BLE proximity: when the AirPods advertise nearby and we're neither
    // connected nor already accepted, prompt "connect?" (debounced ~20 s).
    if mac != 0 {
        let c = ctx.clone();
        let last_prompt: Arc<Mutex<Option<Instant>>> = Arc::new(Mutex::new(None));
        thread::spawn(move || {
            le::watch_nearby(move || {
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
            });
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
