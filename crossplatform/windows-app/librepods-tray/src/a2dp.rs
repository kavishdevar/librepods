//! A2DP recovery: after the hi-res mic puts the AirPods into their bidirectional
//! "call" mode, A2DP playback degrades to mono/right until the audio link is
//! re-established. This toggles the AirPods' A2DP service off then on — the
//! programmatic equivalent of disconnecting + reconnecting them — to restore
//! stereo, without a full Bluetooth restart.

use std::mem::{size_of, zeroed};

use windows_sys::Win32::Devices::Bluetooth::{
    BLUETOOTH_DEVICE_INFO, BLUETOOTH_DEVICE_SEARCH_PARAMS, BLUETOOTH_FIND_RADIO_PARAMS,
    BluetoothFindDeviceClose, BluetoothFindFirstDevice, BluetoothFindFirstRadio,
    BluetoothFindNextDevice, BluetoothFindRadioClose, BluetoothSetServiceState,
};
use windows_sys::Win32::Foundation::{CloseHandle, HANDLE};
use windows_sys::core::GUID;

// AdvancedAudioDistribution (A2DP) profile service.
const A2DP_SERVICE: GUID = GUID {
    data1: 0x0000_110D,
    data2: 0x0000,
    data3: 0x1000,
    data4: [0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB],
};

const BLUETOOTH_SERVICE_DISABLE: u32 = 0x00;
const BLUETOOTH_SERVICE_ENABLE: u32 = 0x01;

fn find_device(mac: u64) -> Option<BLUETOOTH_DEVICE_INFO> {
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
}

/// Reconnect the AirPods' A2DP service (disable then enable) to restore stereo.
/// Best-effort: any failure (e.g. needs elevation) is silently ignored — the
/// user can still recover by toggling Bluetooth.
pub fn reset(mac: u64) {
    unsafe {
        let info = match find_device(mac) {
            Some(i) => i,
            None => return,
        };

        let mut rparams: BLUETOOTH_FIND_RADIO_PARAMS = zeroed();
        rparams.dwSize = size_of::<BLUETOOTH_FIND_RADIO_PARAMS>() as u32;
        let mut radio: HANDLE = std::ptr::null_mut();
        let hfind = BluetoothFindFirstRadio(&rparams, &mut radio);
        if hfind.is_null() {
            return;
        }

        BluetoothSetServiceState(radio, &info, &A2DP_SERVICE, BLUETOOTH_SERVICE_DISABLE);
        std::thread::sleep(std::time::Duration::from_millis(600));
        BluetoothSetServiceState(radio, &info, &A2DP_SERVICE, BLUETOOTH_SERVICE_ENABLE);

        CloseHandle(radio);
        BluetoothFindRadioClose(hfind);
    }
}
