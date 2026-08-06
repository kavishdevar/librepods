//! Windows device discovery.
//!
//! STUB (Phase I): signatures only. Real backend (WinRT `BluetoothDevice`
//! enumeration filtered by the AAP service UUID) lands in Phase J.

use crate::platform::DiscoveredDevice;

pub async fn find_connected_airpods() -> Option<DiscoveredDevice> {
    None
}

pub async fn find_other_managed_devices(_managed_macs: &[String]) -> Vec<DiscoveredDevice> {
    Vec::new()
}
