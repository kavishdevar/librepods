//! System output volume via Core Audio (WASAPI IAudioEndpointVolume). The daemon
//! owns volume so it's the single arbiter — it reports it in the Snapshot, serves
//! the tray's volume commands, and ducks it for Conversational Awareness without
//! an IPC round-trip. Controls the default render device (the AirPods when they
//! are the active output). This is Windows audio, independent of the AAP driver.

use windows::Win32::Media::Audio::Endpoints::IAudioEndpointVolume;
use windows::Win32::Media::Audio::{IMMDeviceEnumerator, MMDeviceEnumerator, eConsole, eRender};
use windows::Win32::System::Com::{
    CLSCTX_ALL, COINIT_MULTITHREADED, CoCreateInstance, CoInitializeEx,
};

/// Join the process MTA for the calling thread. Idempotent-safe (a second call
/// returns S_FALSE). Every daemon thread that touches volume must call this.
pub fn init() {
    unsafe {
        let _ = CoInitializeEx(None, COINIT_MULTITHREADED);
    }
}

fn endpoint() -> windows::core::Result<IAudioEndpointVolume> {
    unsafe {
        let enumerator: IMMDeviceEnumerator =
            CoCreateInstance(&MMDeviceEnumerator, None, CLSCTX_ALL)?;
        let device = enumerator.GetDefaultAudioEndpoint(eRender, eConsole)?;
        device.Activate(CLSCTX_ALL, None)
    }
}

/// Current master volume of the default output (0..=100), or None if it fails.
pub fn get() -> Option<u8> {
    let vol = endpoint().ok()?;
    let level = unsafe { vol.GetMasterVolumeLevelScalar().ok()? };
    Some((level * 100.0).round().clamp(0.0, 100.0) as u8)
}

pub fn set(percent: u8) {
    if let Ok(vol) = endpoint() {
        let level = percent.min(100) as f32 / 100.0;
        unsafe {
            let _ = vol.SetMasterVolumeLevelScalar(level, std::ptr::null());
        }
    }
}

pub fn step(delta: i32) {
    if let Some(cur) = get() {
        set((cur as i32 + delta).clamp(0, 100) as u8);
    }
}

pub fn is_muted() -> bool {
    endpoint()
        .and_then(|v| unsafe { v.GetMute() })
        .map(|b| b.as_bool())
        .unwrap_or(false)
}

pub fn toggle_mute() {
    if let Ok(vol) = endpoint() {
        let new = !unsafe { vol.GetMute() }.map(|b| b.as_bool()).unwrap_or(false);
        unsafe {
            let _ = vol.SetMute(new, std::ptr::null());
        }
    }
}

/// Conversational Awareness volume ducking. The AirPods only *signal* the state
/// (status byte of the 0x4B event) — the host lowers/restores the media volume.
/// Mirrors the app's MediaController::handle_conversational_awareness.
#[derive(Default)]
pub struct ConvDuck {
    original: Option<u8>,
    started: bool,
}

impl ConvDuck {
    /// Apply a Conversational Awareness status, Apple-style (aggressive): the
    /// media drops to a low background level so you focus on the conversation
    /// (the AirPods add the transparency/voice boost themselves). Mirrors iOS /
    /// LibrePods PR #655: 1 = start (→25%), 2 = reduce (→15%), 3 = partial
    /// (→min(original,25)), 4/6/7 = end (→restore original).
    pub fn on_status(&mut self, status: u8) {
        match status {
            1 => {
                let cur = get().unwrap_or(0);
                if !self.started {
                    self.original = Some(cur);
                    self.started = true;
                }
                if self.original.unwrap_or(cur) > 25 {
                    set(25);
                }
            }
            2 => {
                if let Some(orig) = self.original {
                    if orig > 15 {
                        set(15);
                    }
                }
            }
            3 => {
                if self.started {
                    if let Some(orig) = self.original {
                        set(orig.min(25));
                    }
                }
            }
            4 | 6 | 7 => {
                if self.started {
                    if let Some(orig) = self.original {
                        set(orig);
                    }
                    self.started = false;
                    self.original = None;
                }
            }
            _ => {}
        }
    }
}
