//! Windows system tray via the `tray-icon` crate. Runs its own dedicated thread
//! with a Win32 message loop (tray-icon needs one to deliver menu clicks), and
//! renders from the shared `MyTray` view-model — the same struct the Linux ksni
//! backend uses. A `WM_TIMER` tick refreshes battery/ANC state each second.

use crate::bluetooth::aacp::ControlCommandIdentifiers;
use crate::ui::messages::BluetoothUIMessage;
use crate::ui::tray::MyTray;
use std::sync::{Arc, Mutex};
use tray_icon::menu::{CheckMenuItem, Menu, MenuEvent, MenuId, MenuItem, PredefinedMenuItem};
use tray_icon::{Icon, TrayIconBuilder};
use windows_sys::Win32::UI::WindowsAndMessaging::{
    DispatchMessageW, GetMessageW, MSG, PostQuitMessage, SetTimer, TranslateMessage, WM_TIMER,
};

type Shared = Arc<Mutex<MyTray>>;

#[derive(Clone)]
pub struct WindowsTrayHandle {
    shared: Shared,
}

impl WindowsTrayHandle {
    /// Apply a mutation to the tray view-model. The tray thread's per-second
    /// `WM_TIMER` refresh renders the change; mirrors ksni's `Handle::update`.
    pub async fn update<F: FnOnce(&mut MyTray)>(&self, f: F) {
        if let Ok(mut guard) = self.shared.lock() {
            f(&mut guard);
        }
    }
}

pub async fn spawn_tray(_tray: MyTray) -> Option<WindowsTrayHandle> {
    // On Windows the lightweight `librepods-tray` is the tray now; the full app is
    // window-only (no second icon). Its device state still shows in the window.
    None
}

fn make_icon() -> Icon {
    // A 32x32 teal filled circle on a transparent background.
    let (w, h) = (32u32, 32u32);
    let mut rgba = vec![0u8; (w * h * 4) as usize];
    let (cx, cy, r) = (15.5f32, 15.5f32, 15.0f32);
    for y in 0..h {
        for x in 0..w {
            let (dx, dy) = (x as f32 - cx, y as f32 - cy);
            if dx * dx + dy * dy <= r * r {
                let i = ((y * w + x) * 4) as usize;
                rgba[i] = 0x1a;
                rgba[i + 1] = 0xbc;
                rgba[i + 2] = 0x9c;
                rgba[i + 3] = 0xff;
            }
        }
    }
    Icon::from_rgba(rgba, w, h).expect("icon")
}

fn battery_text(s: &MyTray) -> String {
    if !s.connected {
        return "Disconnected".to_string();
    }
    let f = |v: Option<u8>| v.map(|p| format!("{p}%")).unwrap_or_else(|| "—".into());
    format!("L {}  R {}  Case {}", f(s.battery_l), f(s.battery_r), f(s.battery_c))
}

fn send_anc(shared: &Shared, mode: u8) {
    if let Ok(mut s) = shared.lock() {
        if let Some(tx) = &s.command_tx {
            let _ = tx.send((ControlCommandIdentifiers::ListeningMode, vec![mode]));
        }
        s.listening_mode = Some(mode);
    }
}

fn toggle_conversation(shared: &Shared) {
    if let Ok(mut s) = shared.lock()
        && let Some(tx) = &s.command_tx
        && let Some(is_enabled) = s.conversation_detect_enabled
    {
        let new_state = !is_enabled;
        let value = if new_state { 0x01 } else { 0x02 };
        let _ = tx.send((ControlCommandIdentifiers::ConversationDetectConfig, vec![value]));
        s.conversation_detect_enabled = Some(new_state);
    }
}

fn open_window(shared: &Shared) {
    if let Ok(s) = shared.lock()
        && let Some(tx) = &s.ui_tx
    {
        let _ = tx.send(BluetoothUIMessage::OpenWindow);
    }
}

fn tray_thread(shared: Shared) {
    let battery = MenuItem::new("Connecting…", false, None);
    let anc_header = MenuItem::new("Noise Control", false, None);
    let m_off = CheckMenuItem::new("Off", true, false, None);
    let m_anc = CheckMenuItem::new("Noise Cancellation", true, false, None);
    let m_trans = CheckMenuItem::new("Transparency", true, false, None);
    let m_adapt = CheckMenuItem::new("Adaptive", true, false, None);
    let m_conv = CheckMenuItem::new("Conversation Detection", false, false, None);
    let open = MenuItem::new("Open Window", true, None);
    let quit = MenuItem::new("Quit", true, None);

    let off_id = m_off.id().clone();
    let anc_id = m_anc.id().clone();
    let trans_id = m_trans.id().clone();
    let adapt_id = m_adapt.id().clone();
    let conv_id = m_conv.id().clone();
    let open_id = open.id().clone();
    let quit_id = quit.id().clone();

    let menu = Menu::new();
    let _ = menu.append(&battery);
    let _ = menu.append(&PredefinedMenuItem::separator());
    let _ = menu.append(&anc_header);
    let _ = menu.append(&m_off);
    let _ = menu.append(&m_anc);
    let _ = menu.append(&m_trans);
    let _ = menu.append(&m_adapt);
    let _ = menu.append(&PredefinedMenuItem::separator());
    let _ = menu.append(&m_conv);
    let _ = menu.append(&PredefinedMenuItem::separator());
    let _ = menu.append(&open);
    let _ = menu.append(&quit);

    let tray = match TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip("LibrePods")
        .with_icon(make_icon())
        .build()
    {
        Ok(t) => t,
        Err(e) => {
            log::warn!("Failed to build system tray: {e}");
            return;
        }
    };

    let mode_for = |id: &MenuId| -> Option<u8> {
        if *id == off_id {
            Some(1)
        } else if *id == anc_id {
            Some(2)
        } else if *id == trans_id {
            Some(3)
        } else if *id == adapt_id {
            Some(4)
        } else {
            None
        }
    };

    let refresh = || {
        let s = shared.lock().unwrap();
        battery.set_text(battery_text(&s));
        let mode = s.listening_mode.unwrap_or(0);
        m_off.set_checked(mode == 1);
        m_anc.set_checked(mode == 2);
        m_trans.set_checked(mode == 3);
        m_adapt.set_checked(mode == 4);
        m_off.set_enabled(s.allow_off_option == Some(0x01));
        m_conv.set_enabled(s.conversation_detect_enabled.is_some());
        m_conv.set_checked(s.conversation_detect_enabled.unwrap_or(false));
        let _ = tray.set_tooltip(Some(format!("LibrePods · {}", battery_text(&s))));
    };

    unsafe {
        SetTimer(std::ptr::null_mut(), 1, 1000, None);
        let mut msg: MSG = std::mem::zeroed();
        loop {
            let r = GetMessageW(&mut msg, std::ptr::null_mut(), 0, 0);
            if r <= 0 {
                break; // WM_QUIT or error
            }
            if msg.message == WM_TIMER {
                refresh();
            } else {
                TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
            while let Ok(ev) = MenuEvent::receiver().try_recv() {
                if ev.id == quit_id {
                    PostQuitMessage(0);
                } else if ev.id == open_id {
                    open_window(&shared);
                } else if ev.id == conv_id {
                    toggle_conversation(&shared);
                    refresh();
                } else if let Some(mode) = mode_for(&ev.id) {
                    send_anc(&shared, mode);
                    refresh();
                }
            }
        }
    }
}
