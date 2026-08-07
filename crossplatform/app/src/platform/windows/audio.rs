//! Windows audio routing.
//!
//! There is no A2DP *profile* to toggle on Windows — the OS manages the codec
//! itself — so `activate/deactivate_a2dp` are no-ops. Volume goes through Core
//! Audio (WASAPI `IAudioEndpointVolume`) on the default render endpoint, which
//! is the AirPods when they are the active output. The `mac` is ignored: we
//! control whatever the current default output is (same model as the tray app).

use crate::platform::AudioRouter;
use std::sync::Arc;
use windows::Win32::Media::Audio::Endpoints::IAudioEndpointVolume;
use windows::Win32::Media::Audio::{IMMDeviceEnumerator, MMDeviceEnumerator, eConsole, eRender};
use windows::Win32::System::Com::{
    CLSCTX_ALL, COINIT_MULTITHREADED, CoCreateInstance, CoInitializeEx,
};

pub fn audio_router() -> Arc<dyn AudioRouter> {
    Arc::new(WindowsAudioRouter)
}

pub struct WindowsAudioRouter;

fn ensure_com() {
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

#[async_trait::async_trait]
impl AudioRouter for WindowsAudioRouter {
    async fn activate_a2dp(&self, _mac: &str) {}
    async fn deactivate_a2dp(&self, _mac: &str) {}

    async fn get_volume(&self, _mac: &str) -> Option<u32> {
        ensure_com();
        let vol = endpoint().ok()?;
        let level = unsafe { vol.GetMasterVolumeLevelScalar().ok()? };
        Some((level * 100.0).round().clamp(0.0, 100.0) as u32)
    }

    async fn set_volume(&self, _mac: &str, percent: u32) {
        ensure_com();
        if let Ok(vol) = endpoint() {
            let level = percent.min(100) as f32 / 100.0;
            unsafe {
                let _ = vol.SetMasterVolumeLevelScalar(level, std::ptr::null());
            }
        }
    }
}
