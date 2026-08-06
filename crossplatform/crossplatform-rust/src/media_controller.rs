use crate::bluetooth::aacp::AACPManager;
use crate::bluetooth::aacp::EarDetectionStatus;
use crate::platform::{AudioRouter, audio_router};
use dbus::blocking::Connection;
use dbus::blocking::stdintf::org_freedesktop_dbus::Properties;
use log::{debug, error, info, warn};
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Mutex;

struct MediaControllerState {
    connected_device_mac: String,
    local_mac: String,
    is_playing: bool,
    paused_by_app_services: Vec<String>,
    old_in_ear_data: Vec<bool>,
    user_played_the_media: bool,
    i_paused_the_media: bool,
    ear_detection_enabled: bool,
    disconnect_when_not_wearing: bool,
    conv_original_volume: Option<u32>,
    conv_conversation_started: bool,
    playback_listener_running: bool,
}

impl MediaControllerState {
    fn new() -> Self {
        MediaControllerState {
            connected_device_mac: String::new(),
            local_mac: String::new(),
            is_playing: false,
            paused_by_app_services: Vec::new(),
            old_in_ear_data: vec![false, false],
            user_played_the_media: false,
            i_paused_the_media: false,
            ear_detection_enabled: true,
            disconnect_when_not_wearing: true,
            conv_original_volume: None,
            conv_conversation_started: false,
            playback_listener_running: false,
        }
    }
}

#[derive(Clone)]
pub struct MediaController {
    state: Arc<Mutex<MediaControllerState>>,
    /// Platform audio routing (A2DP profile + volume). Linux = PulseAudio.
    audio: Arc<dyn AudioRouter>,
}

impl MediaController {
    pub fn new(connected_mac: String, local_mac: String) -> Self {
        let mut state = MediaControllerState::new();
        state.connected_device_mac = connected_mac;
        state.local_mac = local_mac;
        MediaController {
            state: Arc::new(Mutex::new(state)),
            audio: audio_router(),
        }
    }

    pub async fn start_playback_listener(
        &self,
        aacp_manager: AACPManager,
        control_tx: tokio::sync::mpsc::UnboundedSender<(
            crate::bluetooth::aacp::ControlCommandIdentifiers,
            Vec<u8>,
        )>,
    ) {
        let mut state = self.state.lock().await;
        if state.playback_listener_running {
            debug!("Playback listener already running");
            return;
        }
        state.playback_listener_running = true;
        drop(state);

        let controller_clone = self.clone();
        tokio::spawn(async move {
            controller_clone
                .playback_listener_loop(aacp_manager, control_tx)
                .await;
        });
    }

    async fn playback_listener_loop(
        &self,
        aacp_manager: AACPManager,
        control_tx: tokio::sync::mpsc::UnboundedSender<(
            crate::bluetooth::aacp::ControlCommandIdentifiers,
            Vec<u8>,
        )>,
    ) {
        info!("Starting playback listener loop");
        loop {
            tokio::time::sleep(Duration::from_millis(500)).await;

            let is_playing = tokio::task::spawn_blocking(|| Self::check_if_playing())
                .await
                .unwrap_or(false);

            let mut state = self.state.lock().await;
            let was_playing = state.is_playing;
            state.is_playing = is_playing;
            let local_mac = state.local_mac.clone();
            drop(state);

            if !was_playing && is_playing {
                let aacp_state = aacp_manager.state.lock().await;
                if !aacp_state
                    .ear_detection_status
                    .contains(&EarDetectionStatus::InEar)
                {
                    info!("Media playback started but buds not in ear, skipping takeover");
                    continue;
                }
                info!("Media playback started, taking ownership and activating a2dp");
                let _ = control_tx.send((
                    crate::bluetooth::aacp::ControlCommandIdentifiers::OwnsConnection,
                    vec![0x01],
                ));
                self.activate_a2dp_profile().await;

                info!("already connected locally, hijacking connection by asking AirPods");

                let connected_devices = aacp_state.connected_devices.clone();
                for device in connected_devices {
                    if device.mac != local_mac {
                        if let Err(e) = aacp_manager
                            .send_media_information(&local_mac, &device.mac, true)
                            .await
                        {
                            error!("Failed to send media information to {}: {}", device.mac, e);
                        }
                        if let Err(e) = aacp_manager.send_smart_routing_show_ui(&device.mac).await {
                            error!(
                                "Failed to send smart routing show ui to {}: {}",
                                device.mac, e
                            );
                        }
                        if let Err(e) = aacp_manager.send_hijack_request(&device.mac).await {
                            error!("Failed to send hijack request to {}: {}", device.mac, e);
                        }
                    }
                }

                debug!("completed playback takeover process");
            }
        }
    }

