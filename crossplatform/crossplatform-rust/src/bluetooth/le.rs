use crate::bluetooth::aacp::BatteryStatus;
use crate::devices::enums::{DeviceData, DeviceInformation, DeviceType};
use crate::platform::{
    DeviceId, TrayHandle, connect_device, get_devices_path, get_preferences_path,
    watch_le_advertisements,
};
use crate::ui::tray::MyTray;
use crate::utils::ah;
use aes::Aes128;
use aes::cipher::Array;
use aes::cipher::{BlockCipherDecrypt, KeyInit};
use hex;
use log::{debug, info};
use serde_json;
use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use tokio::sync::Mutex;

fn decrypt(key: &[u8; 16], data: &[u8; 16]) -> [u8; 16] {
    let cipher = Aes128::new(&Array::from(*key));
    let mut block = Array::from(*data);
    cipher.decrypt_block(&mut block);
    block.into()
}

fn verify_rpa(addr: &str, irk: &[u8; 16]) -> bool {
    let rpa: Vec<u8> = addr
        .split(':')
        .map(|s| u8::from_str_radix(s, 16).unwrap())
        .collect::<Vec<_>>()
        .into_iter()
        .rev()
        .collect();
    if rpa.len() != 6 {
        return false;
    }
    let prand_slice = &rpa[3..6];
    let prand: [u8; 3] = prand_slice.try_into().unwrap();
    let hash_slice = &rpa[0..3];
    let hash: [u8; 3] = hash_slice.try_into().unwrap();
    let computed_hash = ah(irk, &prand);
    debug!(
        "Verifying RPA: addr={}, hash={:?}, computed_hash={:?}",
        addr, hash, computed_hash
    );
    hash == computed_hash
}

pub async fn start_le_monitor(tray_handle: Option<TrayHandle>) {
    let all_devices: HashMap<String, DeviceData> = std::fs::read_to_string(get_devices_path())
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_default();

    let mut verified_macs: HashMap<DeviceId, String> = HashMap::new();
    let mut failed_macs: HashSet<DeviceId> = HashSet::new();
    let connecting_macs = Arc::new(Mutex::new(HashSet::<DeviceId>::new()));

    // The platform layer owns the OS-specific advertisement source; here we only
    // resolve the RPA against our IRKs and decode the Apple payload.
    let mut adverts = watch_le_advertisements();
    debug!("Started LE monitor");

    while let Some(adv) = adverts.recv().await {
        let addr = adv.address;
        let addr_str = addr.to_string();
        let apple_data = adv.apple_data;

        // Resolve which of our known AirPods this (rotating) RPA belongs to,
        // caching hits and misses so we only run the IRK match once per address.
        let matched_airpods_mac: String = if let Some(mac) = verified_macs.get(&addr) {
            mac.clone()
        } else if failed_macs.contains(&addr) {
            continue;
        } else {
            debug!("Checking RPA for device: {}", addr_str);
            let mut found_mac = None;
            for (airpods_mac, device_data) in &all_devices {
                if device_data.type_ == DeviceType::AirPods
                    && let Some(DeviceInformation::AirPods(info)) = &device_data.information
                    && let Ok(irk_bytes) = hex::decode(&info.le_keys.irk)
                    && irk_bytes.len() == 16
                {
                    let irk: [u8; 16] = irk_bytes.as_slice().try_into().unwrap();
                    if verify_rpa(&addr_str, &irk) {
                        info!("Matched our device ({}) with the irk for {}", addr, airpods_mac);
                        verified_macs.insert(addr, airpods_mac.clone());
                        found_mac = Some(airpods_mac.clone());
                        break;
                    }
                }
            }
            match found_mac {
                Some(mac) => mac,
                None => {
                    failed_macs.insert(addr);
                    debug!("Device {} did not match any of our irks", addr);
                    continue;
                }
            }
        };

        let matched_enc_key: Option<[u8; 16]> = all_devices
            .get(&matched_airpods_mac)
            .and_then(|d| d.information.as_ref())
            .and_then(|info| match info {
                DeviceInformation::AirPods(info) => Some(info),
                _ => None,
            })
            .and_then(|info| hex::decode(&info.le_keys.enc_key).ok())
            .filter(|b| b.len() == 16)
            .map(|b| b.as_slice().try_into().unwrap());

        let Some(enc_key) = matched_enc_key else {
            continue;
        };
        if apple_data.len() <= 20 {
            continue;
        }

        let last_16: [u8; 16] = apple_data[apple_data.len() - 16..].try_into().unwrap();
        let decrypted = decrypt(&enc_key, &last_16);
        debug!(
            "Decrypted data from airpods_mac {}: {}",
            matched_airpods_mac,
            hex::encode(decrypted)
        );

        let connection_state = apple_data[10] as usize;
        debug!("Connection state: {}", connection_state);
        if connection_state == 0x00 {
            maybe_auto_connect(addr, &matched_airpods_mac, Arc::clone(&connecting_macs)).await;
        }

        let status = apple_data[5] as usize;
        let primary_left = (status >> 5) & 0x01 == 1;
        let this_in_case = (status >> 6) & 0x01 == 1;
        let xor_factor = primary_left ^ this_in_case;
        let is_left_in_ear = if xor_factor {
            (status & 0x02) != 0
        } else {
            (status & 0x08) != 0
        };
        let is_right_in_ear = if xor_factor {
            (status & 0x08) != 0
        } else {
            (status & 0x02) != 0
        };
        let is_flipped = !primary_left;

        let left_byte_index = if is_flipped { 2 } else { 1 };
        let right_byte_index = if is_flipped { 1 } else { 2 };

        let left_byte = decrypted[left_byte_index] as i32;
        let right_byte = decrypted[right_byte_index] as i32;
        let case_byte = decrypted[3] as i32;

        let (left_battery, left_charging) = if left_byte == 0xff {
            (0, false)
        } else {
            (left_byte & 0x7F, (left_byte & 0x80) != 0)
        };
        let (right_battery, right_charging) = if right_byte == 0xff {
            (0, false)
        } else {
            (right_byte & 0x7F, (right_byte & 0x80) != 0)
        };
        let (case_battery, case_charging) = if case_byte == 0xff {
            (0, false)
        } else {
            (case_byte & 0x7F, (case_byte & 0x80) != 0)
        };

        if let Some(handle) = &tray_handle {
            handle
                .update(|tray: &mut MyTray| {
                    tray.battery_l = if left_byte == 0xff {
                        None
                    } else {
                        Some(left_battery as u8)
                    };
                    tray.battery_l_status = if left_byte == 0xff {
                        Some(BatteryStatus::Disconnected)
                    } else if left_charging {
                        Some(BatteryStatus::Charging)
                    } else {
                        Some(BatteryStatus::NotCharging)
                    };
                    tray.battery_r = if right_byte == 0xff {
                        None
                    } else {
                        Some(right_battery as u8)
                    };
                    tray.battery_r_status = if right_byte == 0xff {
                        Some(BatteryStatus::Disconnected)
                    } else if right_charging {
                        Some(BatteryStatus::Charging)
                    } else {
                        Some(BatteryStatus::NotCharging)
                    };
                    tray.battery_c = if case_byte == 0xff {
                        None
                    } else {
                        Some(case_battery as u8)
                    };
                    tray.battery_c_status = if case_byte == 0xff {
                        Some(BatteryStatus::Disconnected)
                    } else if case_charging {
                        Some(BatteryStatus::Charging)
                    } else {
                        Some(BatteryStatus::NotCharging)
                    };
                })
                .await;
        }

        debug!(
            "Battery status: Left: {}, Right: {}, Case: {}, InEar: L:{} R:{}",
            if left_byte == 0xff {
                "disconnected".to_string()
            } else {
                format!("{}% (charging: {})", left_battery, left_charging)
            },
            if right_byte == 0xff {
                "disconnected".to_string()
            } else {
                format!("{}% (charging: {})", right_battery, right_charging)
            },
            if case_byte == 0xff {
                "disconnected".to_string()
            } else {
                format!("{}% (charging: {})", case_battery, case_charging)
            },
            is_left_in_ear,
            is_right_in_ear
        );
    }
}

