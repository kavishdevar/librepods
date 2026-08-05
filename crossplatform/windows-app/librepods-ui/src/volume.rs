//! System output volume via Core Audio (WASAPI IAudioEndpointVolume).
//! Controls the default render device — i.e. the AirPods when they're the
//! active output. This is Windows audio, independent of the AAP driver.

use windows::Win32::Media::Audio::Endpoints::IAudioEndpointVolume;
use windows::Win32::Media::Audio::{IMMDeviceEnumerator, MMDeviceEnumerator, eConsole, eRender};
use windows::Win32::System::Com::{
    CLSCTX_ALL, COINIT_APARTMENTTHREADED, CoCreateInstance, CoInitializeEx,
};

/// Initialize COM (STA) for the calling (main/UI) thread — must be STA because
/// winit calls OleInitialize, which requires a single-threaded apartment.
/// Core Audio works fine in STA.
pub fn init() {
    unsafe {
        let _ = CoInitializeEx(None, COINIT_APARTMENTTHREADED);
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
