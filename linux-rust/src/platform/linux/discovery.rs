use crate::platform::{DeviceId, DiscoveredDevice};
use log::{debug, warn};

const AIRPODS_SERVICE: &str = "74ec2172-0bad-4d01-8f77-997b2be0722a";

/// (address, name, advertised service UUIDs) for every connected device.
async fn connected_devices() -> Vec<(DeviceId, String, Vec<uuid::Uuid>)> {
    let session = match bluer::Session::new().await {
        Ok(s) => s,
        Err(e) => {
            warn!("bluer session: {e}");
            return Vec::new();
        }
    };
    let adapter = match session.default_adapter().await {
        Ok(a) => a,
        Err(e) => {
            warn!("default adapter: {e}");
            return Vec::new();
        }
    };

    let addrs = adapter.device_addresses().await.unwrap_or_default();
    let mut out = Vec::new();
    for addr in addrs {
        let Ok(device) = adapter.device(addr) else {
            continue;
        };
        if !device.is_connected().await.unwrap_or(false) {
            continue;
        }
        let name = device
            .name()
            .await
            .ok()
            .flatten()
            .unwrap_or_else(|| "Unknown".to_string());
        let uuids = device
            .uuids()
            .await
            .ok()
            .flatten()
            .map(|s| s.into_iter().collect())
            .unwrap_or_default();
        out.push((addr, name, uuids));
    }
    out
}

/// The connected AirPods (advertising the AAP service), if any.
pub async fn find_connected_airpods() -> Option<DiscoveredDevice> {
    let target = uuid::Uuid::parse_str(AIRPODS_SERVICE).ok()?;
    for (id, name, uuids) in connected_devices().await {
        if uuids.iter().any(|u| *u == target) {
            return Some(DiscoveredDevice { id, name });
        }
    }
    None
}

/// Connected devices whose MAC is in `managed_macs` (non-AirPods managed ones).
pub async fn find_other_managed_devices(managed_macs: &[String]) -> Vec<DiscoveredDevice> {
    connected_devices()
        .await
        .into_iter()
        .filter_map(|(id, name, _)| {
            let mac = id.to_string();
            if managed_macs.iter().any(|m| m == &mac) {
                debug!("Found managed device: {mac}");
                Some(DiscoveredDevice { id, name })
            } else {
                None
            }
        })
        .collect()
}