/// If auto-connect is enabled for this device and no connect is already in
/// flight for this address, spawn a background connect via the platform.
async fn maybe_auto_connect(
    addr: DeviceId,
    airpods_mac: &str,
    connecting_macs: Arc<Mutex<HashSet<DeviceId>>>,
) {
    let pref_path = get_preferences_path();
    let preferences: HashMap<String, HashMap<String, bool>> = std::fs::read_to_string(&pref_path)
        .ok()
        .and_then(|s| serde_json::from_str(&s).ok())
        .unwrap_or_default();
    let auto_connect = preferences
        .get(airpods_mac)
        .and_then(|prefs| prefs.get("autoConnect"))
        .copied()
        .unwrap_or(true);
    debug!("Auto-connect preference for {}: {}", airpods_mac, auto_connect);
    if !auto_connect {
        debug!("Auto-connect is disabled for {}, not attempting to connect.", airpods_mac);
        return;
    }

    // Connect using the device's *identity* MAC (its stored key), not the
    // rotating RPA we observed the advert from.
    let identity: DeviceId = match airpods_mac.parse() {
        Ok(id) => id,
        Err(e) => {
            debug!("Stored MAC {} is not a valid address: {}", airpods_mac, e);
            return;
        }
    };

    // De-duplicate concurrent attempts, keyed by the observed address.
    {
        let mut cm = connecting_macs.lock().await;
        if cm.contains(&addr) {
            info!("Already connecting to {}, skipping duplicate attempt.", airpods_mac);
            return;
        }
        cm.insert(addr);
    }

    let airpods_mac = airpods_mac.to_string();
    tokio::spawn(async move {
        info!("AirPods are disconnected, attempting to connect to {}", airpods_mac);
        match connect_device(&identity).await {
            Ok(()) => info!("Successfully connected to AirPods {}", airpods_mac),
            Err(e) => info!("Failed to connect to AirPods {}: {}", airpods_mac, e),
        }
        connecting_macs.lock().await.remove(&addr);
    });
}
