//! Windows LE advertisement scan + device connect.
//!
//! STUB (Phase I): signatures only. Real backend (WinRT
//! `BluetoothLEAdvertisementWatcher` for the Apple 0x004C manufacturer data,
//! and OS pairing/connect) lands in Phase J.

use crate::platform::{DeviceId, LeAdvertisement};
use tokio::sync::mpsc::{UnboundedReceiver, unbounded_channel};

/// Returns a receiver that stays open but never emits yet (sender leaked).
pub fn watch_le_advertisements() -> UnboundedReceiver<LeAdvertisement> {
    let (tx, rx) = unbounded_channel();
    std::mem::forget(tx);
    rx
}

/// On Windows the OS owns pairing/connection; treat as a no-op success for now.
pub async fn connect_device(_id: &DeviceId) -> Result<(), String> {
    Ok(())
}
