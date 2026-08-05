mod paths;
pub use paths::LinuxPlatform;

mod transport;
pub use transport::l2cap_connect;

/// Bluetooth device identity. On Linux this is BlueZ's MAC address type, so
/// the whole codebase depends on `platform::DeviceId` rather than `bluer`
/// directly. All of `Display`/`FromStr`/`Hash`/`Serialize` carry over.
pub type DeviceId = bluer::Address;
