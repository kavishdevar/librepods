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

/// Name patterns of Apple-chip audio devices that speak the AAP protocol — not
/// just AirPods but Beats too (Powerbeats, Beats Fit Pro, Studio Buds, Solo,
/// Studio3, Flex…), which share the same H1/W1 chip and endpoint. Matched
/// case-insensitively against the paired device's name.
const AAP_NAME_HINTS: &[&str] = &["airpod", "beats"];

/// (address, display name) of the first paired device whose name matches a known
/// AAP device (AirPods / Beats). The Windows " - Find My" suffix is stripped for
/// display.
pub fn find_airpods() -> Option<(u64, String)> {
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
            let lname = name.to_lowercase();
            if AAP_NAME_HINTS.iter().any(|h| lname.contains(h)) {
                let clean = name
                    .trim_end_matches("- Find My")
                    .trim_end_matches(" -")
                    .trim()
                    .to_string();
                found = Some((info.Address.Anonymous.ullLong, clean));
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
