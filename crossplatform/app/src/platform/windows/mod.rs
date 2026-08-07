mod paths;
pub use paths::WindowsPlatform;

mod device_id;
pub use device_id::DeviceId;

mod transport;
pub use transport::l2cap_connect;

// Phase 3: IPC client of librepodsd (additive; wired into the GUI incrementally).
#[allow(unused_imports)]
pub mod daemon_client;

mod watcher;
pub use watcher::{local_adapter_address, power_on_adapter, watch_connections};

mod discovery;
pub use discovery::{find_connected_airpods, find_other_managed_devices};

mod le_scan;
pub use le_scan::{connect_device, watch_le_advertisements};

mod audio;
pub use audio::audio_router;

mod media;
pub use media::media_control;

mod tray;
pub use tray::{WindowsTrayHandle, spawn_tray};
