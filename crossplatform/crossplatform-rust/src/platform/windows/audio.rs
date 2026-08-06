//! Windows audio routing.
//!
//! There is no A2DP *profile* to toggle on Windows — the OS manages the codec
//! itself — so those are no-ops. Volume is a STUB for now (Phase J = WASAPI,
//! mirroring the tray app's Core Audio control).

use crate::platform::AudioRouter;
use std::sync::Arc;

pub fn audio_router() -> Arc<dyn AudioRouter> {
    Arc::new(WindowsAudioRouter)
}

pub struct WindowsAudioRouter;

#[async_trait::async_trait]
impl AudioRouter for WindowsAudioRouter {
    async fn activate_a2dp(&self, _mac: &str) {}
    async fn deactivate_a2dp(&self, _mac: &str) {}
    async fn get_volume(&self, _mac: &str) -> Option<u32> {
        None
    }
    async fn set_volume(&self, _mac: &str, _percent: u32) {}
}
