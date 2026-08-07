//! Linux audio routing via PulseAudio (libpulse). Owns the AirPods' A2DP card
//! profile selection + the sink-volume control used by conversational-awareness
//! ducking. This is the Linux backend of `platform::AudioRouter`; the
//! platform-agnostic `MediaController` never touches libpulse directly.

use crate::platform::AudioRouter;
use libpulse_binding::callbacks::ListResult;
use libpulse_binding::context::introspect::SinkInfo;
use libpulse_binding::context::{Context, FlagSet as ContextFlagSet};
use libpulse_binding::def::Retval;
use libpulse_binding::mainloop::standard::Mainloop;
use libpulse_binding::operation::State as OperationState;
use libpulse_binding::proplist::Proplist;
use libpulse_binding::volume::{ChannelVolumes, Volume};
use log::{debug, error, info, warn};
use std::cell::RefCell;
use std::process::Command;
use std::rc::Rc;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Mutex;

/// Construct the Linux audio router.
pub fn audio_router() -> Arc<dyn AudioRouter> {
    Arc::new(LinuxAudioRouter::new())
}

#[derive(Default)]
struct AudioState {
    /// Cached PulseAudio card index for the connected device.
    device_index: Option<u32>,
    /// Cached best A2DP profile name, re-validated before reuse.
    cached_a2dp_profile: String,
}

pub struct LinuxAudioRouter {
    inner: Mutex<AudioState>,
}

impl LinuxAudioRouter {
    fn new() -> Self {
        LinuxAudioRouter {
            inner: Mutex::new(AudioState::default()),
        }
    }

    /// Best available A2DP profile for the cached device index, preferring the
    /// last one we used, then higher-quality codecs.
    async fn preferred_a2dp_profile(&self) -> String {
        let (device_index, cached_profile) = {
            let s = self.inner.lock().await;
            (s.device_index, s.cached_a2dp_profile.clone())
        };
        let index = match device_index {
            Some(i) => i,
            None => return String::new(),
        };

        if !cached_profile.is_empty() && profile_available(index, &cached_profile).await {
            debug!("Using cached A2DP profile: {}", cached_profile);
            return cached_profile;
        }

        for profile in ["a2dp-sink-sbc_xq", "a2dp-sink-sbc", "a2dp-sink"] {
            if profile_available(index, profile).await {
                info!("Selected best available A2DP profile: {}", profile);
                self.inner.lock().await.cached_a2dp_profile = profile.to_string();
                return profile.to_string();
            }
        }
        debug!("No suitable profile found");
        String::new()
    }
}

#[async_trait::async_trait]
impl AudioRouter for LinuxAudioRouter {
    async fn activate_a2dp(&self, mac: &str) {
        debug!("Entering activate_a2dp");
        if mac.is_empty() {
            warn!("Connected device MAC is empty, cannot activate A2DP profile");
            return;
        }

        let mut current_device_index = self.inner.lock().await.device_index;
        if current_device_index.is_none() {
            warn!("Device index not found, trying to get it.");
            current_device_index = audio_device_index(mac).await;
            if let Some(idx) = current_device_index {
                self.inner.lock().await.device_index = Some(idx);
            } else {
                warn!("Could not get device index. Cannot activate A2DP profile.");
                return;
            }
        }

        let idx = self.inner.lock().await.device_index;
        let mut idx = match idx {
            Some(i) => i,
            None => return,
        };
        if !a2dp_available(idx).await {
            warn!("A2DP profile not available, attempting to restart WirePlumber");
            if restart_wire_plumber().await {
                let ni = audio_device_index(mac).await;
                self.inner.lock().await.device_index = ni;
                debug!("Updated device_index after WirePlumber restart: {:?}", ni);
                match ni {
                    Some(i) if a2dp_available(i).await => idx = i,
                    _ => {
                        error!("A2DP profile still not available after WirePlumber restart");
                        return;
                    }
                }
            } else {
                error!("Could not restart WirePlumber, A2DP profile unavailable");
                return;
            }
        }

        let preferred_profile = self.preferred_a2dp_profile().await;
        if preferred_profile.is_empty() {
            error!("No suitable A2DP profile found");
            return;
        }

        info!("Activating A2DP profile for AirPods: {}", preferred_profile);
        let profile_name = preferred_profile.clone();
        let success =
            tokio::task::spawn_blocking(move || set_card_profile_sync(idx, &profile_name))
                .await
                .unwrap_or(false);

        if success {
            info!("Successfully activated A2DP profile: {}", preferred_profile);
        } else {
            warn!("Failed to activate A2DP profile: {}", preferred_profile);
        }
    }

