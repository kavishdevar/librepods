//! Phase-2 pipe test: stream a sine tone into the LibrePodsMic virtual
//! microphone via the control device `\\.\LibrePodsMic`.
//!
//! Usage: run this, then record from "Microphone (AudioCodec Device)" in Voice
//! Recorder / Audacity — you should hear a steady tone. Proves the
//! user-mode -> IOCTL -> ring -> ACX capture path end to end.
//!
//!   lp-mic-test [freq_hz] [sample_rate]
//!   defaults: 440 Hz tone, 48000 Hz mono 16-bit (match the recording app's rate)

use std::f64::consts::PI;
use std::os::raw::c_void;

type Handle = *mut c_void;

const GENERIC_WRITE: u32 = 0x4000_0000;
const FILE_SHARE_RW: u32 = 0x0000_0003;
const OPEN_EXISTING: u32 = 3;
const INVALID_HANDLE: Handle = usize::MAX as Handle;

// CTL_CODE(FILE_DEVICE_UNKNOWN, 0x800, METHOD_BUFFERED, FILE_WRITE_DATA)
const IOCTL_LIBREPODS_MIC_WRITE_PCM: u32 = 0x0022_A000;

#[link(name = "kernel32")]
extern "system" {
    fn CreateFileW(
        name: *const u16,
        access: u32,
        share: u32,
        sec: *mut c_void,
        disp: u32,
        flags: u32,
        template: Handle,
    ) -> Handle;
    fn DeviceIoControl(
        dev: Handle,
        code: u32,
        in_buf: *const c_void,
        in_len: u32,
        out_buf: *mut c_void,
        out_len: u32,
        returned: *mut u32,
        overlapped: *mut c_void,
    ) -> i32;
    fn CloseHandle(h: Handle) -> i32;
    fn GetLastError() -> u32;
    fn Sleep(ms: u32);
}

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let freq: f64 = args.get(1).and_then(|s| s.parse().ok()).unwrap_or(440.0);
    let rate: u32 = args.get(2).and_then(|s| s.parse().ok()).unwrap_or(48_000);

    let path = wide(r"\\.\LibrePodsMic");
    let dev = unsafe {
        CreateFileW(
            path.as_ptr(),
            GENERIC_WRITE,
            FILE_SHARE_RW,
            std::ptr::null_mut(),
            OPEN_EXISTING,
            0,
            std::ptr::null_mut(),
        )
    };
    if dev == INVALID_HANDLE {
        let e = unsafe { GetLastError() };
        eprintln!(
            "Could not open \\\\.\\LibrePodsMic (error {e}). Is the LibrePodsMic \
             driver installed (install.ps1) and the virtual mic present?"
        );
        std::process::exit(1);
    }
    println!("Feeding a {freq} Hz tone at {rate} Hz mono 16-bit into the virtual mic.");
    println!("Record from \"Microphone (AudioCodec Device)\" to hear it. Ctrl+C to stop.");

    // 10 ms chunks, fed in real time so the ring tracks "now".
    let chunk_frames = (rate / 100) as usize; // 10 ms
    let mut phase: f64 = 0.0;
    let step = 2.0 * PI * freq / rate as f64;
    let amp = (i16::MAX as f64) * 0.30;
    let mut pcm: Vec<i16> = vec![0; chunk_frames];

    loop {
        for s in pcm.iter_mut() {
            *s = (phase.sin() * amp) as i16;
            phase += step;
            if phase >= 2.0 * PI {
                phase -= 2.0 * PI;
            }
        }
        let bytes = pcm.len() * 2;
        let mut returned: u32 = 0;
        let ok = unsafe {
            DeviceIoControl(
                dev,
                IOCTL_LIBREPODS_MIC_WRITE_PCM,
                pcm.as_ptr() as *const c_void,
                bytes as u32,
                std::ptr::null_mut(),
                0,
                &mut returned,
                std::ptr::null_mut(),
            )
        };
        if ok == 0 {
            let e = unsafe { GetLastError() };
            eprintln!("DeviceIoControl failed (error {e}).");
            break;
        }
        unsafe { Sleep(10) };
    }

    unsafe { CloseHandle(dev) };
}
