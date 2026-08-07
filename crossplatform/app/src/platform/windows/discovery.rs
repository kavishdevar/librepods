//! Windows device discovery via the classic Win32 Bluetooth device enumeration
//! (`BluetoothFindFirstDevice`) — the same approach the librepods-tray app uses.

use crate::platform::{DeviceId, DiscoveredDevice};
use std::mem::{size_of, zeroed};
use windows_sys::Win32::Devices::Bluetooth::{
    BLUETOOTH_DEVICE_INFO, BLUETOOTH_DEVICE_SEARCH_PARAMS, BluetoothFindDeviceClose,
    BluetoothFindFirstDevice, BluetoothFindNextDevice,
};

fn utf16_name(buf: &[u16]) -> String {
    let end = buf.iter().position(|&c| c == 0).unwrap_or(buf.len());
    String::from_utf16_lossy(&buf[..end])
}

/// Enumerate all remembered/connected Bluetooth devices as (address, name,
/// connected).
fn all_devices() -> Vec<(u64, String, bool)> {
    let mut out = Vec::new();
    unsafe {
        let mut params: BLUETOOTH_DEVICE_SEARCH_PARAMS = zeroed();
        params.dwSize = size_of::<BLUETOOTH_DEVICE_SEARCH_PARAMS>() as u32;
        params.fReturnAuthenticated = 1;
        params.fReturnRemembered = 1;
        params.fReturnConnected = 1;
        params.fReturnUnknown = 1;

        let mut info: BLUETOOTH_DEVICE_INFO = zeroed();
        info.dwSize = size_of::<BLUETOOTH_DEVICE_INFO>() as u32;

        let h = BluetoothFindFirstDevice(&params, &mut info);
        if h.is_null() {
            return out;
        }
        loop {
            out.push((
                info.Address.Anonymous.ullLong,
                utf16_name(&info.szName),
                info.fConnected != 0,
            ));
            info.dwSize = size_of::<BLUETOOTH_DEVICE_INFO>() as u32;
            if BluetoothFindNextDevice(h, &mut info) == 0 {
                break;
            }
        }
        BluetoothFindDeviceClose(h);
    }
    out
}

fn clean_name(name: &str) -> String {
    name.trim_end_matches("- Find My")
        .trim_end_matches(" -")
        .trim()
        .to_string()
}

pub async fn find_connected_airpods() -> Option<DiscoveredDevice> {
    all_devices()
        .into_iter()
        .find(|(_, name, connected)| *connected && name.to_lowercase().contains("airpod"))
        .map(|(addr, name, _)| DiscoveredDevice {
            id: DeviceId::from(addr),
            name: clean_name(&name),
        })
}

pub async fn find_other_managed_devices(managed_macs: &[String]) -> Vec<DiscoveredDevice> {
    all_devices()
        .into_iter()
        .filter(|(_, _, connected)| *connected)
        .filter_map(|(addr, name, _)| {
            let id = DeviceId::from(addr);
            let mac = id.to_string();
            managed_macs
                .iter()
                .any(|m| m.eq_ignore_ascii_case(&mac))
                .then(|| DiscoveredDevice {
                    id,
                    name: clean_name(&name),
                })
        })
        .collect()
}