    async fn deactivate_a2dp(&self, mac: &str) {
        debug!("Entering deactivate_a2dp");
        let mut idx = self.inner.lock().await.device_index;
        if idx.is_none() {
            idx = audio_device_index(mac).await;
            self.inner.lock().await.device_index = idx;
        }

        if mac.is_empty() || idx.is_none() {
            warn!("Connected device MAC or index is empty, cannot deactivate A2DP profile");
            return;
        }
        let device_index = idx.unwrap();

        info!("Deactivating A2DP profile for AirPods by setting to off");
        let success = tokio::task::spawn_blocking(move || {
            std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
                set_card_profile_sync(device_index, "off")
            }))
            .unwrap_or_else(|e| {
                warn!("Panic in set_card_profile_sync: {:?}", e);
                false
            })
        })
        .await
        .unwrap_or(false);

        if success {
            info!("Successfully deactivated A2DP profile");
        } else {
            warn!("Failed to deactivate A2DP profile");
        }
    }

    async fn get_volume(&self, mac: &str) -> Option<u32> {
        let sink = get_sink_name_by_mac(mac).await?;
        tokio::task::spawn_blocking(move || get_sink_volume_percent_by_name_sync(&sink))
            .await
            .unwrap_or(None)
    }

    async fn set_volume(&self, mac: &str, percent: u32) {
        if let Some(sink) = get_sink_name_by_mac(mac).await {
            let _ = tokio::task::spawn_blocking(move || transition_sink_volume(&sink, percent)).await;
        }
    }
}

// --- Owned copies of libpulse introspection results (the borrowed ones can't
// leave the mainloop callback). ---

#[derive(Clone, Debug)]
struct OwnedCardProfileInfo {
    name: Option<String>,
}

#[derive(Clone, Debug)]
struct OwnedCardInfo {
    index: u32,
    proplist: Proplist,
    profiles: Vec<OwnedCardProfileInfo>,
}

#[derive(Clone, Debug)]
struct OwnedSinkInfo {
    name: Option<String>,
    proplist: Proplist,
    volume: ChannelVolumes,
}

// --- PulseAudio helpers (blocking; call from spawn_blocking) ---

fn pulse_connect() -> Option<(Mainloop, Context)> {
    let mut mainloop = Mainloop::new()?;
    let mut context = Context::new(&mainloop, "LibrePods")?;
    context.connect(None, ContextFlagSet::NOAUTOSPAWN, None).ok()?;
    loop {
        mainloop.iterate(false);
        match context.get_state() {
            libpulse_binding::context::State::Ready => break,
            libpulse_binding::context::State::Failed
            | libpulse_binding::context::State::Terminated => return None,
            _ => {}
        }
    }
    Some((mainloop, context))
}

fn get_card_info_list_sync() -> Vec<OwnedCardInfo> {
    let (mut mainloop, context) = match pulse_connect() {
        Some(c) => c,
        None => return vec![],
    };

    let introspector = context.introspect();
    let cards: Rc<RefCell<Vec<OwnedCardInfo>>> = Rc::new(RefCell::new(Vec::new()));
    let op = introspector.get_card_info_list({
        let cards = cards.clone();
        move |result| {
            if let ListResult::Item(item) = result {
                let profiles = item
                    .profiles
                    .iter()
                    .map(|p| OwnedCardProfileInfo {
                        name: p.name.as_ref().map(|n| n.to_string()),
                    })
                    .collect();
                cards.borrow_mut().push(OwnedCardInfo {
                    index: item.index,
                    proplist: item.proplist.clone(),
                    profiles,
                });
            }
        }
    });

    while op.get_state() == OperationState::Running {
        mainloop.iterate(false);
    }
    mainloop.quit(Retval(0));
    Rc::try_unwrap(cards).unwrap().into_inner()
}

