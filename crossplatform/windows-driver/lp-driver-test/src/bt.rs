//! Find the paired AirPods and return their 48-bit Bluetooth address.

use std::mem::{size_of, zeroed};

use windows_sys::Win32::Devices::Bluetooth::{
    BluetoothFindDeviceClose, BluetoothFindFirstDevice, BluetoothFindNextDevice,
    BLUETOOTH_DEVICE_INFO, BLUETOOTH_DEVICE_SEARCH_PARAMS,
};

pub fn fmt_mac(addr: u64) -> String {
    let b = addr.to_be_bytes();
    format!(
        "{:02X}:{:02X}:{:02X}:{:02X}:{:02X}:{:02X}",
        b[2], b[3], b[4], b[5], b[6], b[7]
    )
}

pub fn parse_mac(s: &str) -> Option<u64> {
    let mut acc: u64 = 0;
    let mut n = 0;
    for part in s.split(':') {
        acc = (acc << 8) | u8::from_str_radix(part, 16).ok()? as u64;
        n += 1;
    }
    if n == 6 {
        Some(acc)
    } else {
        None
    }
}

fn utf16_name(buf: &[u16]) -> String {
    let end = buf.iter().position(|&c| c == 0).unwrap_or(buf.len());
    String::from_utf16_lossy(&buf[..end])
}

/// Enumerate paired devices and return the AirPods address (name contains
/// "airpod"). Returns None if not found.
pub fn find_airpods() -> Option<u64> {
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
            return None;
        }

        let mut found = None;
        loop {
            let addr = info.Address.Anonymous.ullLong;
            let name = utf16_name(&info.szName);
            if name.to_lowercase().contains("airpod") {
                found = Some(addr);
                break;
            }
            info.dwSize = size_of::<BLUETOOTH_DEVICE_INFO>() as u32;
            if BluetoothFindNextDevice(h, &mut info) == 0 {
                break;
            }
        }
        BluetoothFindDeviceClose(h);
        found
    }
}
