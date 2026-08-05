use crate::platform::{BtConnectionEvent, DeviceId};
use dbus::arg::{RefArg, Variant};
use dbus::blocking::Connection;
use dbus::blocking::stdintf::org_freedesktop_dbus::Properties;
use dbus::message::MatchRule;
use log::warn;
use std::collections::HashMap;
use tokio::sync::mpsc::{UnboundedReceiver, UnboundedSender, unbounded_channel};

/// Power on the default Bluetooth adapter.
pub async fn power_on_adapter() -> Result<(), String> {
    let session = bluer::Session::new().await.map_err(|e| e.to_string())?;
    let adapter = session.default_adapter().await.map_err(|e| e.to_string())?;
    adapter.set_powered(true).await.map_err(|e| e.to_string())?;
    Ok(())
}

/// Watch org.bluez for device connect/disconnect events. The blocking D-Bus
/// loop runs on its own thread and forwards events over an async channel.
pub fn watch_connections() -> UnboundedReceiver<BtConnectionEvent> {
    let (tx, rx) = unbounded_channel();
    std::thread::spawn(move || {
        if let Err(e) = run_watcher(tx) {
            warn!("D-Bus connection watcher stopped: {e}");
        }
    });
    rx
}

fn run_watcher(tx: UnboundedSender<BtConnectionEvent>) -> Result<(), Box<dyn std::error::Error>> {
    let conn = Connection::new_system()?;
    let rule = MatchRule::new_signal("org.freedesktop.DBus.Properties", "PropertiesChanged");
    conn.add_match(rule, move |_: (), conn, msg| {
        let Some(path) = msg.path() else {
            return true;
        };
        if !path.contains("/org/bluez/hci") || !path.contains("/dev_") {
            return true;
        }
        let Ok((iface, changed, _)) =
            msg.read3::<String, HashMap<String, Variant<Box<dyn RefArg>>>, Vec<String>>()
        else {
            return true;
        };
        if iface != "org.bluez.Device1" {
            return true;
        }
        let Some(connected_var) = changed.get("Connected") else {
            return true;
        };
        let Some(is_connected) = connected_var.0.as_ref().as_u64() else {
            return true;
        };

        let proxy = conn.with_proxy("org.bluez", path, std::time::Duration::from_millis(5000));
        let Ok(addr_str) = proxy.get::<String>("org.bluez.Device1", "Address") else {
            return true;
        };
        let Ok(id) = addr_str.parse::<DeviceId>() else {
            return true;
        };

        if is_connected == 0 {
            let _ = tx.send(BtConnectionEvent::Disconnected { id });
            return true;
        }

        let uuids = proxy
            .get::<Vec<String>>("org.bluez.Device1", "UUIDs")
            .unwrap_or_default();
        let name = proxy
            .get::<String>("org.bluez.Device1", "Name")
            .unwrap_or_else(|_| "Unknown".to_string());
        let _ = tx.send(BtConnectionEvent::Connected { id, name, uuids });
        true
    })?;

    loop {
        conn.process(std::time::Duration::from_millis(1000))?;
    }
}
