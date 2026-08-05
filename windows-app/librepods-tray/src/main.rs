//! LibrePods tray app for Windows.
//!
//! Runs in the system tray: connects to the AirPods AAP channel through the
//! LibrePodsAAP driver, shows battery (L/R/Case) and the current noise mode,
//! and lets you switch Off / Noise Cancellation / Transparency / Adaptive.
//!
//! Build (from WSL):  cargo build --release --target x86_64-pc-windows-gnu
//! Needs the LibrePodsAAP driver installed (Test Mode).

#![windows_subsystem = "windows"] // no console window

mod aap;
mod bt;
mod driver;

use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use driver::Driver;
use tray_icon::menu::{CheckMenuItem, Menu, MenuEvent, MenuId, MenuItem, PredefinedMenuItem};
use tray_icon::{Icon, TrayIconBuilder};

use windows_sys::Win32::UI::WindowsAndMessaging::{
    DispatchMessageW, GetMessageW, MSG, PostQuitMessage, SetTimer, TranslateMessage, WM_TIMER,
};

#[derive(Default)]
struct State {
    connected: bool,
    battery: aap::Battery,
    anc: u8, // 0 = unknown, 1..=4
}

type Shared = Arc<Mutex<State>>;

fn make_icon() -> Icon {
    // A 32x32 teal filled circle on transparent background.
    let (w, h) = (32u32, 32u32);
    let mut rgba = vec![0u8; (w * h * 4) as usize];
    let cx = 15.5f32;
    let cy = 15.5f32;
    let r = 15.0f32;
    for y in 0..h {
        for x in 0..w {
            let dx = x as f32 - cx;
            let dy = y as f32 - cy;
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

fn battery_text(b: &aap::Battery, connected: bool) -> String {
    if !connected {
        return "Disconnected".to_string();
    }
    let f = |v: Option<u8>| v.map(|p| format!("{p}%")).unwrap_or_else(|| "—".into());
    format!(
        "Left {}   Right {}   Case {}",
        f(b.left),
        f(b.right),
        f(b.case)
    )
}

/// Background loop: keep the AAP session alive and parse pushed packets.
/// `connected` stays true while the link is up; it only flips false when a send
/// actually fails (a real disconnect), so the UI never flickers on idle reads.
fn run_receiver(driver: Driver, mac: u64, state: Shared) {
    let mut buf = [0u8; 1024];
    loop {
        if !driver.connect(mac, aap::PSM_AACP).unwrap_or(false) {
            state.lock().unwrap().connected = false;
            thread::sleep(Duration::from_secs(3));
            continue;
        }
        let _ = driver.send(&aap::HANDSHAKE);
        thread::sleep(Duration::from_millis(300));
        let _ = driver.send(&aap::SET_FEATURES);
        thread::sleep(Duration::from_millis(300));
        let _ = driver.send(&aap::REQUEST_NOTIFS);
        state.lock().unwrap().connected = true;

        let mut ticks = 0u32;
        loop {
            if let Ok(n) = driver.recv(1000, &mut buf) {
                if n > 0 {
                    let data = &buf[..n];
                    if let Some(b) = aap::parse_battery(data) {
                        let mut s = state.lock().unwrap();
                        // merge — a packet may carry only some components
                        if b.left.is_some() {
                            s.battery.left = b.left;
                        }
                        if b.right.is_some() {
                            s.battery.right = b.right;
                        }
                        if b.case.is_some() {
                            s.battery.case = b.case;
                        }
                        if b.headphone.is_some() {
                            s.battery.headphone = b.headphone;
                        }
                    }
                    if let Some(m) = aap::parse_anc_mode(data) {
                        state.lock().unwrap().anc = m;
                    }
                }
            }
            ticks += 1;
            if ticks >= 8 {
                // ~every 8s: nudge a fresh battery push and check liveness.
                ticks = 0;
                if driver.send(&aap::REQUEST_NOTIFS).is_err() {
                    state.lock().unwrap().connected = false;
                    break; // reconnect
                }
            }
        }
        thread::sleep(Duration::from_secs(2));
    }
}

fn main() {
    let state: Shared = Arc::new(Mutex::new(State::default()));

    let (mac, dev_name) = match bt::find_airpods() {
        Some((m, n)) => (Some(m), n),
        None => (None, "AirPods".to_string()),
    };
    let driver = Driver::open().ok();

    // Start the background AAP session if we have both a device and the driver.
    if let (Some(mac), Some(drv)) = (mac, driver.clone()) {
        let st = state.clone();
        thread::spawn(move || run_receiver(drv, mac, st));
    }

    // --- Tray menu ---
    let title = MenuItem::new(&dev_name, false, None);
    let battery = MenuItem::new("Connecting…", false, None);
    let anc_header = MenuItem::new("Noise Control", false, None);
    let m_off = CheckMenuItem::new("Off", true, false, None);
    let m_anc = CheckMenuItem::new("Noise Cancellation", true, false, None);
    let m_trans = CheckMenuItem::new("Transparency", true, false, None);
    let m_adapt = CheckMenuItem::new("Adaptive", true, false, None);
    let quit = MenuItem::new("Quit", true, None);

    let off_id = m_off.id().clone();
    let anc_id = m_anc.id().clone();
    let trans_id = m_trans.id().clone();
    let adapt_id = m_adapt.id().clone();
    let quit_id = quit.id().clone();

    let menu = Menu::new();
    menu.append(&title).unwrap();
    menu.append(&battery).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&anc_header).unwrap();
    menu.append(&m_off).unwrap();
    menu.append(&m_anc).unwrap();
    menu.append(&m_trans).unwrap();
    menu.append(&m_adapt).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&quit).unwrap();

    let tray = TrayIconBuilder::new()
        .with_menu(Box::new(menu))
        .with_tooltip("LibrePods")
        .with_icon(make_icon())
        .build()
        .expect("tray");

    // Map a clicked menu id to an ANC mode.
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
        let s = state.lock().unwrap();
        let bt = battery_text(&s.battery, s.connected);
        battery.set_text(&bt);
        m_off.set_checked(s.anc == 1);
        m_anc.set_checked(s.anc == 2);
        m_trans.set_checked(s.anc == 3);
        m_adapt.set_checked(s.anc == 4);
        let _ = tray.set_tooltip(Some(format!("{dev_name} · {bt} · ANC: {}", aap::anc_name(s.anc))));
    };

    unsafe {
        SetTimer(std::ptr::null_mut(), 1, 2000, None);
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
                } else if let Some(mode) = mode_for(&ev.id) {
                    if let Some(drv) = driver.clone() {
                        let _ = drv.send(&aap::anc_command(mode));
                    }
                    state.lock().unwrap().anc = mode;
                    refresh();
                }
            }
        }
    }
}
