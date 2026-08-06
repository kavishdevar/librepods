//! Linux media control via MPRIS over D-Bus. Enumerates the `org.mpris.*`
//! players and drives play/pause/next/previous on them. This is the Linux
//! backend of `platform::MediaControl`; the platform-agnostic `MediaController`
//! calls it (wrapped in `spawn_blocking`, since these are blocking D-Bus calls).

use crate::platform::MediaControl;
use dbus::blocking::Connection;
use dbus::blocking::stdintf::org_freedesktop_dbus::Properties;
use log::{debug, error, info, warn};
use std::sync::Arc;
use std::time::Duration;

/// Construct the Linux (MPRIS) media control.
pub fn media_control() -> Arc<dyn MediaControl> {
    Arc::new(LinuxMediaControl)
}

pub struct LinuxMediaControl;

impl LinuxMediaControl {
    fn is_kdeconnect_service(service: &str) -> bool {
        service.starts_with("org.mpris.MediaPlayer2.kdeconnect.mpris_")
    }

    fn list_mpris_services(conn: &Connection) -> Vec<String> {
        let proxy = conn.with_proxy(
            "org.freedesktop.DBus",
            "/org/freedesktop/DBus",
            Duration::from_secs(5),
        );

        let (names,): (Vec<String>,) =
            match proxy.method_call("org.freedesktop.DBus", "ListNames", ()) {
                Ok(n) => n,
                Err(_) => return vec![],
            };

        names
            .into_iter()
            .filter(|s| s.starts_with("org.mpris.MediaPlayer2.") && !Self::is_kdeconnect_service(s))
            .collect()
    }
}

impl MediaControl for LinuxMediaControl {
    fn is_playing(&self) -> bool {
        let conn = match Connection::new_session() {
            Ok(c) => c,
            Err(_) => return false,
        };
        Self::list_mpris_services(&conn).iter().any(|service| {
            let proxy = conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
            proxy
                .get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                .map(|s| s == "Playing")
                .unwrap_or(false)
        })
    }

    fn pause_playing(&self) -> Vec<String> {
        let conn = match Connection::new_session() {
            Ok(c) => c,
            Err(_) => return vec![],
        };
        let mut paused_services = Vec::new();

        for service in Self::list_mpris_services(&conn) {
            debug!("Checking playback status for service: {}", service);
            let proxy =
                conn.with_proxy(&service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));

            if let Ok(playback_status) =
                proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                && playback_status == "Playing"
            {
                if proxy
                    .method_call::<(), _, &str, &str>("org.mpris.MediaPlayer2.Player", "Pause", ())
                    .is_ok()
                {
                    info!("Paused playback for: {}", service);
                    paused_services.push(service);
                } else {
                    error!("Failed to pause {}", service);
                }
            }
        }
        paused_services
    }

    fn pause_all(&self) {
        let conn = match Connection::new_session() {
            Ok(c) => c,
            Err(_) => return,
        };

        for service in Self::list_mpris_services(&conn) {
            let proxy =
                conn.with_proxy(&service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
            if let Ok(playback_status) =
                proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                && playback_status == "Playing"
            {
                if proxy
                    .method_call::<(), _, &str, &str>("org.mpris.MediaPlayer2.Player", "Pause", ())
                    .is_ok()
                {
                    info!("Paused playback for: {}", service);
                } else {
                    error!("Failed to pause {}", service);
                }
            }
        }
    }

    fn resume(&self, players: &[String]) {
        let conn = match Connection::new_session() {
            Ok(c) => c,
            Err(_) => return,
        };
        for service in players {
            let proxy =
                conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
            if proxy
                .method_call::<(), _, &str, &str>("org.mpris.MediaPlayer2.Player", "Play", ())
                .is_ok()
            {
                info!("Resumed playback for: {}", service);
            } else {
                warn!("Failed to resume {}", service);
            }
        }
    }

    fn command(&self, command: &str) {
        let conn = match Connection::new_session() {
            Ok(c) => c,
            Err(_) => return,
        };

        let services = Self::list_mpris_services(&conn);
        let mut playing = None;
        let mut fallback = None;

        for service in &services {
            let proxy = conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
            if let Ok(status) = proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                && status == "Playing"
                && playing.is_none()
            {
                playing = Some(service);
            }
            if fallback.is_none() {
                fallback = Some(service);
            }
        }

        if let Some(service) = playing.or(fallback) {
            let proxy = conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
            if proxy
                .method_call::<(), _, &str, &str>("org.mpris.MediaPlayer2.Player", command, ())
                .is_ok()
            {
                info!("Sent {} to: {}", command, service);
            } else {
                debug!("Failed to send {} to: {}", command, service);
            }
        }
    }
}
