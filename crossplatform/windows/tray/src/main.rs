//! LibrePods system-tray app for Windows — a thin IPC client of `librepodsd`.
//!
//! The daemon owns the driver + AAP session + hi-res mic; this renders the tray
//! icon/menu/overlay from the daemon's State events and sends it commands. It
//! spawns the daemon if it isn't already running. (See ../../../docs/windows/daemon-ipc/PLAN.md.)
#![windows_subsystem = "windows"]

mod client;
mod overlay;
mod pref;
mod theme;

use std::sync::{Arc, Mutex};

use tray_icon::menu::{
    CheckMenuItem, Menu, MenuEvent, MenuId, MenuItem, PredefinedMenuItem, Submenu,
};
use tray_icon::{Icon, TrayIconBuilder};

use librepods_ipc::{Battery, Command, Snapshot};

use windows_sys::Win32::Foundation::{ERROR_ALREADY_EXISTS, GetLastError};
use windows_sys::Win32::System::Threading::CreateMutexW;
use windows_sys::Win32::UI::WindowsAndMessaging::{
    DispatchMessageW, GetMessageW, MSG, PostQuitMessage, SetTimer, TranslateMessage, WM_TIMER,
};

type Shared = Arc<Mutex<Snapshot>>;

fn make_icon() -> Icon {
    // A 32x32 teal filled circle on transparent background.
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

/// A tray icon that shows the battery percentage as a number (MagicPods-style).
fn battery_icon(percent: u8) -> Icon {
    use ab_glyph::{Font, PxScale, ScaleFont};
    use image::{ImageBuffer, Rgba};
    use imageproc::drawing::draw_text_mut;

    let (w, h) = (64u32, 64u32);
    let mut img = ImageBuffer::from_fn(w, h, |_, _| Rgba([0u8, 0, 0, 0]));
    let font = match ab_glyph::FontRef::try_from_slice(include_bytes!("../assets/DejaVuSans.ttf")) {
        Ok(f) => f,
        Err(_) => return make_icon(),
    };
    let text = format!("{percent}");
    let scale = PxScale::from(if text.len() >= 3 { 44.0 } else { 56.0 });
    let scaled = font.as_scaled(scale);
    let mut tw = 0.0f32;
    for c in text.chars() {
        tw += scaled.h_advance(font.glyph_id(c));
    }
    let x = ((w as f32 - tw) / 2.0).max(0.0) as i32;
    let y = ((h as f32 - scale.y) / 2.0).max(0.0) as i32 - 2;
    let digit = if theme::system_dark() {
        Rgba([255u8, 255, 255, 255])
    } else {
        Rgba([24u8, 24, 24, 255])
    };
    draw_text_mut(&mut img, digit, x, y, scale, &font, &text);
    Icon::from_rgba(img.into_raw(), w, h).unwrap_or_else(|_| make_icon())
}

fn avg_battery(b: &Battery) -> Option<u8> {
    match (b.left, b.right) {
        (Some(l), Some(r)) => Some(((l as u16 + r as u16) / 2) as u8),
        (Some(v), None) | (None, Some(v)) => Some(v),
        (None, None) => None,
    }
}

fn battery_text(b: &Battery, connected: bool) -> String {
    if !connected {
        return "Disconnected".to_string();
    }
    let f = |v: Option<u8>| v.map(|p| format!("{p}%")).unwrap_or_else(|| "—".into());
    format!("Left {}   Right {}   Case {}", f(b.left), f(b.right), f(b.case))
}

fn main() {
    // Single instance: never two tray icons.
    unsafe {
        let name: Vec<u16> = "Local\\LibrePodsTraySingleton\0".encode_utf16().collect();
        let _ = CreateMutexW(std::ptr::null(), 0, name.as_ptr());
        if GetLastError() == ERROR_ALREADY_EXISTS {
            return;
        }
    }

    overlay::init(); // the hidden centered popup window on this thread
    theme::apply_menu_theme();

    let state: Shared = Arc::new(Mutex::new(Snapshot::default()));

    // Connect to the daemon (spawning it if needed); it feeds `state` + overlays.
    let client = client::start(state.clone(), overlay::show);

    // --- Tray menu ---
    let title = MenuItem::new("LibrePods", false, None);
    let battery = MenuItem::new("Connecting…", false, None);
    let m_connect = MenuItem::new("Connect", true, None);
    let anc_header = MenuItem::new("Noise Control", false, None);
    let m_off = CheckMenuItem::new("Off", true, false, None);
    let m_anc = CheckMenuItem::new("Noise Cancellation", true, false, None);
    let m_trans = CheckMenuItem::new("Transparency", true, false, None);
    let m_adapt = CheckMenuItem::new("Adaptive", true, false, None);
    let vol_line = MenuItem::new("Volume: —", false, None);
    let m_vol_up = MenuItem::new("Volume  +", true, None);
    let m_vol_down = MenuItem::new("Volume  −", true, None);
    let m_mute = MenuItem::new("Mute / Unmute", true, None);
    let m_mic = MenuItem::new("Microphone: idle", false, None);
    let m_auto = CheckMenuItem::new("Auto-enable on recording", true, true, None);
    let m_mic_manual = CheckMenuItem::new("Hi-res microphone (manual)", true, false, None);
    let feat_header = MenuItem::new("Features", false, None);
    let m_conv = CheckMenuItem::new("Conversational Awareness", true, false, None);
    let m_adaptive_vol = CheckMenuItem::new("Adaptive Volume", true, false, None);
    let m_allow_off = CheckMenuItem::new("Allow \"Off\" mode", true, false, None);
    // Adaptive Audio noise strength (0x2E, value 0..=100) — only meaningful in
    // Adaptive mode. A submenu keeps the main menu tidy.
    let m_noise_low = MenuItem::new("Low", true, None);
    let m_noise_mid = MenuItem::new("Medium", true, None);
    let m_noise_high = MenuItem::new("High", true, None);
    let noise_sub = Submenu::new("Adaptive noise", true);
    noise_sub.append(&m_noise_low).unwrap();
    noise_sub.append(&m_noise_mid).unwrap();
    noise_sub.append(&m_noise_high).unwrap();
    let m_open = MenuItem::new("Open App", true, None);
    // Which front-end "Open App" launches (both are IPC clients of the daemon).
    let cur_ui = pref::get();
    let m_ui_iced = CheckMenuItem::new("iced (cross-platform)", true, cur_ui == pref::Ui::Iced, None);
    let m_ui_winui = CheckMenuItem::new("WinUI 3 (native)", true, cur_ui == pref::Ui::WinUi, None);
    let ui_sub = Submenu::new("Interface", true);
    ui_sub.append(&m_ui_iced).unwrap();
    ui_sub.append(&m_ui_winui).unwrap();
    let quit = MenuItem::new("Quit", true, None);

    let connect_id = m_connect.id().clone();
    let off_id = m_off.id().clone();
    let anc_id = m_anc.id().clone();
    let trans_id = m_trans.id().clone();
    let adapt_id = m_adapt.id().clone();
    let vol_up_id = m_vol_up.id().clone();
    let vol_down_id = m_vol_down.id().clone();
    let mute_id = m_mute.id().clone();
    let auto_id = m_auto.id().clone();
    let mic_manual_id = m_mic_manual.id().clone();
    let conv_id = m_conv.id().clone();
    let adaptive_vol_id = m_adaptive_vol.id().clone();
    let allow_off_id = m_allow_off.id().clone();
    let noise_low_id = m_noise_low.id().clone();
    let noise_mid_id = m_noise_mid.id().clone();
    let noise_high_id = m_noise_high.id().clone();
    let open_id = m_open.id().clone();
    let ui_iced_id = m_ui_iced.id().clone();
    let ui_winui_id = m_ui_winui.id().clone();
    let quit_id = quit.id().clone();

    let menu = Menu::new();
    menu.append(&title).unwrap();
    menu.append(&battery).unwrap();
    menu.append(&m_connect).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&anc_header).unwrap();
    menu.append(&m_off).unwrap();
    menu.append(&m_anc).unwrap();
    menu.append(&m_trans).unwrap();
    menu.append(&m_adapt).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&vol_line).unwrap();
    menu.append(&m_vol_up).unwrap();
    menu.append(&m_vol_down).unwrap();
    menu.append(&m_mute).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&m_mic).unwrap();
    menu.append(&m_auto).unwrap();
    menu.append(&m_mic_manual).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&feat_header).unwrap();
    menu.append(&m_conv).unwrap();
    menu.append(&m_adaptive_vol).unwrap();
    menu.append(&m_allow_off).unwrap();
    menu.append(&noise_sub).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&m_open).unwrap();
    menu.append(&ui_sub).unwrap();
    menu.append(&quit).unwrap();

    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip("LibrePods")
        .with_icon(make_icon())
        .build()
        .expect("tray");

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
        // Clone + release the state lock immediately so we never hold it during
        // the (COM) volume calls or the icon render (which would let a busy IPC
        // reader thread stall the UI thread → a frozen tray menu).
        let s = state.lock().unwrap().clone();
        title.set_text(if s.dev_name.is_empty() { "LibrePods" } else { &s.dev_name });
        battery.set_text(&battery_text(&s.battery, s.connected));
        m_connect.set_enabled(!s.connected); // only offer Connect when disconnected
        m_off.set_checked(s.anc == 1);
        m_anc.set_checked(s.anc == 2);
        m_trans.set_checked(s.anc == 3);
        m_adapt.set_checked(s.anc == 4);
        let vol = if s.muted {
            "muted".to_string()
        } else {
            format!("{}%", s.volume)
        };
        vol_line.set_text(&format!("Volume: {vol}"));
        m_mic.set_text(if s.mic_recording {
            "Microphone: recording"
        } else {
            "Microphone: idle"
        });
        m_auto.set_checked(s.auto_mode);
        m_mic_manual.set_checked(s.mic_recording && !s.auto_mode);
        m_conv.set_checked(s.conversational_awareness);
        m_adaptive_vol.set_checked(s.adaptive_volume);
        m_allow_off.set_checked(s.allow_off);
        let icon = match (s.connected, avg_battery(&s.battery)) {
            (true, Some(avg)) => battery_icon(avg),
            _ => make_icon(),
        };
        let _ = tray.set_icon(Some(icon));
        theme::apply_menu_theme();
    };

    unsafe {
        SetTimer(std::ptr::null_mut(), 1, 2000, None);
        let mut msg: MSG = std::mem::zeroed();
        loop {
            let r = GetMessageW(&mut msg, std::ptr::null_mut(), 0, 0);
            if r <= 0 {
                break;
            }
            if msg.message == WM_TIMER && msg.hwnd.is_null() {
                refresh();
            } else {
                TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
            while let Ok(ev) = MenuEvent::receiver().try_recv() {
                if ev.id == quit_id {
                    client.send(&Command::Shutdown); // stop the daemon too
                    PostQuitMessage(0);
                } else if ev.id == connect_id {
                    client.send(&Command::Connect);
                    overlay::show("LibrePods", "Connecting…");
                } else if ev.id == open_id {
                    // Launch the user's preferred front-end (iced or WinUI 3) —
                    // both are IPC clients of the daemon, so they coexist with us.
                    pref::launch();
                } else if ev.id == ui_iced_id {
                    pref::set(pref::Ui::Iced);
                    m_ui_iced.set_checked(true);
                    m_ui_winui.set_checked(false);
                } else if ev.id == ui_winui_id {
                    pref::set(pref::Ui::WinUi);
                    m_ui_winui.set_checked(true);
                    m_ui_iced.set_checked(false);
                } else if ev.id == vol_up_id {
                    client.send(&Command::StepVolume { delta: 5 });
                } else if ev.id == vol_down_id {
                    client.send(&Command::StepVolume { delta: -5 });
                } else if ev.id == mute_id {
                    client.send(&Command::ToggleMute);
                } else if ev.id == auto_id {
                    let (auto, mic) = {
                        let s = state.lock().unwrap();
                        (s.auto_mode, s.mic_recording)
                    };
                    let new_auto = !auto;
                    client.send(&Command::SetMicMode { auto: new_auto, manual: mic });
                    overlay::show(
                        "LibrePods",
                        if new_auto { "Microphone: auto mode" } else { "Microphone: manual mode" },
                    );
                } else if ev.id == mic_manual_id {
                    let mic = state.lock().unwrap().mic_recording;
                    client.send(&Command::SetMicMode { auto: false, manual: !mic });
                } else if ev.id == conv_id {
                    let on = !state.lock().unwrap().conversational_awareness;
                    client.send(&Command::SetFeature {
                        feature: librepods_ipc::feature::CONVERSATIONAL_AWARENESS,
                        on,
                    });
                } else if ev.id == adaptive_vol_id {
                    let on = !state.lock().unwrap().adaptive_volume;
                    client.send(&Command::SetFeature {
                        feature: librepods_ipc::feature::ADAPTIVE_VOLUME,
                        on,
                    });
                } else if ev.id == allow_off_id {
                    let on = !state.lock().unwrap().allow_off;
                    client.send(&Command::SetFeature {
                        feature: librepods_ipc::feature::ALLOW_OFF,
                        on,
                    });
                } else if ev.id == noise_low_id {
                    client.send(&Command::SetControl { id: 0x2E, value: 25 });
                    overlay::show("LibrePods", "Adaptive noise: Low");
                } else if ev.id == noise_mid_id {
                    client.send(&Command::SetControl { id: 0x2E, value: 50 });
                    overlay::show("LibrePods", "Adaptive noise: Medium");
                } else if ev.id == noise_high_id {
                    client.send(&Command::SetControl { id: 0x2E, value: 75 });
                    overlay::show("LibrePods", "Adaptive noise: High");
                } else if let Some(mode) = mode_for(&ev.id) {
                    client.send(&Command::SetAnc { mode });
                }
            }
            // A click on the "connect?" prompt card accepts it.
            if overlay::take_connect_clicked() {
                client.send(&Command::Connect);
                overlay::show("LibrePods", "Connecting…");
            }
        }
    }
}
