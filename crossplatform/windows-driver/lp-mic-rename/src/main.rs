//! Rename the LibrePods virtual microphone endpoint(s) to a given name (e.g. the
//! connected device's name — "AirPods Pro de Pedro", "Beats Studio Buds", …), so
//! it shows that in Discord/Teams/Zoom instead of the driver's generic name.
//! MUST run elevated (writes HKLM\...\MMDevices).
//!
//!   lp-mic-rename "AirPods Pro de Pedro"
//!
//! IPolicyConfig::SetPropertyValue returns E_ACCESSDENIED even elevated, so we
//! write the endpoint's PKEY_Device_FriendlyName registry blob directly (as
//! SoundVolumeView does) and restart the audio endpoint service. Reinstalls leave
//! several stale "LibrePods" capture endpoints, so we rename ALL of them (the
//! active one is the one that shows up).

use std::ffi::c_void;
use std::process::Command;

use windows::core::{GUID, PCWSTR};
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

const PKEY_DEVICE_FRIENDLYNAME: PROPERTYKEY = PROPERTYKEY {
    fmtid: GUID::from_u128(0xa45c254e_df1c_4efd_8020_67d146a850e0),
    pid: 14,
};
const PKEY_DEVICE_DEVICEDESC: PROPERTYKEY = PROPERTYKEY {
    fmtid: GUID::from_u128(0xa45c254e_df1c_4efd_8020_67d146a850e0),
    pid: 2,
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

/// Write PKEY_Device_FriendlyName (VT_LPWSTR REG_BINARY: 0x1F + UTF-16LE + NUL)
/// under the endpoint's registry Properties. Returns true on success.
unsafe fn write_friendly(guid: &str, name: &str) -> bool {
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
        log(&format!("RegCreateKeyEx {guid} failed: {rc:?}"));
        return false;
    }
    let mut blob: Vec<u8> = vec![VT_LPWSTR as u8, 0, 0, 0];
    for w in wide(name) {
        blob.extend_from_slice(&w.to_le_bytes());
    }
    let valname = wide("{a45c254e-df1c-4efd-8020-67d146a850e0},14");
    let rc = RegSetValueExW(hkey, PCWSTR(valname.as_ptr()), 0, REG_BINARY, Some(&blob));
    let _ = RegCloseKey(hkey);
    log(&format!("wrote {guid}: {rc:?}"));
    rc.is_ok()
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

        let read = |dev: &windows::Win32::Media::Audio::IMMDevice, key: &PROPERTYKEY| -> String {
            dev.OpenPropertyStore(STGM_READ)
                .ok()
                .and_then(|s| s.GetValue(key).ok())
                .map(|pv| {
                    let raw = &pv as *const _ as *const PropVariantStr;
                    if (*raw).vt == VT_LPWSTR {
                        wide_ptr_to_string((*raw).pwsz)
                    } else {
                        String::new()
                    }
                })
                .unwrap_or_default()
        };

        // Rename EVERY LibrePods capture endpoint (match on the constant
        // DeviceDesc so it works even after a previous rename), so the active one
        // is covered regardless of stale duplicates.
        let mut renamed = 0;
        for i in 0..count {
            let dev = match collection.Item(i) {
                Ok(d) => d,
                Err(_) => continue,
            };
            let id = match dev.GetId() {
                Ok(id) => id,
                Err(_) => continue,
            };
            let desc = read(&dev, &PKEY_DEVICE_DEVICEDESC);
            let friendly = read(&dev, &PKEY_DEVICE_FRIENDLYNAME);
            let is_ours = desc.contains("LibrePods")
                || desc.contains("AudioCodec")
                || friendly.contains("LibrePods")
                || friendly.contains("AudioCodec");
            if is_ours {
                let id_str = wide_ptr_to_string(id.0);
                if let Some(guid) = id_str.rsplit('.').next() {
                    log(&format!("match {guid} desc='{desc}' friendly='{friendly}'"));
                    if write_friendly(guid, &name) {
                        renamed += 1;
                    }
                }
            }
            CoTaskMemFree(Some(id.0 as *const c_void));
        }

        if renamed == 0 {
            eprintln!("No LibrePods capture endpoint found (or write denied — run elevated).");
            std::process::exit(1);
        }

        let _ = Command::new("powershell")
            .args([
                "-NoProfile",
                "-Command",
                "Restart-Service AudioEndpointBuilder -Force",
            ])
            .status();

        println!("Renamed {renamed} LibrePods mic endpoint(s) to \"{name}\".");
    }
}
