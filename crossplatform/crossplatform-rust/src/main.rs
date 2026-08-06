mod bluetooth;
mod devices;
mod media_controller;
mod platform;
mod ui;
mod utils;

use crate::bluetooth::le::start_le_monitor;
use crate::bluetooth::managers::DeviceManagers;
use crate::devices::enums::DeviceData;
use crate::ui::messages::BluetoothUIMessage;
use crate::ui::tray::MyTray;
use crate::platform::{find_connected_airpods, find_other_managed_devices, get_devices_path};
use clap::Parser;
use devices::airpods::AirPodsDevice;
use ksni::TrayMethods;
use log::{debug, info, warn};
use std::collections::HashMap;
use std::env;
use std::sync::atomic::{AtomicBool};
use std::sync::Arc;
use tokio::sync::RwLock;
use tokio::sync::mpsc::unbounded_channel;

#[derive(Parser)]
struct Args {
    #[arg(long, short = 'd', help = "Enable debug logging")]
    debug: bool,
    #[arg(
        long,
        help = "Disable system tray, useful if your environment doesn't support AppIndicator or StatusNotifier"
    )]
    no_tray: bool,
    #[arg(long, help = "Start the application minimized to tray")]
    start_minimized: bool,
    #[arg(
        long,
        help = "Enable Bluetooth LE debug logging. Only use when absolutely necessary; this produces a lot of logs."
    )]
    le_debug: bool,
    #[arg(long, short = 'v', help = "Show application version and exit")]
    version: bool
}

fn main() -> iced::Result {
    let args = Args::parse();

    if args.version {
        println!(
            "You are running LibrePods version {}",
            env!("CARGO_PKG_VERSION")
        );
        return Ok(());
    }

    let log_level = if args.debug { "debug" } else { "info" };
    // let wayland_display = env::var("WAYLAND_DISPLAY").is_ok();
    // if wayland_display && env::var("WGPU_BACKEND").is_err() {
    //     unsafe { env::set_var("WGPU_BACKEND", "gl") };
    // }
    if env::var("RUST_LOG").is_err() {
        unsafe {
            env::set_var(
                "RUST_LOG",
                log_level.to_owned()
                    + &format!(
                        ",zbus=warn,winit=warn,tracing=warn,iced_wgpu=warn,wgpu_hal=warn,wgpu_core=warn,cosmic_text=warn,naga=warn,iced_winit=warn,librepods::bluetooth::le={}",
                        if args.le_debug { "debug" } else { "info" }
                    ),
            )
        };
    }
    env_logger::init();

    let (ui_tx, ui_rx) = unbounded_channel::<BluetoothUIMessage>();

    let device_managers: Arc<RwLock<HashMap<String, DeviceManagers>>> =
        Arc::new(RwLock::new(HashMap::new()));

    // Load stem_control initial value from settings JSON, then apply CLI override.
    if args.no_tray {
        // Run headless without UI
        info!("Running in headless mode (no GUI)");
        let rt = tokio::runtime::Runtime::new().unwrap();
        rt.block_on(async_main(ui_tx, device_managers));
        Ok(())
    } else {
        // Run with UI
        let device_managers_clone = device_managers.clone();
        std::thread::spawn(|| {
            let rt = tokio::runtime::Runtime::new().unwrap();
            rt.block_on(async_main(ui_tx, device_managers_clone));
        });

        ui::window::start_ui(ui_rx, args.start_minimized, device_managers)
    }
}

