//! Rename the LibrePods virtual microphone endpoint(s) to a given name (e.g. the
//! connected device's name — "AirPods Pro de Pedro", "Beats Studio Buds", …), so
//! any app (Discord/Teams/Zoom) shows that instead of the driver's generic name.
//! MUST run elevated (writes HKLM\...\MMDevices).
//!
//!   lp-mic-rename "AirPods Pro de Pedro"
//!
//! HOW (learned the hard way): the Windows Sound "Rename" UI persists the name in
//! PKEY_Device_DeviceDesc ({a45c254e...},2) as a **plain REG_SZ string** — NOT in
//! PKEY_Device_FriendlyName (,14), and NOT as a REG_BINARY PROPVARIANT blob (a
//! blob there is ignored → the display falls back to the driver's INF name). So we
//! write ,2 as REG_SZ, exactly like the UI.
//!
//! We identify our endpoint(s) by STABLE, name-independent properties (so re-runs
//! still match after a rename): the device/bus name ({b3f8fa53...},6 == "LibrePods",
//! from our INF) or the hardware id ({a8b865dd...},8 contains "AudioCodec"). All
//! matching endpoints are renamed (reinstalls leave stale duplicates; the active
//! one is whichever shows up). We skip endpoints already named correctly, and only
//! restart the audio service if something actually changed (no needless blip).

use std::ffi::c_void;
use std::process::Command;

use windows::core::{PCWSTR, PWSTR};
use windows::Win32::System::Registry::{
    RegCloseKey, RegCreateKeyExW, RegEnumKeyExW, RegGetValueW, RegOpenKeyExW, RegSetValueExW, HKEY,
    HKEY_LOCAL_MACHINE, KEY_READ, KEY_SET_VALUE, REG_OPTION_NON_VOLATILE, REG_SZ, RRF_RT_REG_SZ,
};

const BASE: &str = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\MMDevices\\Audio\\Capture";
/// Device/bus friendly name — stays "LibrePods" (our INF DeviceDesc) after renames.
const PROP_DEVICE_NAME: &str = "{b3f8fa53-0004-438e-9003-51a46e139bfc},6";
/// Hardware id — "ROOT\\AudioCodec" for our driver.
const PROP_HARDWARE_ID: &str = "{a8b865dd-2e3d-4094-ad97-e593a70c75d6},8";
/// PKEY_Device_DeviceDesc — the endpoint's display name (what "Rename" sets).
const PROP_DEVICE_DESC: &str = "{a45c254e-df1c-4efd-8020-67d146a850e0},2";

fn wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
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

/// Read a REG_SZ value under HKLM\<subpath>. Returns None if missing / wrong type.
unsafe fn read_reg_sz(subpath: &str, value: &str) -> Option<String> {
    let wpath = wide(subpath);
    let wval = wide(value);
    let mut buf = [0u16; 512];
    let mut cb: u32 = (buf.len() * 2) as u32;
    let rc = RegGetValueW(
        HKEY_LOCAL_MACHINE,
        PCWSTR(wpath.as_ptr()),
        PCWSTR(wval.as_ptr()),
        RRF_RT_REG_SZ,
        None,
        Some(buf.as_mut_ptr() as *mut c_void),
        Some(&mut cb),
    );
    if rc.is_ok() {
        let n = (cb as usize / 2).saturating_sub(1); // drop the NUL terminator
        Some(String::from_utf16_lossy(&buf[..n]))
    } else {
        None
    }
}

/// Write PKEY_Device_DeviceDesc as a plain REG_SZ string (what the Sound UI does).
unsafe fn write_desc(guid: &str, name: &str) -> bool {
    let subkey = wide(&format!("{BASE}\\{guid}\\Properties"));
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
    let wname = wide(name);
    let bytes = std::slice::from_raw_parts(wname.as_ptr() as *const u8, wname.len() * 2);
    let valname = wide(PROP_DEVICE_DESC);
    let rc = RegSetValueExW(hkey, PCWSTR(valname.as_ptr()), 0, REG_SZ, Some(bytes));
    let _ = RegCloseKey(hkey);
    rc.is_ok()
}

/// The name to apply: the CLI arg if given, else the contents of
/// %LOCALAPPDATA%\LibrePods\micname.txt (so an elevated scheduled task launched
/// by the non-elevated tray can pick up the connected device's name).
fn resolve_name() -> Option<String> {
    if let Some(a) = std::env::args().nth(1) {
        let a = a.trim().to_string();
        if !a.is_empty() {
            return Some(a);
        }
    }
    let la = std::env::var("LOCALAPPDATA").ok()?;
    let s = std::fs::read_to_string(format!("{la}\\LibrePods\\micname.txt")).ok()?;
    let s = s.trim().to_string();
    if s.is_empty() {
        None
    } else {
        Some(s)
    }
}

fn main() {
    let name = resolve_name().unwrap_or_else(|| {
        eprintln!(
            "usage: lp-mic-rename \"<new microphone name>\"\n       \
             (or write the name to %LOCALAPPDATA%\\LibrePods\\micname.txt)"
        );
        std::process::exit(2);
    });

    unsafe {
        let mut hbase = HKEY::default();
        let rc = RegOpenKeyExW(
            HKEY_LOCAL_MACHINE,
            PCWSTR(wide(BASE).as_ptr()),
            0,
            KEY_READ,
            &mut hbase,
        );
        if rc.is_err() {
            eprintln!("cannot open MMDevices Capture key: {rc:?}");
            std::process::exit(1);
        }

        let mut changed = 0;
        let mut already = 0;
        let mut i = 0u32;
        loop {
            let mut namebuf = [0u16; 128];
            let mut namelen = namebuf.len() as u32;
            let rc = RegEnumKeyExW(
                hbase,
                i,
                PWSTR(namebuf.as_mut_ptr()),
                &mut namelen,
                None,
                PWSTR::null(),
                None,
                None,
            );
            if rc.is_err() {
                break; // ERROR_NO_MORE_ITEMS
            }
            i += 1;

            let guid = String::from_utf16_lossy(&namebuf[..namelen as usize]);
            let props = format!("{BASE}\\{guid}\\Properties");
            let dev = read_reg_sz(&props, PROP_DEVICE_NAME).unwrap_or_default();
            let hw = read_reg_sz(&props, PROP_HARDWARE_ID).unwrap_or_default();
            let is_ours =
                dev.contains("LibrePods") || hw.to_ascii_lowercase().contains("audiocodec");
            if !is_ours {
                continue;
            }

            let cur = read_reg_sz(&props, PROP_DEVICE_DESC).unwrap_or_default();
            if cur == name {
                already += 1;
                log(&format!("ok {guid} already '{name}'"));
            } else if write_desc(&guid, &name) {
                changed += 1;
                log(&format!("wrote {guid} dev='{dev}' hw='{hw}' (was '{cur}')"));
            }
        }
        let _ = RegCloseKey(hbase);

        if changed == 0 && already == 0 {
            eprintln!("No LibrePods capture endpoint found (run elevated).");
            std::process::exit(1);
        }

        if changed > 0 {
            let _ = Command::new("powershell")
                .args([
                    "-NoProfile",
                    "-Command",
                    "Restart-Service AudioEndpointBuilder -Force",
                ])
                .status();
            println!("Renamed {changed} LibrePods mic endpoint(s) to \"{name}\".");
        } else {
            println!("Already named \"{name}\" ({already} endpoint(s)) — nothing to do.");
        }
    }
}
