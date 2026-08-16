//! Locate the paired AirPods and return their 48-bit Bluetooth address.

use std::mem::{size_of, zeroed};

use windows_sys::Win32::Devices::Bluetooth::{
    BLUETOOTH_DEVICE_INFO, BLUETOOTH_DEVICE_SEARCH_PARAMS, BLUETOOTH_FIND_RADIO_PARAMS,
    BluetoothFindDeviceClose, BluetoothFindFirstDevice, BluetoothFindFirstRadio,
    BluetoothFindNextDevice, BluetoothFindRadioClose, BluetoothSetServiceState,
};
use windows_sys::Win32::Foundation::{CloseHandle, HANDLE};
use windows_sys::core::GUID;

// Classic-audio service GUIDs — A2DP AudioSink + Handsfree.
const AUDIO_SINK: GUID = GUID {
    data1: 0x0000_110b, data2: 0, data3: 0x1000,
    data4: [0x80, 0x00, 0x00, 0x80, 0x5f, 0x9b, 0x34, 0xfb],
};
const HANDSFREE: GUID = GUID {
    data1: 0x0000_111e, data2: 0, data3: 0x1000,
    data4: [0x80, 0x00, 0x00, 0x80, 0x5f, 0x9b, 0x34, 0xfb],
};

/// Real Bluetooth connect/disconnect of the AirPods' AUDIO by toggling their audio
/// services on the local radio — distinct from releasing our AAP control session.
/// `connect = false` disconnects (Windows drops the device); `true` reconnects.
/// Returns true if at least one service state was set. May require the device to be
/// paired and, on some systems, elevation.
pub fn set_audio_connected(mac: u64, connect: bool) -> bool {
    unsafe {
        let dev = match device_info(mac) {
            Some(d) => d,
            None => return false,
        };
        let mut rparams: BLUETOOTH_FIND_RADIO_PARAMS = zeroed();
        rparams.dwSize = size_of::<BLUETOOTH_FIND_RADIO_PARAMS>() as u32;
        let mut hradio: HANDLE = std::ptr::null_mut();
        let hfind = BluetoothFindFirstRadio(&rparams, &mut hradio);
        if hfind.is_null() {
            return false;
        }
        let flags: u32 = if connect { 1 } else { 0 }; // ENABLE / DISABLE
        // Enable BOTH A2DP (stereo output) and Handsfree so at least one audio
        // endpoint always comes up — enabling only A2DP left the user with NO sound
        // when A2DP alone failed to connect (no HFP fallback). (The A2DP-vs-HFP
        // routing / "mic but no audio" concern is better solved by picking the default
        // output device, not by dropping HFP here.)
        let a = BluetoothSetServiceState(hradio, &dev, &AUDIO_SINK, flags);
        let b = BluetoothSetServiceState(hradio, &dev, &HANDSFREE, flags);
        CloseHandle(hradio);
        BluetoothFindRadioClose(hfind);
        a == 0 || b == 0 // ERROR_SUCCESS on either
    }
}

/// Look up a paired device's `BLUETOOTH_DEVICE_INFO` by its 48-bit address.
unsafe fn device_info(mac: u64) -> Option<BLUETOOTH_DEVICE_INFO> {
    let mut params: BLUETOOTH_DEVICE_SEARCH_PARAMS = zeroed();
    params.dwSize = size_of::<BLUETOOTH_DEVICE_SEARCH_PARAMS>() as u32;
    params.fReturnAuthenticated = 1;
    params.fReturnRemembered = 1;
    params.fReturnConnected = 1;
    let mut info: BLUETOOTH_DEVICE_INFO = zeroed();
    info.dwSize = size_of::<BLUETOOTH_DEVICE_INFO>() as u32;
    let h = BluetoothFindFirstDevice(&params, &mut info);
    if h.is_null() {
        return None;
    }
    let mut found = None;
    loop {
        if info.Address.Anonymous.ullLong == mac {
            found = Some(info);
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