async fn async_main(
    ui_tx: tokio::sync::mpsc::UnboundedSender<BluetoothUIMessage>,
    device_managers: Arc<RwLock<HashMap<String, DeviceManagers>>>,
) {
    let args = Args::parse();

    let mut managed_devices_mac: Vec<String> = Vec::new(); // includes ony non-AirPods. AirPods handled separately.

    let devices_path = get_devices_path();
    let devices_json = std::fs::read_to_string(&devices_path).unwrap_or_else(|e| {
        log::error!("Failed to read devices file: {}", e);
        "{}".to_string()
    });
    let devices_list: HashMap<String, DeviceData> = serde_json::from_str(&devices_json)
        .unwrap_or_else(|e| {
            log::error!("Deserialization failed: {}", e);
            HashMap::new()
        });
    for (mac, device_data) in devices_list.iter() {
        if device_data.type_ == devices::enums::DeviceType::Nothing {
            managed_devices_mac.push(mac.clone());
        }
    }

    let tray_handle = if args.no_tray {
        None
    } else {
        let tray = MyTray {
            conversation_detect_enabled: None,
            battery_headphone: None,
            battery_headphone_status: None,
            battery_l: None,
            battery_l_status: None,
            battery_r: None,
            battery_r_status: None,
            battery_c: None,
            battery_c_status: None,
            connected: false,
            listening_mode: None,
            allow_off_option: None,
            command_tx: None,
            ui_tx: Some(ui_tx.clone()),
        };
        let handle = tray.spawn().await.unwrap();
        Some(handle)
    };

    if let Err(e) = crate::platform::power_on_adapter().await {
        log::error!("Failed to power on Bluetooth adapter: {e}");
    }

    let le_tray_clone = tray_handle.clone();
    tokio::spawn(async move {
        info!("Starting LE monitor...");
        start_le_monitor(le_tray_clone).await;
    });

    info!("Listening for new connections.");

    info!("Checking for connected devices...");
    if let Some(device) = find_connected_airpods().await {
        info!("Found connected AirPods: {}, initializing.", device.name);
        let airpods_device =
            AirPodsDevice::new(device.id, tray_handle.clone(), ui_tx.clone()).await;

        let mut managers = device_managers.write().await;
        let dev_managers = DeviceManagers::with_aacp(airpods_device.aacp_manager.clone());
        managers
            .entry(device.id.to_string())
            .or_insert(dev_managers)
            .set_aacp(airpods_device.aacp_manager);
        drop(managers);
        if let Err(e) = ui_tx.send(BluetoothUIMessage::DeviceConnected(device.id.to_string())) {
            warn!("Failed to send DeviceConnected UI message: {:?}", e);
        }
    } else {
        info!("No connected AirPods found.");
    }

    for device in find_other_managed_devices(&managed_devices_mac).await {
        let addr_str = device.id.to_string();
        info!("Found connected managed device: {}, initializing.", addr_str);
        let type_ = devices_list.get(&addr_str).unwrap().type_.clone();
        let ui_tx_clone = ui_tx.clone();
        let device_managers = device_managers.clone();
        let dev_id = device.id;
        tokio::spawn(async move {
            let mut managers = device_managers.write().await;
            if type_ == devices::enums::DeviceType::Nothing {
                let dev = devices::nothing::NothingDevice::new(dev_id, ui_tx_clone.clone()).await;
                let dev_managers = DeviceManagers::with_att(dev.att_manager.clone());
                managers
                    .entry(addr_str.clone())
                    .or_insert(dev_managers)
                    .set_att(dev.att_manager);
                if let Err(e) = ui_tx_clone.send(BluetoothUIMessage::DeviceConnected(addr_str)) {
                    warn!("Failed to send DeviceConnected UI message: {:?}", e);
                }
            }
            drop(managers)
        });
    }

    let mut events = crate::platform::watch_connections();
    info!("Listening for Bluetooth connections...");
    let target_uuid = "74ec2172-0bad-4d01-8f77-997b2be0722a";

    while let Some(event) = events.recv().await {
        match event {
            crate::platform::BtConnectionEvent::Disconnected { id } => {
                if let Err(e) =
                    ui_tx.send(BluetoothUIMessage::DeviceDisconnected(id.to_string()))
                {
                    warn!("Failed to send DeviceDisconnected UI message: {:?}", e);
                }
            }
            crate::platform::BtConnectionEvent::Connected { id, name, uuids } => {
                let addr_str = id.to_string();

                if managed_devices_mac.contains(&addr_str) {
                    info!("Managed device connected: {}, initializing", addr_str);
                    let type_ = devices_list.get(&addr_str).unwrap().type_.clone();
                    if type_ == devices::enums::DeviceType::Nothing {
                        let ui_tx_clone = ui_tx.clone();
                        let device_managers = device_managers.clone();
                        tokio::spawn(async move {
                            let mut managers = device_managers.write().await;
                            let dev =
                                devices::nothing::NothingDevice::new(id, ui_tx_clone.clone()).await;
                            let dev_managers = DeviceManagers::with_att(dev.att_manager.clone());
                            managers
                                .entry(addr_str.clone())
                                .or_insert(dev_managers)
                                .set_att(dev.att_manager);
                            drop(managers);
                            if let Err(e) = ui_tx_clone
                                .send(BluetoothUIMessage::DeviceConnected(addr_str.clone()))
                            {
                                warn!("Failed to send DeviceConnected UI message: {:?}", e);
                            }
                        });
                    }
                    continue;
                }

                if !uuids.iter().any(|u| u.to_lowercase() == target_uuid) {
                    continue;
                }
                info!("AirPods connected: {}, initializing", name);
                let handle_clone = tray_handle.clone();
                let ui_tx_clone = ui_tx.clone();
                let device_managers = device_managers.clone();
                tokio::spawn(async move {
                    let airpods_device =
                        AirPodsDevice::new(id, handle_clone, ui_tx_clone.clone()).await;
                    let mut managers = device_managers.write().await;
                    let dev_managers =
                        DeviceManagers::with_aacp(airpods_device.aacp_manager.clone());
                    managers
                        .entry(addr_str.clone())
                        .or_insert(dev_managers)
                        .set_aacp(airpods_device.aacp_manager);
                    drop(managers);
                    if let Err(e) =
                        ui_tx_clone.send(BluetoothUIMessage::DeviceConnected(addr_str.clone()))
                    {
                        warn!("Failed to send DeviceConnected UI message: {:?}", e);
                    }
                });
            }
        }
    }
}
