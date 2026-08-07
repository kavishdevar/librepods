//! IPC client to `librepodsd` over two one-directional named pipes: read
//! State/Overlay events from `PIPE_EVENTS`, write commands to `PIPE_CMDS`.
//! (A single duplex pipe deadlocks — a Windows sync handle serializes concurrent
//! read+write; see librepods-ipc.) Both directions are decoupled from the UI
//! thread, so a stalled pipe never freezes the tray.

use std::sync::{Arc, Mutex};
use std::{ptr, thread, time::Duration};

use librepods_ipc::{from_line, to_line, Command, Event, Snapshot, PIPE_CMDS, PIPE_EVENTS};

use windows_sys::Win32::Foundation::{
    CloseHandle, GENERIC_READ, GENERIC_WRITE, HANDLE, INVALID_HANDLE_VALUE,
};
use windows_sys::Win32::Storage::FileSystem::{CreateFileW, ReadFile, WriteFile, OPEN_EXISTING};

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

fn log(s: &str) {
    use std::io::Write;
    if let Ok(la) = std::env::var("LOCALAPPDATA") {
        if let Ok(mut f) = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(format!("{la}\\LibrePods\\tray.log"))
        {
            let _ = writeln!(f, "{s}");
        }
    }
}

fn connect_to(name: &str, access: u32) -> Option<HANDLE> {
    let w = wide(name);
    let h = unsafe {
        CreateFileW(w.as_ptr(), access, 0, ptr::null(), OPEN_EXISTING, 0, ptr::null_mut())
    };
    if h == INVALID_HANDLE_VALUE {
        None
    } else {
        Some(h)
    }
}

fn spawn_daemon() {
    if let Ok(exe) = std::env::current_exe() {
        if let Some(dir) = exe.parent() {
            let _ = std::process::Command::new(dir.join("librepodsd.exe")).spawn();
        }
    }
}

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

/// Handle to the daemon; `send` queues a command (never blocks the caller).
#[derive(Clone)]
pub struct Client {
    out: std::sync::mpsc::Sender<Vec<u8>>,
}

impl Client {
    pub fn send(&self, cmd: &Command) {
        let _ = self.out.send(to_line(cmd).into_bytes());
    }
}

/// Start the client: an events-reader thread (feeds `state`, runs overlays,
/// spawns the daemon if absent) and a commands-writer thread. Returns a `Client`
/// for sending commands.
pub fn start(state: Arc<Mutex<Snapshot>>, on_overlay: fn(&str, &str)) -> Client {
    let (out_tx, out_rx) = std::sync::mpsc::channel::<Vec<u8>>();

    // Events reader: connect to PIPE_EVENTS (spawning the daemon if needed).
    {
        thread::spawn(move || loop {
            let h = match connect_to(PIPE_EVENTS, GENERIC_READ) {
                Some(h) => {
                    log("client: events connected");
                    h
                }
                None => {
                    spawn_daemon();
                    thread::sleep(Duration::from_millis(500));
                    continue;
                }
            };
            let mut buf = [0u8; 8192];
            let mut acc = String::new();
            loop {
                let mut read = 0u32;
                let ok = unsafe {
                    ReadFile(h, buf.as_mut_ptr(), buf.len() as u32, &mut read, ptr::null_mut())
                };
                if ok == 0 || read == 0 {
                    log("client: events ended");
                    break;
                }
                acc.push_str(&String::from_utf8_lossy(&buf[..read as usize]));
                while let Some(nl) = acc.find('\n') {
                    let line: String = acc.drain(..=nl).collect();
                    match from_line::<Event>(&line) {
                        Some(Event::State(s)) => *state.lock().unwrap() = s,
                        Some(Event::Overlay { title, body }) => on_overlay(&title, &body),
                        None => {}
                    }
                }
            }
            unsafe { CloseHandle(h) };
            thread::sleep(Duration::from_millis(500));
        });
    }

    // Commands writer: connect to PIPE_CMDS, drain the outgoing queue.
    {
        thread::spawn(move || loop {
            let h = match connect_to(PIPE_CMDS, GENERIC_WRITE) {
                Some(h) => {
                    log("client: cmds connected");
                    h
                }
                None => {
                    thread::sleep(Duration::from_millis(500));
                    continue;
                }
            };
            while let Ok(msg) = out_rx.recv() {
                if !unsafe { write_all(h, &msg) } {
                    break; // reconnect
                }
            }
            unsafe { CloseHandle(h) };
            thread::sleep(Duration::from_millis(500));
        });
    }

    Client { out: out_tx }
}
