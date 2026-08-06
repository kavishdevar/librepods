//! Windows media control via the System Media Transport Controls (SMTC) — the
//! same session API every media app registers with, and the one MagicPods uses.
//! This drives play/pause/skip on whatever app owns the media session.
//!
//! The `MediaControl` methods are sync and run on `spawn_blocking` pool threads,
//! so each ensures COM (MTA) is initialized on the current thread first.

use crate::platform::MediaControl;
use std::sync::Arc;
use windows::Media::Control::{
    GlobalSystemMediaTransportControlsSession as Session,
    GlobalSystemMediaTransportControlsSessionManager as SessionManager,
    GlobalSystemMediaTransportControlsSessionPlaybackStatus as PlaybackStatus,
};
use windows::Win32::System::Com::{COINIT_MULTITHREADED, CoInitializeEx};

pub fn media_control() -> Arc<dyn MediaControl> {
    Arc::new(WindowsMediaControl)
}

pub struct WindowsMediaControl;

/// Ensure COM is up (MTA) on this thread; idempotent per thread.
fn ensure_com() {
    unsafe {
        let _ = CoInitializeEx(None, COINIT_MULTITHREADED);
    }
}

fn manager() -> windows::core::Result<SessionManager> {
    SessionManager::RequestAsync()?.get()
}

/// All current media sessions.
fn sessions() -> Vec<Session> {
    let mut out = Vec::new();
    if let Ok(mgr) = manager()
        && let Ok(list) = mgr.GetSessions()
    {
        if let Ok(size) = list.Size() {
            for i in 0..size {
                if let Ok(s) = list.GetAt(i) {
                    out.push(s);
                }
            }
        }
    }
    out
}

fn is_session_playing(s: &Session) -> bool {
    s.GetPlaybackInfo()
        .and_then(|i| i.PlaybackStatus())
        .map(|st| st == PlaybackStatus::Playing)
        .unwrap_or(false)
}

fn session_id(s: &Session) -> Option<String> {
    s.SourceAppUserModelId().ok().map(|h| h.to_string())
}

impl MediaControl for WindowsMediaControl {
    fn is_playing(&self) -> bool {
        ensure_com();
        sessions().iter().any(is_session_playing)
    }

    fn pause_playing(&self) -> Vec<String> {
        ensure_com();
        let mut paused = Vec::new();
        for s in sessions() {
            if is_session_playing(&s) && s.TryPauseAsync().and_then(|op| op.get()).unwrap_or(false) {
                if let Some(id) = session_id(&s) {
                    paused.push(id);
                }
            }
        }
        paused
    }

    fn pause_all(&self) {
        ensure_com();
        for s in sessions() {
            if is_session_playing(&s) {
                let _ = s.TryPauseAsync().and_then(|op| op.get());
            }
        }
    }

    fn resume(&self, players: &[String]) {
        ensure_com();
        for s in sessions() {
            if let Some(id) = session_id(&s)
                && players.contains(&id)
            {
                let _ = s.TryPlayAsync().and_then(|op| op.get());
            }
        }
    }

    fn command(&self, command: &str) {
        ensure_com();
        let Ok(mgr) = manager() else { return };
        let Ok(session) = mgr.GetCurrentSession() else {
            return;
        };
        let _ = match command {
            "Next" => session.TrySkipNextAsync().and_then(|op| op.get()),
            "Previous" => session.TrySkipPreviousAsync().and_then(|op| op.get()),
            _ => Ok(false),
        };
    }
}