    fn check_if_playing() -> bool {
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

    fn is_kdeconnect_service(service: &str) -> bool {
        service.starts_with("org.mpris.MediaPlayer2.kdeconnect.mpris_")
    }

    pub async fn handle_ear_detection(
        &self,
        old_statuses: Vec<EarDetectionStatus>,
        new_statuses: Vec<EarDetectionStatus>,
    ) {
        debug!(
            "Entering handle_ear_detection with old_statuses: {:?}, new_statuses: {:?}",
            old_statuses, new_statuses
        );

        let old_in_ear_data: Vec<bool> = old_statuses
            .iter()
            .map(|s| *s == EarDetectionStatus::InEar)
            .collect();
        let new_in_ear_data: Vec<bool> = new_statuses
            .iter()
            .map(|s| *s == EarDetectionStatus::InEar)
            .collect();

        let in_ear = new_in_ear_data.iter().all(|&b| b);
        let old_all_out = old_in_ear_data.iter().all(|&b| !b);
        let new_has_at_least_one_in = new_in_ear_data.iter().any(|&b| b);
        let new_all_out = new_in_ear_data.iter().all(|&b| !b);

        debug!(
            "Computed states: in_ear={}, old_all_out={}, new_has_at_least_one_in={}, new_all_out={}",
            in_ear, old_all_out, new_has_at_least_one_in, new_all_out
        );

        {
            let state = self.state.lock().await;
            if !state.ear_detection_enabled {
                debug!("Ear detection disabled, skipping");
                return;
            }
        }

        if new_has_at_least_one_in && old_all_out {
            debug!("Condition met: buds inserted, activating A2DP and checking play state");
            self.activate_a2dp_profile().await;
            {
                let mut state = self.state.lock().await;
                if state.is_playing {
                    state.user_played_the_media = true;
                    debug!("Set user_played_the_media to true as media was playing");
                }
            }
        } else if new_all_out {
            debug!("Condition met: buds removed, pausing media");
            self.pause().await;
            {
                let state = self.state.lock().await;
                if state.disconnect_when_not_wearing {
                    debug!("Disconnect when not wearing enabled, deactivating A2DP");
                    drop(state);
                    self.deactivate_a2dp_profile().await;
                }
            }
        }

        let reset_user_played = (old_in_ear_data.iter().any(|&b| !b)
            && new_in_ear_data.iter().all(|&b| b))
            || (new_in_ear_data.iter().any(|&b| !b) && old_in_ear_data.iter().all(|&b| b));
        if reset_user_played {
            debug!("Transition detected, resetting user_played_the_media");
            let mut state = self.state.lock().await;
            state.user_played_the_media = false;
        }

        info!(
            "Ear Detection - old_in_ear_data: {:?}, new_in_ear_data: {:?}",
            old_in_ear_data, new_in_ear_data
        );

        let mut old_sorted = old_in_ear_data.clone();
        old_sorted.sort();
        let mut new_sorted = new_in_ear_data.clone();
        new_sorted.sort();
        if new_sorted != old_sorted {
            debug!("Ear data changed, checking resume/pause logic");
            if in_ear {
                debug!("Resuming media as buds are in ear");
                self.resume().await;
                self.state.lock().await.i_paused_the_media = false;
            } else if !old_all_out {
                debug!("Pausing media as buds are not fully in ear");
                self.pause().await;
                self.state.lock().await.i_paused_the_media = true;
            } else {
                debug!("Playing media");
                self.resume().await;
                self.state.lock().await.i_paused_the_media = false;
            }
        }

        {
            let mut state = self.state.lock().await;
            state.old_in_ear_data = new_in_ear_data;
            debug!("Updated old_in_ear_data to {:?}", state.old_in_ear_data);
        }
    }

    pub async fn activate_a2dp_profile(&self) {
        let mac = self.state.lock().await.connected_device_mac.clone();
        self.audio.activate_a2dp(&mac).await;
    }

    async fn pause(&self) {
        debug!("Pausing playback");

        let paused_services = tokio::task::spawn_blocking(|| {
            let conn = match Connection::new_session() {
                Ok(c) => c,
                Err(_) => return vec![],
            };
            let mut paused_services = Vec::new();

            for service in Self::list_mpris_services(&conn) {
                debug!("Checking playback status for service: {}", service);
                let proxy = conn.with_proxy(
                    &service,
                    "/org/mpris/MediaPlayer2",
                    Duration::from_secs(5),
                );

                if let Ok(playback_status) =
                    proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                    && playback_status == "Playing"
                {
                    if proxy
                        .method_call::<(), _, &str, &str>(
                            "org.mpris.MediaPlayer2.Player",
                            "Pause",
                            (),
                        )
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
        })
            .await
            .unwrap();

        if !paused_services.is_empty() {
            info!("Paused {} media player(s) via DBus", paused_services.len());
            let mut state = self.state.lock().await;
            state.paused_by_app_services = paused_services;
            state.is_playing = false;
        } else {
            info!("No playing media players found to pause");
        }
    }

    pub async fn pause_all_media(&self) {
        debug!("Pausing all media (without tracking for resume)");

        let paused_count = tokio::task::spawn_blocking(|| {
            let conn = match Connection::new_session() {
                Ok(c) => c,
                Err(_) => return 0,
            };
            let mut paused_count = 0;

            for service in Self::list_mpris_services(&conn) {
                let proxy =
                    conn.with_proxy(&service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
                if let Ok(playback_status) =
                    proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                    && playback_status == "Playing"
                {
                    if proxy
                        .method_call::<(), _, &str, &str>(
                            "org.mpris.MediaPlayer2.Player",
                            "Pause",
                            (),
                        )
                        .is_ok()
                    {
                        info!("Paused playback for: {}", service);
                        paused_count += 1;
                    } else {
                        error!("Failed to pause {}", service);
                    }
                }
            }
            paused_count
        })
            .await
            .unwrap();

        if paused_count > 0 {
            info!("Paused {} media player(s) due to ownership loss", paused_count);
            self.state.lock().await.is_playing = false;
        } else {
            debug!("No playing media players found to pause");
        }
    }

    async fn resume(&self) {
        debug!("Resuming playback");
        let services = self.state.lock().await.paused_by_app_services.clone();

        if services.is_empty() {
            debug!("No services to resume");
            return;
        }

        let resumed_count = tokio::task::spawn_blocking(move || {
            let conn = match Connection::new_session() {
                Ok(c) => c,
                Err(_) => return 0,
            };
            let mut resumed_count = 0;
            for service in services {
                let proxy =
                    conn.with_proxy(&service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
                if proxy
                    .method_call::<(), _, &str, &str>("org.mpris.MediaPlayer2.Player", "Play", ())
                    .is_ok()
                {
                    info!("Resumed playback for: {}", service);
                    resumed_count += 1;
                } else {
                    warn!("Failed to resume {}", service);
                }
            }
            resumed_count
        })
            .await
            .unwrap();

        if resumed_count > 0 {
            info!("Resumed {} media player(s) via DBus", resumed_count);
            self.state.lock().await.paused_by_app_services.clear();
        } else {
            error!("Failed to resume any media players via DBus");
        }
    }

    pub async fn next_track(&self) {
        info!("Skipping to next track");
        self.mpris_command("Next").await;
    }

    pub async fn previous_track(&self) {
        info!("Going to previous track");
        self.mpris_command("Previous").await;
    }

    pub async fn deactivate_a2dp_profile(&self) {
        let mac = self.state.lock().await.connected_device_mac.clone();
        self.audio.deactivate_a2dp(&mac).await;
    }

    pub async fn handle_conversational_awareness(&self, status: u8) {
        debug!("Entering handle_conversational_awareness with status: {}", status);

        let mac = self.state.lock().await.connected_device_mac.clone();
        if mac.is_empty() {
            debug!("No connected device MAC, skipping conversational awareness");
            return;
        }

        let current_volume_opt = self.audio.get_volume(&mac).await;

        match status {
            1 => {
                let original = current_volume_opt.unwrap_or(0);
                debug!("Conversation start (1). Current volume: {}", original);
                {
                    let mut state = self.state.lock().await;
                    if !state.conv_conversation_started {
                        state.conv_original_volume = Some(original);
                        state.conv_conversation_started = true;
                    } else {
                        debug!(
                            "Conversation already started; not overwriting conv_original_volume"
                        );
                    }
                }
                if original > 25 {
                    self.audio.set_volume(&mac, 25).await;
                    info!(
                        "Conversation start: lowered volume to 25% (original {})",
                        original
                    );
                } else {
                    debug!("Original volume {} <= 25, not reducing to 25", original);
                }
            }
            2 => {
                let original = self.state.lock().await.conv_original_volume;
                if let Some(orig) = original {
                    debug!("Conversation reduce (2). Original: {}", orig);
                    if orig > 15 {
                        self.audio.set_volume(&mac, 15).await;
                        info!(
                            "Conversation reduce: lowered volume to 15% (original {})",
                            orig
                        );
                    } else {
                        debug!("Original {} <= 15, not reducing to 15", orig);
                    }
                } else {
                    debug!("No original volume known for status 2, skipping");
                }
            }
            3 => {
                let (conv_started, conv_original) = {
                    let state = self.state.lock().await;
                    (state.conv_conversation_started, state.conv_original_volume)
                };
                if !conv_started {
                    debug!("Received status 3 but conversation was not started; ignoring increase");
                    return;
                }
                if let Some(orig) = conv_original {
                    let target = orig.min(25);
                    self.audio.set_volume(&mac, target).await;
                    info!(
                        "Conversation partial increase (3): set volume to {} (original {})",
                        target, orig
                    );
                } else if let Some(orig_from_current) = current_volume_opt {
                    let target = orig_from_current.min(25);
                    self.audio.set_volume(&mac, target).await;
                    info!(
                        "Conversation partial increase (3) with fallback current: set volume to {} (measured {})",
                        target, orig_from_current
                    );
                } else {
                    debug!("No original volume known for status 3, skipping");
                }
            }
            4 | 6 | 7 => {
                debug!("Conversation end ({}), restoring volume if needed", status);
                self.restore_volume_if_needed(&mac).await;
            }
            _ => {
                debug!("Conversation status ({}), ignoring", status);
            }
        }
    }

    async fn mpris_command(&self, command: &'static str) {
        tokio::task::spawn_blocking(move || {
            let conn = match Connection::new_session() {
                Ok(c) => c,
                Err(_) => return,
            };

            let services = Self::list_mpris_services(&conn);
            let mut playing = None;
            let mut fallback = None;

            for service in &services {
                let proxy =
                    conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
                if let Ok(status) =
                    proxy.get::<String>("org.mpris.MediaPlayer2.Player", "PlaybackStatus")
                {
                    if status == "Playing" && playing.is_none() {
                        playing = Some(service);
                    }
                }
                if fallback.is_none() {
                    fallback = Some(service);
                }
            }

            if let Some(service) = playing.or(fallback) {
                let proxy =
                    conn.with_proxy(service, "/org/mpris/MediaPlayer2", Duration::from_secs(5));
                if proxy
                    .method_call::<(), _, &str, &str>(
                        "org.mpris.MediaPlayer2.Player",
                        command,
                        (),
                    )
                    .is_ok()
                {
                    info!("Sent {} to: {}", command, service);
                } else {
                    debug!("Failed to send {} to: {}", command, service);
                }
            }
        })
            .await
            .unwrap();
    }

    async fn restore_volume_if_needed(&self, mac: &str) {
        let maybe_original = {
            let mut state = self.state.lock().await;
            if state.conv_conversation_started {
                let orig = state.conv_original_volume;
                state.conv_original_volume = None;
                state.conv_conversation_started = false;
                orig
            } else {
                return;
            }
        };

        if let Some(orig) = maybe_original {
            self.audio.set_volume(mac, orig).await;
        }
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
            .filter(|s| {
                s.starts_with("org.mpris.MediaPlayer2.") && !Self::is_kdeconnect_service(s)
            })
            .collect()
    }
}

// --- PulseAudio helpers ---

