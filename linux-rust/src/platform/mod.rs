//! Platform abstraction layer.
//!
//! OS-specific integrations live behind traits whose backend is selected at
//! compile time via `#[cfg(target_os = ...)]`. Only Linux and Windows are
//! supported targets; macOS is out of scope.
//!
//! Phase A introduces only the filesystem-paths seam (`AppPaths`). Later phases
//! add the Bluetooth transport, adapter/discovery, audio routing, media control
//! and the system tray.

use std::path::PathBuf;

/// Locations of the app's on-disk config/data files.
///
/// Implemented once per platform; the active impl is re-exported as
/// [`Platform`]. Call sites use the free `get_*_path` helpers below.
pub trait AppPaths {
    /// `devices.json` — known managed devices, keyed by MAC.
    fn devices_path() -> PathBuf;
    /// `preferences.json`.
    fn preferences_path() -> PathBuf;
    /// `app_settings.json`, migrating any legacy location on the way.
    fn app_settings_path() -> PathBuf;
}

#[cfg(target_os = "linux")]
mod linux;
#[cfg(target_os = "linux")]
pub use linux::{DeviceId, LinuxPlatform as Platform};

#[cfg(target_os = "windows")]
mod windows;
#[cfg(target_os = "windows")]
pub use windows::{DeviceId, WindowsPlatform as Platform};

// Backwards-compatible free functions so existing call sites stay unchanged
// while the implementation moves behind the platform boundary.
pub fn get_devices_path() -> PathBuf {
    Platform::devices_path()
}

pub fn get_preferences_path() -> PathBuf {
    Platform::preferences_path()
}

pub fn get_app_settings_path() -> PathBuf {
    Platform::app_settings_path()
}
