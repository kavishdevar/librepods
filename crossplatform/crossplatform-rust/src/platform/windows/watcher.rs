//! Windows adapter power + connection watcher.
//!
//! `local_adapter_address` is real (WinRT `BluetoothAdapter`). The
//! connect/disconnect watcher is still a STUB (Phase J = WinRT `DeviceWatcher`).

use crate::platform::{BtConnectionEvent, DeviceId};
use tokio::sync::mpsc::{UnboundedReceiver, unbounded_channel};
use windows::Devices::Bluetooth::BluetoothAdapter;
use windows::Win32::System::Com::{COINIT_MULTITHREADED, CoInitializeEx};

/// Windows manages the Bluetooth radio itself; nothing to power on here.
pub async fn power_on_adapter() -> Result<(), String> {
    Ok(())
}

/// The local Bluetooth adapter's MAC (our identity in the AAP hijack exchange),
/// via WinRT `BluetoothAdapter.BluetoothAddress`.
pub async fn local_adapter_address() -> Option<String> {
    tokio::task::spawn_blocking(|| {
        unsafe {
            let _ = CoInitializeEx(None, COINIT_MULTITHREADED);
        }
        let adapter = BluetoothAdapter::GetDefaultAsync().ok()?.get().ok()?;
        let addr = adapter.BluetoothAddress().ok()?;
        Some(DeviceId::from(addr).to_string())
    })
    .await
    .ok()
    .flatten()
}

/// Returns a receiver that stays open but never emits yet. Leaking the sender
/// keeps the consumer's `recv().await` idle rather than terminating its loop.
pub fn watch_connections() -> UnboundedReceiver<BtConnectionEvent> {
    let (tx, rx) = unbounded_channel();
    std::mem::forget(tx);
    rx
}