fn get_sink_volume_percent_by_name_sync(sink_name: &str) -> Option<u32> {
    let (mut mainloop, context) = pulse_connect()?;
    let introspector = context.introspect();
    let sink_info: Rc<RefCell<Option<OwnedSinkInfo>>> = Rc::new(RefCell::new(None));
    let op = introspector.get_sink_info_by_name(sink_name, {
        let sink_info = sink_info.clone();
        move |result: ListResult<&SinkInfo>| {
            if let ListResult::Item(item) = result {
                *sink_info.borrow_mut() = Some(OwnedSinkInfo {
                    name: item.name.as_ref().map(|s| s.to_string()),
                    proplist: item.proplist.clone(),
                    volume: item.volume,
                });
            }
        }
    });
    while op.get_state() == OperationState::Running {
        mainloop.iterate(false);
    }
    mainloop.quit(Retval(0));

    let borrowed = sink_info.borrow();
    let info = borrowed.as_ref()?;
    let channels = info.volume.len();
    if channels == 0 {
        return None;
    }
    let total: f64 = info.volume.get().iter().map(|v| v.0 as f64).sum();
    let percent = ((total / channels as f64 / Volume::NORMAL.0 as f64) * 100.0).round() as u32;
    Some(percent)
}

fn set_card_profile_sync(card_index: u32, profile_name: &str) -> bool {
    let (mut mainloop, context) = match pulse_connect() {
        Some(c) => c,
        None => return false,
    };
    let mut introspector = context.introspect();
    let op = introspector.set_card_profile_by_index(card_index, profile_name, None);
    while op.get_state() == OperationState::Running {
        mainloop.iterate(false);
    }
    mainloop.quit(Retval(0));
    true
}

fn transition_sink_volume(sink_name: &str, target_volume: u32) -> bool {
    let (mut mainloop, context) = match pulse_connect() {
        Some(c) => c,
        None => return false,
    };
    let mut introspector = context.introspect();
    let sink_info: Rc<RefCell<Option<OwnedSinkInfo>>> = Rc::new(RefCell::new(None));
    let op = introspector.get_sink_info_by_name(sink_name, {
        let sink_info = sink_info.clone();
        move |result: ListResult<&SinkInfo>| {
            if let ListResult::Item(item) = result {
                *sink_info.borrow_mut() = Some(OwnedSinkInfo {
                    name: item.name.as_ref().map(|s| s.to_string()),
                    proplist: item.proplist.clone(),
                    volume: item.volume,
                });
            }
        }
    });
    while op.get_state() == OperationState::Running {
        mainloop.iterate(false);
    }

    if let Some(info) = sink_info.borrow().as_ref() {
        let channels = info.volume.len();
        let mut new_volumes = ChannelVolumes::default();
        let raw =
            (((target_volume as f64) / 100.0) * Volume::NORMAL.0 as f64).round() as u32;
        new_volumes.set(channels, Volume(raw));

        let op = introspector.set_sink_volume_by_name(sink_name, &new_volumes, None);
        while op.get_state() == OperationState::Running {
            mainloop.iterate(false);
        }
        mainloop.quit(Retval(0));
        true
    } else {
        error!("Sink not found: {}", sink_name);
        false
    }
}

// --- async wrappers over the blocking helpers ---

