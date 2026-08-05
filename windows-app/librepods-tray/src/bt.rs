//! Locate the paired AirPods and return their 48-bit Bluetooth address.

use std::mem::{size_of, zeroed};

use windows_sys::Win32::Devices::Bluetooth::{
    BLUETOOTH_DEVICE_INFO, BLUETOOTH_DEVICE_SEARCH_PARAMS, BluetoothFindDeviceClose,
    BluetoothFindFirstDevice, BluetoothFindNextDevice,
};

fn utf16_name(buf: &[u16]) -> String {
    let end = buf.iter().position(|&c| c == 0).unwrap_or(buf.len());
    String::from_utf16_lossy(&buf[..end])
}

/// Address of the first paired device whose name contains "airpod".
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
            let name = utf16_name(&info.szName);
            if name.to_lowercase().contains("airpod") {
                found = Some(info.Address.Anonymous.ullLong);
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
