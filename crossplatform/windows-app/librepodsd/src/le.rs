//! BLE advertisement watcher: detect the AirPods nearby (their Apple
//! proximity-pairing advertisement) so the daemon can prompt "connect?" before
//! it opens the AAP session. Ported/trimmed from crossplatform-rust's le_scan.rs.

use windows::Devices::Bluetooth::Advertisement::{
    BluetoothLEAdvertisementReceivedEventArgs, BluetoothLEAdvertisementWatcher,
    BluetoothLEScanningMode,
};
use windows::Foundation::TypedEventHandler;
use windows::Storage::Streams::DataReader;
use windows::Win32::System::Com::{CoInitializeEx, COINIT_MULTITHREADED};

const APPLE_COMPANY_ID: u16 = 0x004C;
/// Apple manufacturer-data message type for proximity pairing (AirPods / Beats).
const PROXIMITY_PAIRING: u8 = 0x07;

/// Start watching and call `on_nearby` on each AirPods proximity advertisement.
/// Blocks (keeps this thread's COM apartment + the watcher alive), so run it on
/// its own thread.
pub fn watch_nearby(on_nearby: impl Fn() + Send + Sync + 'static) {
    unsafe {
        let _ = CoInitializeEx(None, COINIT_MULTITHREADED);
    }
    if start(on_nearby).is_err() {
        return;
    }
    loop {
        std::thread::sleep(std::time::Duration::from_secs(3600));
    }
}

fn start(on_nearby: impl Fn() + Send + Sync + 'static) -> windows::core::Result<()> {
    let watcher = BluetoothLEAdvertisementWatcher::new()?;
    watcher.SetScanningMode(BluetoothLEScanningMode::Active)?;
    let handler = TypedEventHandler::new(
        move |_s: &Option<BluetoothLEAdvertisementWatcher>,
              args: &Option<BluetoothLEAdvertisementReceivedEventArgs>| {
            if let Some(args) = args.as_ref() {
                if is_airpods(args) {
                    on_nearby();
                }
            }
            Ok(())
        },
    );
    watcher.Received(&handler)?;
    watcher.Start()?;
    std::mem::forget(watcher); // keep it alive for the daemon's lifetime
    Ok(())
}

/// True if this advertisement is an Apple proximity-pairing message (AirPods /
/// Beats). (Not IRK-resolved — any nearby AirPods matches; fine for one user.)
fn is_airpods(args: &BluetoothLEAdvertisementReceivedEventArgs) -> bool {
    let Ok(adv) = args.Advertisement() else {
        return false;
    };
    let Ok(mfg) = adv.ManufacturerData() else {
        return false;
    };
    let size = mfg.Size().unwrap_or(0);
    for i in 0..size {
        let Ok(md) = mfg.GetAt(i) else { continue };
        if md.CompanyId().unwrap_or(0) != APPLE_COMPANY_ID {
            continue;
        }
        if let Ok(buf) = md.Data() {
            let len = buf.Length().unwrap_or(0) as usize;
            if let Ok(reader) = DataReader::FromBuffer(&buf) {
                let mut bytes = vec![0u8; len];
                if reader.ReadBytes(&mut bytes).is_ok() && bytes.first() == Some(&PROXIMITY_PAIRING) {
                    return true;
                }
            }
        }
    }
    false
}
