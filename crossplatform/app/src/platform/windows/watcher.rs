//! Windows adapter power + connection watcher.
//!
//! `local_adapter_address` is real (WinRT `BluetoothAdapter`). The
//! connect/disconnect watcher polls the classic Bluetooth device list (no WinRT
//! `DeviceWatcher` needed) and emits Connected/Disconnected on change.

use crate::platform::{BtConnectionEvent, DeviceId};
use std::time::Duration;
use tokio::sync::mpsc::{UnboundedReceiver, unbounded_channel};
use windows::Devices::Bluetooth::BluetoothAdapter;
use windows::Win32::System::Com::{COINIT_MULTITHREADED, CoInitializeEx};

/// The AAP SDP service UUID all AirPods advertise. We synthesize it into the
/// Connected event's uuid list so the main loop's AirPods filter matches (the
/// classic device enumeration doesn't expose service UUIDs directly).
const AAP_SERVICE_UUID: &str = "74ec2172-0bad-4d01-8f77-997b2be0722a";

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

/// Poll the connected-AirPods state and emit Connected/Disconnected on change.
/// Runs on a background task; a 3s cadence is plenty for hot-plug detection.
pub fn watch_connections() -> UnboundedReceiver<BtConnectionEvent> {
    let (tx, rx) = unbounded_channel();
    tokio::spawn(async move {
        let mut current: Option<DeviceId> = None;
        let mut ticker = tokio::time::interval(Duration::from_secs(3));
        loop {
            ticker.tick().await;
            let found = super::discovery::find_connected_airpods().await;

            let connect = |id: DeviceId, name: String| BtConnectionEvent::Connected {
                id,
                name,
                uuids: vec![AAP_SERVICE_UUID.to_string()],
            };

            let ok = match (current, &found) {
                (None, Some(dev)) => {
                    current = Some(dev.id);
                    tx.send(connect(dev.id, dev.name.clone())).is_ok()
                }
                (Some(prev), None) => {
                    current = None;
                    tx.send(BtConnectionEvent::Disconnected { id: prev }).is_ok()
                }
                (Some(prev), Some(dev)) if prev != dev.id => {
                    current = Some(dev.id);
                    tx.send(BtConnectionEvent::Disconnected { id: prev }).is_ok()
                        && tx.send(connect(dev.id, dev.name.clone())).is_ok()
                }
                _ => true,
            };
            if !ok {
                break; // consumer dropped
            }
        }
    });
    rx
}
