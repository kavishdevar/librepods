//! Windows LE advertisement scan via WinRT `BluetoothLEAdvertisementWatcher`.
//! Forwards Apple (company id 0x004C) manufacturer data to the shared
//! `bluetooth/le.rs` decoder — the same RPA/battery/ear-detection pipeline as
//! Linux, just a different advertisement source.

use crate::platform::{DeviceId, LeAdvertisement};
use tokio::sync::mpsc::{UnboundedReceiver, UnboundedSender, unbounded_channel};
use windows::Devices::Bluetooth::Advertisement::{
    BluetoothLEAdvertisementReceivedEventArgs, BluetoothLEAdvertisementWatcher,
    BluetoothLEScanningMode,
};
use windows::Foundation::TypedEventHandler;
use windows::Storage::Streams::DataReader;
use windows::Win32::System::Com::{COINIT_MULTITHREADED, CoInitializeEx};

const APPLE_COMPANY_ID: u16 = 0x004C;

pub fn watch_le_advertisements() -> UnboundedReceiver<LeAdvertisement> {
    let (tx, rx) = unbounded_channel();
    if let Err(e) = start_watcher(tx) {
        log::warn!("Failed to start LE advertisement watcher: {e:?}");
    }
    rx
}

fn start_watcher(tx: UnboundedSender<LeAdvertisement>) -> windows::core::Result<()> {
    unsafe {
        let _ = CoInitializeEx(None, COINIT_MULTITHREADED);
    }

    let watcher = BluetoothLEAdvertisementWatcher::new()?;
    watcher.SetScanningMode(BluetoothLEScanningMode::Active)?;

    let handler = TypedEventHandler::new(
        move |_sender: &Option<BluetoothLEAdvertisementWatcher>,
              args: &Option<BluetoothLEAdvertisementReceivedEventArgs>| {
            if let Some(args) = args.as_ref()
                && let Some(adv) = extract(args)
            {
                let _ = tx.send(adv);
            }
            Ok(())
        },
    );
    watcher.Received(&handler)?;
    watcher.Start()?;

    // The watcher stops if dropped; keep it alive for the app's lifetime.
    std::mem::forget(watcher);
    Ok(())
}

/// Pull the Apple manufacturer-data payload out of one advertisement.
fn extract(args: &BluetoothLEAdvertisementReceivedEventArgs) -> Option<LeAdvertisement> {
    let addr = args.BluetoothAddress().ok()?;
    let adv = args.Advertisement().ok()?;
    let mfg = adv.ManufacturerData().ok()?;

    for i in 0..mfg.Size().ok()? {
        let md = mfg.GetAt(i).ok()?;
        if md.CompanyId().ok()? == APPLE_COMPANY_ID {
            let buf = md.Data().ok()?;
            let len = buf.Length().ok()?;
            let reader = DataReader::FromBuffer(&buf).ok()?;
            let mut bytes = vec![0u8; len as usize];
            reader.ReadBytes(&mut bytes).ok()?;
            return Some(LeAdvertisement {
                address: DeviceId::from(addr),
                apple_data: bytes,
            });
        }
    }
    None
}

/// On Windows the OS owns pairing/connection; treat as a no-op success for now.
pub async fn connect_device(_id: &DeviceId) -> Result<(), String> {
    Ok(())
}
