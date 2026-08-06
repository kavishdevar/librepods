mod paths;
pub use paths::LinuxPlatform;

mod transport;
pub use transport::l2cap_connect;

mod discovery;
pub use discovery::{find_connected_airpods, find_other_managed_devices};

mod watcher;
pub use watcher::{power_on_adapter, watch_connections};

mod le_scan;
pub use le_scan::{connect_device, watch_le_advertisements};

mod audio;
pub use audio::audio_router;

mod media;
pub use media::media_control;

mod tray;
pub use tray::spawn_tray;

/// Bluetooth device identity. On Linux this is BlueZ's MAC address type, so
/// the whole codebase depends on `platform::DeviceId` rather than `bluer`
/// directly. All of `Display`/`FromStr`/`Hash`/`Serialize` carry over.
pub type DeviceId = bluer::Address;