async fn a2dp_available(index: u32) -> bool {
    tokio::task::spawn_blocking(move || {
        let available = get_card_info_list_sync()
            .iter()
            .find(|c| c.index == index)
            .map(|card| {
                card.profiles
                    .iter()
                    .any(|p| p.name.as_ref().is_some_and(|n| n.starts_with("a2dp-sink")))
            })
            .unwrap_or(false);
        debug!("A2DP profile available: {}", available);
        available
    })
    .await
    .unwrap_or(false)
}

async fn profile_available(card_index: u32, profile: &str) -> bool {
    let profile_name = profile.to_string();
    tokio::task::spawn_blocking(move || {
        let available = get_card_info_list_sync()
            .iter()
            .find(|c| c.index == card_index)
            .map(|card| {
                card.profiles
                    .iter()
                    .any(|p| p.name.as_ref() == Some(&profile_name))
            })
            .unwrap_or(false);
        debug!("Profile {} available: {}", profile_name, available);
        available
    })
    .await
    .unwrap_or(false)
}

async fn restart_wire_plumber() -> bool {
    info!("Restarting WirePlumber to rediscover A2DP profiles");
    let result = Command::new("systemctl")
        .args(["--user", "restart", "wireplumber"])
        .output();

    match result {
        Ok(output) if output.status.success() => {
            info!("WirePlumber restarted successfully");
            tokio::time::sleep(Duration::from_secs(2)).await;
            true
        }
        _ => {
            error!("Failed to restart WirePlumber. Do you use wireplumber?");
            false
        }
    }
}

async fn audio_device_index(mac: &str) -> Option<u32> {
    if mac.is_empty() {
        return None;
    }
    let mac_clone = mac.to_string();

    tokio::task::spawn_blocking(move || {
        for card in get_card_info_list_sync() {
            if let Some(device_string) = card.proplist.get_str("device.string")
                && device_string.contains(&mac_clone)
            {
                info!("Found audio device index for MAC {}: {}", mac_clone, card.index);
                return Some(card.index);
            }
        }
        error!("No matching Bluetooth card found for MAC address: {}", mac_clone);
        None
    })
    .await
    .unwrap_or(None)
}

async fn get_sink_name_by_mac(mac: &str) -> Option<String> {
    if mac.is_empty() {
        return None;
    }
    let mac_clone = mac.to_string();

    tokio::task::spawn_blocking(move || {
        let (mut mainloop, context) = pulse_connect()?;
        let introspector = context.introspect();
        let sink_list: Rc<RefCell<Vec<OwnedSinkInfo>>> = Rc::new(RefCell::new(Vec::new()));
        let op = introspector.get_sink_info_list({
            let sink_list = sink_list.clone();
            move |result: ListResult<&SinkInfo>| {
                if let ListResult::Item(item) = result {
                    sink_list.borrow_mut().push(OwnedSinkInfo {
                        name: item.name.as_ref().map(|s| s.to_string()),
                        proplist: item.proplist.clone(),
                        volume: item.volume,
                    });
                }
            }
        });
        while op.get_state() == OperationState::Running {
            mainloop.iterate(false);
        }
        mainloop.quit(Retval(0));

        for sink in sink_list.borrow().iter() {
            if let Some(device_string) = sink.proplist.get_str("device.string")
                && device_string.to_uppercase().contains(&mac_clone.to_uppercase())
                && let Some(name) = &sink.name
            {
                info!("Found sink name for MAC {}: {}", mac_clone, name);
                return Some(name.to_string());
            }
            if let Some(bluez_path) = sink.proplist.get_str("bluez.path") {
                let mac_from_path = bluez_path
                    .split('/')
                    .next_back()
                    .unwrap_or("")
                    .replace("dev_", "")
                    .replace('_', ":");
                if mac_from_path.eq_ignore_ascii_case(&mac_clone)
                    && let Some(name) = &sink.name
                {
                    info!("Found sink name for MAC {}: {}", mac_clone, name);
                    return Some(name.to_string());
                }
            }
        }
        error!("No matching sink found for MAC address: {}", mac_clone);
        None
    })
    .await
    .unwrap_or(None)
}
