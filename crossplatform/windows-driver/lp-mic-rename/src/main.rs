//! Rename the LibrePodsMic virtual microphone endpoint's friendly name — e.g. to
//! the connected AirPods' name, so it shows as "AirPods Pro de Pedro" in Discord,
//! Teams, Zoom, etc. instead of the driver's static "CustomName2 (AudioCodec
//! Device)".
//!
//! It enumerates active capture endpoints, finds the one whose name contains
//! "AudioCodec", and sets its PKEY_Device_FriendlyName via IPolicyConfig
//! (CPolicyConfigClient) — the same runtime API MagicPods & co. use. User-mode,
//! no admin, no reboot.
//!
//!   lp-mic-rename "AirPods Pro de Pedro"

use std::ffi::c_void;

use windows::core::{Interface, IUnknown, IUnknown_Vtbl, GUID, HRESULT, PWSTR};
use windows::Win32::Media::Audio::{
    eCapture, IMMDeviceEnumerator, MMDeviceEnumerator, DEVICE_STATE_ACTIVE,
};
use windows::Win32::System::Com::{
    CoCreateInstance, CoInitializeEx, CoTaskMemFree, CLSCTX_ALL, COINIT_MULTITHREADED, STGM_READ,
};
use windows::Win32::UI::Shell::PropertiesSystem::PROPERTYKEY;

const CLSID_POLICY_CONFIG_CLIENT: GUID = GUID::from_u128(0x870af99c_171d_4f9e_af0d_e63df40c2bc9);

// PKEY_Device_FriendlyName = {a45c254e-df1c-4efd-8020-67d146a850e0}, 14
const PKEY_DEVICE_FRIENDLYNAME: PROPERTYKEY = PROPERTYKEY {
    fmtid: GUID::from_u128(0xa45c254e_df1c_4efd_8020_67d146a850e0),
    pid: 14,
};

const VT_LPWSTR: u16 = 31;

// A PROPVARIANT holding a VT_LPWSTR — laid out to match the real 24-byte x64
// PROPVARIANT (vt + 3 reserved u16, then the pointer union). We only ever set
// the string case, so the rest is padding.
#[repr(C)]
struct PropVariantStr {
    vt: u16,
    r1: u16,
    r2: u16,
    r3: u16,
    pwsz: *mut u16,
    _pad: u64,
}

// IPolicyConfig (CPolicyConfigClient), IID f8679f50-850a-41cf-9c72-430f290290c8.
// We only need SetPropertyValue (the 10th method); everything before it is an
// opaque slot so the vtable offset is right.
#[repr(transparent)]
#[derive(Clone)]
struct IPolicyConfig(IUnknown);

#[repr(C)]
#[allow(non_snake_case)]
struct IPolicyConfig_Vtbl {
    base__: IUnknown_Vtbl,
    GetMixFormat: usize,
    GetDeviceFormat: usize,
    ResetDeviceFormat: usize,
    SetDeviceFormat: usize,
    GetProcessingPeriod: usize,
    SetProcessingPeriod: usize,
    GetShareMode: usize,
    SetShareMode: usize,
    GetPropertyValue: usize,
    // SetPropertyValue(PCWSTR name, const PROPERTYKEY& key, PROPVARIANT* pv)
    SetPropertyValue:
        unsafe extern "system" fn(*mut c_void, *const u16, *const c_void, *const c_void) -> HRESULT,
}

unsafe impl Interface for IPolicyConfig {
    type Vtable = IPolicyConfig_Vtbl;
    const IID: GUID = GUID::from_u128(0xf8679f50_850a_41cf_9c72_430f290290c8);
}

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

/// Read a null-terminated wide (UTF-16) string from a raw pointer.
unsafe fn wide_ptr_to_string(p: *const u16) -> String {
    if p.is_null() {
        return String::new();
    }
    let mut len = 0isize;
    while *p.offset(len) != 0 {
        len += 1;
    }
    let slice = std::slice::from_raw_parts(p, len as usize);
    String::from_utf16_lossy(slice)
}

fn main() {
    let name = std::env::args().nth(1).unwrap_or_else(|| {
        eprintln!("usage: lp-mic-rename \"<new microphone name>\"");
        std::process::exit(2);
    });

    unsafe {
        let _ = CoInitializeEx(None, COINIT_MULTITHREADED);

        let enumerator: IMMDeviceEnumerator =
            CoCreateInstance(&MMDeviceEnumerator, None, CLSCTX_ALL).expect("MMDeviceEnumerator");
        let collection = enumerator
            .EnumAudioEndpoints(eCapture, DEVICE_STATE_ACTIVE)
            .expect("EnumAudioEndpoints");
        let count = collection.GetCount().unwrap_or(0);

        let mut target: Option<PWSTR> = None;
        for i in 0..count {
            let device = match collection.Item(i) {
                Ok(d) => d,
                Err(_) => continue,
            };
            let id = match device.GetId() {
                Ok(id) => id,
                Err(_) => continue,
            };
            let friendly = device
                .OpenPropertyStore(STGM_READ)
                .ok()
                .and_then(|store| store.GetValue(&PKEY_DEVICE_FRIENDLYNAME).ok())
                .map(|pv| {
                    // PROPVARIANT holding the friendly name (VT_LPWSTR). Read the
                    // pointer directly via the matching layout; `pv` clears itself
                    // on drop.
                    let raw = &pv as *const _ as *const PropVariantStr;
                    if (*raw).vt == VT_LPWSTR {
                        wide_ptr_to_string((*raw).pwsz)
                    } else {
                        String::new()
                    }
                })
                .unwrap_or_default();

            if friendly.contains("AudioCodec") {
                println!("Found the virtual mic: \"{friendly}\"");
                target = Some(id);
                break;
            }
            CoTaskMemFree(Some(id.0 as *const c_void));
        }

        let id = match target {
            Some(id) => id,
            None => {
                eprintln!(
                    "Could not find the AudioCodec capture endpoint. Is the LibrePodsMic \
                     driver installed and the virtual mic present?"
                );
                std::process::exit(1);
            }
        };

        let policy: IPolicyConfig =
            CoCreateInstance(&CLSID_POLICY_CONFIG_CLIENT, None, CLSCTX_ALL)
                .expect("CPolicyConfigClient");

        let wname = wide(&name);
        let pv = PropVariantStr {
            vt: VT_LPWSTR,
            r1: 0,
            r2: 0,
            r3: 0,
            pwsz: wname.as_ptr() as *mut u16,
            _pad: 0,
        };

        let hr = (policy.vtable().SetPropertyValue)(
            policy.as_raw(),
            id.0,
            &PKEY_DEVICE_FRIENDLYNAME as *const _ as *const c_void,
            &pv as *const _ as *const c_void,
        );

        CoTaskMemFree(Some(id.0 as *const c_void));

        if hr.is_ok() {
            println!("Renamed the virtual microphone to \"{name}\".");
        } else {
            eprintln!("SetPropertyValue failed: {hr:?}");
            std::process::exit(1);
        }
    }
}
