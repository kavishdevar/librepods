//! IPC client to `librepodsd`: connects to the named pipe (spawning the daemon
//! if it isn't running yet), receives State/Overlay events into shared state, and
//! sends commands. The tray no longer touches the driver — the daemon owns it.

use std::sync::{Arc, Mutex};
use std::{ptr, thread, time::Duration};

use librepods_ipc::{from_line, to_line, ClientKind, Command, Event, Snapshot, PIPE_NAME};

use windows_sys::Win32::Foundation::{
    CloseHandle, GENERIC_READ, GENERIC_WRITE, HANDLE, INVALID_HANDLE_VALUE,
};
use windows_sys::Win32::Storage::FileSystem::{CreateFileW, ReadFile, WriteFile, OPEN_EXISTING};

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// A pipe HANDLE we can share across threads (the reader owns it; the UI thread
/// writes commands to it). Duplex pipes allow concurrent read/write.
struct SendHandle(HANDLE);
unsafe impl Send for SendHandle {}

/// Handle to the daemon connection; `send` posts a command (dropped if offline).
#[derive(Clone)]
pub struct Client {
    handle: Arc<Mutex<Option<SendHandle>>>,
}

impl Client {
    pub fn send(&self, cmd: &Command) {
        if let Some(h) = self.handle.lock().unwrap().as_ref() {
            unsafe { write_all(h.0, to_line(cmd).as_bytes()) };
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

fn connect() -> Option<HANDLE> {
    let name = wide(PIPE_NAME);
    let h = unsafe {
        CreateFileW(
            name.as_ptr(),
            GENERIC_READ | GENERIC_WRITE,
            0,
            ptr::null(),
            OPEN_EXISTING,
            0,
            ptr::null_mut(),
        )
    };
    if h == INVALID_HANDLE_VALUE {
        None
    } else {
        Some(h)
    }
}

/// Connect, spawning the sibling `librepodsd.exe` if the pipe isn't there yet.
fn connect_or_spawn() -> Option<HANDLE> {
    if let Some(h) = connect() {
        return Some(h);
    }
    if let Ok(exe) = std::env::current_exe() {
        if let Some(dir) = exe.parent() {
            let _ = std::process::Command::new(dir.join("librepodsd.exe")).spawn();
        }
    }
    for _ in 0..40 {
        thread::sleep(Duration::from_millis(250));
        if let Some(h) = connect() {
            return Some(h);
        }
    }
    None
}

/// Start the client: spawns a thread that keeps the connection up, feeds `state`
/// from `State` events, and calls `on_overlay` for `Overlay` events. Returns a
/// `Client` for sending commands.
pub fn start(state: Arc<Mutex<Snapshot>>, on_overlay: fn(&str, &str)) -> Client {
    let handle: Arc<Mutex<Option<SendHandle>>> = Arc::new(Mutex::new(None));
    let slot = handle.clone();
    thread::spawn(move || loop {
        let h = match connect_or_spawn() {
            Some(h) => h,
            None => {
                thread::sleep(Duration::from_secs(1));
                continue;
            }
        };
        *slot.lock().unwrap() = Some(SendHandle(h));
        unsafe {
            write_all(h, to_line(&Command::Hello { kind: ClientKind::Tray }).as_bytes());
        }
        let mut buf = [0u8; 8192];
        let mut acc = String::new();
        loop {
            let mut read = 0u32;
            let ok =
                unsafe { ReadFile(h, buf.as_mut_ptr(), buf.len() as u32, &mut read, ptr::null_mut()) };
            if ok == 0 || read == 0 {
                break; // daemon gone
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
        *slot.lock().unwrap() = None;
        unsafe { CloseHandle(h) };
        thread::sleep(Duration::from_millis(500)); // then reconnect / respawn
    });
    Client { handle }
}
