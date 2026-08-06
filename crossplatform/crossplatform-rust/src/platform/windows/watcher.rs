//! Windows adapter power + connection watcher.
//!
//! STUB (Phase I): signatures only, so the crate compiles for Windows. Real
//! backend (WinRT `BluetoothAdapter` + `DeviceWatcher`) lands in Phase J.

use crate::platform::BtConnectionEvent;
use tokio::sync::mpsc::{UnboundedReceiver, unbounded_channel};

/// Windows manages the Bluetooth radio itself; nothing to power on here.
pub async fn power_on_adapter() -> Result<(), String> {
    Ok(())
}

/// Local adapter MAC. STUB (Phase J = WinRT `BluetoothAdapter.BluetoothAddress`).
pub async fn local_adapter_address() -> Option<String> {
    None
}

/// Returns a receiver that stays open but never emits yet. Leaking the sender
/// keeps the consumer's `recv().await` idle rather than terminating its loop.
pub fn watch_connections() -> UnboundedReceiver<BtConnectionEvent> {
    let (tx, rx) = unbounded_channel();
    std::mem::forget(tx);
    rx
}
