//! Rename the LibrePods virtual microphone endpoint to a given name (e.g. the
//! connected device's name — "AirPods Pro de Pedro", "Beats Studio Buds", …), so
//! it shows that in Discord/Teams/Zoom instead of the driver's generic
//! "LibrePods". MUST run elevated (writes HKLM\...\MMDevices).
//!
//!   lp-mic-rename "AirPods Pro de Pedro"
//!
//! IPolicyConfig::SetPropertyValue returns E_ACCESSDENIED even elevated (the
//! FriendlyName property is protected against it), so we write the endpoint's
//! PKEY_Device_FriendlyName registry blob directly (as SoundVolumeView does) and
//! restart the audio endpoint service so apps pick it up.

use std::ffi::c_void;
use std::process::Command;

use windows::core::PCWSTR;
use windows::Win32::Media::Audio::{
    eCapture, IMMDeviceEnumerator, MMDeviceEnumerator, DEVICE_STATE_ACTIVE,
};
use windows::Win32::System::Com::{
    CoCreateInstance, CoInitializeEx, CoTaskMemFree, CLSCTX_ALL, COINIT_MULTITHREADED, STGM_READ,
};
use windows::Win32::System::Registry::{
    RegCloseKey, RegCreateKeyExW, RegSetValueExW, HKEY, HKEY_LOCAL_MACHINE, KEY_SET_VALUE,
    REG_BINARY, REG_OPTION_NON_VOLATILE,
};
use windows::Win32::UI::Shell::PropertiesSystem::PROPERTYKEY;
use windows::core::GUID;

const PKEY_DEVICE_FRIENDLYNAME: PROPERTYKEY = PROPERTYKEY {
    fmtid: GUID::from_u128(0xa45c254e_df1c_4efd_8020_67d146a850e0),
    pid: 14,
};
const VT_LPWSTR: u16 = 31;

#[repr(C)]
struct PropVariantStr {
    vt: u16,
    r1: u16,
    r2: u16,
    r3: u16,
    pwsz: *mut u16,
    _pad: u64,
}

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

unsafe fn wide_ptr_to_string(p: *const u16) -> String {
    if p.is_null() {
        return String::new();
    }
    let mut len = 0isize;
    while *p.offset(len) != 0 {
        len += 1;
    }
    String::from_utf16_lossy(std::slice::from_raw_parts(p, len as usize))
}

fn log(s: &str) {
    use std::io::Write;
    if let Ok(la) = std::env::var("LOCALAPPDATA") {
        if let Ok(mut f) = std::fs::OpenOptions::new()
            .create(true)
            .append(true)
            .open(format!("{la}\\LibrePods\\rename.log"))
        {
            let _ = writeln!(f, "{s}");
        }
    }
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

        // Find our virtual mic and grab its endpoint id.
        let mut endpoint_id = String::new();
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
                .and_then(|s| s.GetValue(&PKEY_DEVICE_FRIENDLYNAME).ok())
                .map(|pv| {
                    let raw = &pv as *const _ as *const PropVariantStr;
                    if (*raw).vt == VT_LPWSTR {
                        wide_ptr_to_string((*raw).pwsz)
                    } else {
                        String::new()
                    }
                })
                .unwrap_or_default();
            if friendly.contains("LibrePods") || friendly.contains("AudioCodec") {
                endpoint_id = wide_ptr_to_string(id.0);
                CoTaskMemFree(Some(id.0 as *const c_void));
                println!("Found the virtual mic: \"{friendly}\"");
                break;
            }
            CoTaskMemFree(Some(id.0 as *const c_void));
        }

        if endpoint_id.is_empty() {
            eprintln!("Could not find the LibrePods capture endpoint. Is the driver installed?");
            std::process::exit(1);
        }
        // Endpoint id looks like "{0.0.1.00000000}.{<endpoint-guid>}"; the registry
        // subkey is that trailing "{<endpoint-guid>}".
        let guid = endpoint_id.rsplit('.').next().unwrap_or("");
        log(&format!("id={endpoint_id} guid={guid}"));

        // Write PKEY_Device_FriendlyName as a VT_LPWSTR REG_BINARY blob:
        // 4-byte type (0x1F) + UTF-16LE string + NUL.
        let subkey = wide(&format!(
            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\MMDevices\\Audio\\Capture\\{guid}\\Properties"
        ));
        let mut hkey = HKEY::default();
        let rc = RegCreateKeyExW(
            HKEY_LOCAL_MACHINE,
            PCWSTR(subkey.as_ptr()),
            0,
            PCWSTR::null(),
            REG_OPTION_NON_VOLATILE,
            KEY_SET_VALUE,
            None,
            &mut hkey,
            None,
        );
        if rc.is_err() {
            log(&format!("RegCreateKeyEx failed: {rc:?}"));
            eprintln!("RegCreateKeyEx failed ({rc:?}) — run elevated (admin).");
            std::process::exit(3);
        }
        let mut blob: Vec<u8> = vec![VT_LPWSTR as u8, 0, 0, 0];
        for w in wide(&name) {
            blob.extend_from_slice(&w.to_le_bytes());
        }
        let valname = wide("{a45c254e-df1c-4efd-8020-67d146a850e0},14");
        let rc = RegSetValueExW(hkey, PCWSTR(valname.as_ptr()), 0, REG_BINARY, Some(&blob));
        let _ = RegCloseKey(hkey);
        log(&format!("RegSetValueEx rc={rc:?}"));
        if rc.is_err() {
            eprintln!("RegSetValueEx failed ({rc:?}) — run elevated (admin).");
            std::process::exit(4);
        }

        // Refresh the audio endpoint service so the new name shows immediately.
        let _ = Command::new("powershell")
            .args([
                "-NoProfile",
                "-Command",
                "Restart-Service AudioEndpointBuilder -Force",
            ])
            .status();

        println!("Renamed the virtual microphone to \"{name}\".");
    }
}
