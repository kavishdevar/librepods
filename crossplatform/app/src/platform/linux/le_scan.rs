//! Linux LE advertisement source (BlueZ `bluer::monitor`) and device-connect
//! shell-out. These are the two Linux-only seams the platform-agnostic
//! `bluetooth/le.rs` consumed directly; they now live behind `platform::`.

use crate::platform::{DeviceId, LeAdvertisement};
use bluer::Session;
use bluer::monitor::{Monitor, MonitorEvent, Pattern};
use futures::StreamExt;
use log::warn;
use tokio::sync::mpsc::{UnboundedReceiver, UnboundedSender, unbounded_channel};

/// Start scanning for Apple LE advertisements. The BlueZ monitor + per-device
/// event streams run on background tasks and forward each Apple manufacturer-data
/// update over the returned channel.
pub fn watch_le_advertisements() -> UnboundedReceiver<LeAdvertisement> {
    let (tx, rx) = unbounded_channel();
    tokio::spawn(async move {
        if let Err(e) = run_le_scan(tx).await {
            warn!("LE advertisement scan stopped: {e}");
        }
    });
    rx
}

async fn run_le_scan(tx: UnboundedSender<LeAdvertisement>) -> bluer::Result<()> {
    let session = Session::new().await?;
    let adapter = session.default_adapter().await?;
    adapter.set_powered(true).await?;

    // Match manufacturer-specific data (0xFF) starting with Apple's company id
    // 0x004C (little-endian on the wire).
    let pattern = Pattern {
        data_type: 0xFF,
        start_position: 0,
        content: vec![0x4C, 0x00],
    };

    let mm = adapter.monitor().await?;
    let mut monitor_handle = mm
        .register(Monitor {
            monitor_type: bluer::monitor::Type::OrPatterns,
            patterns: Some(vec![pattern]),
            ..Default::default()
        })
        .await?;

    while let Some(mevt) = monitor_handle.next().await {
        let MonitorEvent::DeviceFound(devid) = mevt else {
            continue;
        };
        let dev = adapter.device(devid.device)?;
        let addr = dev.address();
        let mut events = dev.events().await?;
        let tx = tx.clone();
        // Each matched device gets its own task that forwards subsequent
        // ManufacturerData updates until the channel receiver is dropped.
        tokio::spawn(async move {
            while let Some(ev) = events.next().await {
                if let bluer::DeviceEvent::PropertyChanged(
                    bluer::DeviceProperty::ManufacturerData(data),
                ) = ev
                    && let Some(apple) = data.get(&76)
                {
                    let adv = LeAdvertisement {
                        address: addr,
                        apple_data: apple.clone(),
                    };
                    if tx.send(adv).is_err() {
                        break; // consumer gone
                    }
                }
            }
        });
    }
    Ok(())
}

/// Ask the OS to connect to a known device by MAC. On Linux `bluer`'s own
/// `connect()` proved unreliable for AirPods, so we shell out to `bluetoothctl`.
pub async fn connect_device(id: &DeviceId) -> Result<(), String> {
    let output = tokio::process::Command::new("bluetoothctl")
        .arg("connect")
        .arg(id.to_string())
        .output()
        .await
        .map_err(|e| e.to_string())?;
    if output.status.success() {
        Ok(())
    } else {
        Err(String::from_utf8_lossy(&output.stderr).into_owned())
    }
}
