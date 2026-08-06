//! Windows media control.
//!
//! STUB (Phase I). The real backend is the System Media Transport Controls
//! (SMTC) — already proven in the `librepods-tray` app — and lands in Phase J.

use crate::platform::MediaControl;
use std::sync::Arc;

pub fn media_control() -> Arc<dyn MediaControl> {
    Arc::new(WindowsMediaControl)
}

pub struct WindowsMediaControl;

impl MediaControl for WindowsMediaControl {
    fn is_playing(&self) -> bool {
        false
    }
    fn pause_playing(&self) -> Vec<String> {
        Vec::new()
    }
    fn pause_all(&self) {}
    fn resume(&self, _players: &[String]) {}
    fn command(&self, _command: &str) {}
}
