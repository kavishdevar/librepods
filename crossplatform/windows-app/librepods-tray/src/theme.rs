//! Windows light/dark theme detection via the registry. Windows tracks two
//! flags: `AppsUseLightTheme` (app windows — our overlay) and
//! `SystemUsesLightTheme` (the shell/taskbar — our tray icon). They can differ,
//! so read the one that matches the surface being drawn.

use windows_sys::Win32::System::LibraryLoader::{GetProcAddress, LoadLibraryW};
use windows_sys::Win32::System::Registry::{RegGetValueW, HKEY_CURRENT_USER, RRF_RT_REG_DWORD};

fn dword(value: &str) -> Option<u32> {
    let sub: Vec<u16> = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
        .encode_utf16()
        .chain(std::iter::once(0))
        .collect();
    let val: Vec<u16> = value.encode_utf16().chain(std::iter::once(0)).collect();
    let mut data: u32 = 0;
    let mut size = 4u32;
    let r = unsafe {
        RegGetValueW(
            HKEY_CURRENT_USER,
            sub.as_ptr(),
            val.as_ptr(),
            RRF_RT_REG_DWORD,
            std::ptr::null_mut(),
            &mut data as *mut u32 as *mut core::ffi::c_void,
            &mut size,
        )
    };
    if r == 0 { Some(data) } else { None }
}

/// True if app windows use the dark theme. Defaults to dark if the key is
/// missing (the common Windows default, and our historical look).
pub fn apps_dark() -> bool {
    dword("AppsUseLightTheme").map(|v| v == 0).unwrap_or(true)
}

/// True if the system (taskbar) uses the dark theme. Governs the tray-icon
/// contrast. Defaults to dark if unknown.
pub fn system_dark() -> bool {
    dword("SystemUsesLightTheme").map(|v| v == 0).unwrap_or(true)
}

/// Make Win32 context menus (the tray's right-click menu) follow the app theme,
/// via the undocumented uxtheme ordinals SetPreferredAppMode (135) +
/// FlushMenuThemes (136). Win32 menus don't honor dark mode otherwise. No-op on
/// OSes without these exports. Call at startup and whenever the theme may change.
pub fn apply_menu_theme() {
    unsafe {
        let name: Vec<u16> = "uxtheme.dll".encode_utf16().chain(std::iter::once(0)).collect();
        let lib = LoadLibraryW(name.as_ptr());
        if lib.is_null() {
            return;
        }
        // SetPreferredAppMode(PreferredAppMode): 2 = ForceDark, 3 = ForceLight.
        if let Some(set) = GetProcAddress(lib, 135usize as *const u8) {
            let set: unsafe extern "system" fn(i32) -> i32 = std::mem::transmute(set);
            set(if apps_dark() { 2 } else { 3 });
        }
        if let Some(flush) = GetProcAddress(lib, 136usize as *const u8) {
            let flush: unsafe extern "system" fn() = std::mem::transmute(flush);
            flush();
        }
    }
}
