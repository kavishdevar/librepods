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

use std::io;

/// A connected, byte-framed L2CAP channel to a device. The AAP/ATT managers
/// talk to the peer only through this — on Linux it wraps a `bluer` SeqPacket,
/// on Windows it bridges to the LibrePodsAAP kernel driver via DeviceIoControl.
#[async_trait::async_trait]
pub trait L2capTransport: Send + Sync {
    /// Send one packet; returns the number of bytes written.
    async fn send(&self, data: &[u8]) -> io::Result<usize>;
    /// Receive one packet into `buf`; `Ok(0)` means the peer closed the channel.
    async fn recv(&self, buf: &mut [u8]) -> io::Result<usize>;
}

/// Audio-output routing for the connected device. On Linux this drives
/// PulseAudio — the AirPods' A2DP card *profile* plus the sink volume. On
/// Windows there is no A2DP profile to toggle (the OS manages it), so the
/// profile calls are no-ops and volume goes through WASAPI. The
/// `MediaController` orchestration is written against this trait, not libpulse.
#[async_trait::async_trait]
pub trait AudioRouter: Send + Sync {
    /// Switch the device's audio card to the best available A2DP (stereo)
    /// profile, restarting the audio server to rediscover it if needed. No-op
    /// where the OS manages A2DP itself.
    async fn activate_a2dp(&self, mac: &str);
    /// Set the device's audio card profile to "off". No-op on Windows.
    async fn deactivate_a2dp(&self, mac: &str);
    /// The device's current output volume as a 0..=100 percentage, if resolvable.
    async fn get_volume(&self, mac: &str) -> Option<u32>;
    /// Set the device's output volume from a 0..=100 percentage.
    async fn set_volume(&self, mac: &str, percent: u32);
}

/// Control of whatever media player currently owns the system's playback
/// session. On Linux this is MPRIS over D-Bus; on Windows it is the System Media
/// Transport Controls (SMTC). All methods are synchronous/blocking — call them
/// from a blocking context (the `MediaController` wraps them in `spawn_blocking`).
pub trait MediaControl: Send + Sync {
    /// Is any player currently playing?
    fn is_playing(&self) -> bool;
    /// Pause every currently-playing player; return their ids so they can be
    /// resumed later with [`MediaControl::resume`].
    fn pause_playing(&self) -> Vec<String>;
    /// Pause every currently-playing player without tracking them.
    fn pause_all(&self);
    /// Resume the given player ids.
    fn resume(&self, players: &[String]);
    /// Send a transport command (`"Next"` / `"Previous"`) to the active player.
    fn command(&self, command: &str);
}

/// A currently-connected Bluetooth device found during discovery.
pub struct DiscoveredDevice {
    pub id: DeviceId,
    pub name: String,
}

/// One Bluetooth-LE advertisement carrying Apple manufacturer data, surfaced by
/// the platform LE scanner (Linux: `bluer::monitor`; Windows: WinRT
/// `BluetoothLEAdvertisementWatcher`). The RPA/IRK match and payload decode stay
/// platform-agnostic in `bluetooth/le.rs`; only this raw source is per-platform.
pub struct LeAdvertisement {
    /// The (usually resolvable-private) address the advert was seen from.
    pub address: DeviceId,
    /// Manufacturer-specific bytes for Apple's company id 0x004C, with the
    /// company id already stripped (matching BlueZ's ManufacturerData layout).
    pub apple_data: Vec<u8>,
}

/// A device connection/disconnection observed by the platform watcher
/// (Linux: org.bluez D-Bus; Windows: WinRT DeviceWatcher).
pub enum BtConnectionEvent {
    Connected {
        id: DeviceId,
        name: String,
        uuids: Vec<String>,
    },
    Disconnected {
        id: DeviceId,
    },
}

#[cfg(target_os = "linux")]
mod linux;
#[cfg(target_os = "linux")]
pub use linux::{
    DeviceId, LinuxPlatform as Platform, audio_router, connect_device, find_connected_airpods,
    find_other_managed_devices, l2cap_connect, media_control, power_on_adapter, spawn_tray,
    watch_connections, watch_le_advertisements,
};

/// Handle to the running system tray. Callers hold `Option<TrayHandle>` and
/// refresh the tray via `handle.update(|t: &mut MyTray| ...).await`. On Linux
/// this is the ksni handle; the Windows backend mirrors the same API.
#[cfg(target_os = "linux")]
pub type TrayHandle = ksni::Handle<crate::ui::tray::MyTray>;

#[cfg(target_os = "windows")]
mod windows;
#[cfg(target_os = "windows")]
pub use windows::{DeviceId, WindowsPlatform as Platform, l2cap_connect};

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
