//! Linux system-tray spawn via ksni (StatusNotifier). Returns the ksni handle,
//! which is re-exported as `platform::TrayHandle` — callers hold that neutral
//! type and drive the tray with `handle.update(|t| ...).await`.

use crate::ui::tray::MyTray;
use ksni::TrayMethods;

pub async fn spawn_tray(tray: MyTray) -> Option<ksni::Handle<MyTray>> {
    match tray.spawn().await {
        Ok(handle) => Some(handle),
        Err(e) => {
            log::warn!("Failed to spawn system tray: {e}");
            None
        }
    }
}
