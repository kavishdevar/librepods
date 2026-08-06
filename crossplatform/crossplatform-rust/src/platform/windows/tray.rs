//! Windows system tray.
//!
//! STUB (Phase I): no tray is spawned yet (Phase J wires the `tray-icon`
//! crate). `WindowsTrayHandle` exists so the shared `Option<TrayHandle>`
//! plumbing — and every `handle.update(|t| ...)` call site — type-checks.

use crate::ui::tray::MyTray;

#[derive(Clone)]
pub struct WindowsTrayHandle;

impl WindowsTrayHandle {
    /// Mirror of ksni's `Handle::update`. No-op until the tray-icon backend
    /// exists; accepts the same `|tray: &mut MyTray| ...` closures.
    pub async fn update<F: FnOnce(&mut MyTray)>(&self, _f: F) {}
}

pub async fn spawn_tray(_tray: MyTray) -> Option<WindowsTrayHandle> {
    None
}
