//! LibrePods tray app for Windows.
//!
//! Runs in the system tray: connects to the AirPods AAP channel through the
//! LibrePodsAAP driver, shows battery (L/R/Case) and the current noise mode,
//! and lets you switch Off / Noise Cancellation / Transparency / Adaptive.
//!
//! Build (from WSL):  cargo build --release --target x86_64-pc-windows-gnu
//! Needs the LibrePodsAAP driver installed (Test Mode).

#![windows_subsystem = "windows"] // no console window

mod a2dp;
mod aap;
mod bt;
mod driver;
mod eld;
mod media;
mod micpipe;
mod overlay;
mod theme;
mod volume;

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use driver::Driver;
use tray_icon::menu::{CheckMenuItem, Menu, MenuEvent, MenuId, MenuItem, PredefinedMenuItem};
use tray_icon::{Icon, TrayIconBuilder};

use windows_sys::Win32::Foundation::{ERROR_ALREADY_EXISTS, GetLastError};
use windows_sys::Win32::System::Threading::CreateMutexW;
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

/// Average earbud battery (L+R)/2, or the single known one.
fn avg_battery(b: &aap::Battery) -> Option<u8> {
    match (b.left, b.right) {
        (Some(l), Some(r)) => Some(((l as u16 + r as u16) / 2) as u8),
        (Some(v), None) | (None, Some(v)) => Some(v),
        (None, None) => None,
    }
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
    // Shrink for 3 digits ("100") so it still fits.
    let scale = PxScale::from(if text.len() >= 3 { 44.0 } else { 56.0 });

    // Center horizontally using the glyph advances.
    let scaled = font.as_scaled(scale);
    let mut tw = 0.0f32;
    for c in text.chars() {
        tw += scaled.h_advance(font.glyph_id(c));
    }
    let x = ((w as f32 - tw) / 2.0).max(0.0) as i32;
    let y = ((h as f32 - scale.y) / 2.0).max(0.0) as i32 - 2;

    // Contrast with the taskbar: white digits on a dark taskbar, near-black on a
    // light one (otherwise white digits vanish on a light taskbar).
    let digit = if theme::system_dark() {
        Rgba([255u8, 255, 255, 255])
    } else {
        Rgba([24u8, 24, 24, 255])
    };
    draw_text_mut(&mut img, digit, x, y, scale, &font, &text);
    Icon::from_rgba(img.into_raw(), w, h).unwrap_or_else(|_| make_icon())
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
fn run_receiver(
    mac: u64,
    state: Shared,
    dev_name: String,
    driver_cell: Arc<Mutex<Option<Driver>>>,
    mic_on: Arc<AtomicBool>,
    pipe: Option<Arc<micpipe::MicPipe>>,
) {
    let mut buf = [0u8; 8192];
    let mut audio_pkts = 0u32;
    // Hi-res mic decoder (Phase 3b): created on the first audio packet, torn down
    // when the mic is disabled. It feeds the shared mic pipe.
    let mut decoder: Option<eld::Decoder> = None;
    media::init(); // COM (MTA) for the SMTC ear-detection auto-pause on this thread
    let mut last_anc = 0u8;
    let mut last_case_present: Option<bool> = None;
    // Set on every (re)connect; the first battery packet then shows a card. A
    // lid-open wakes the buds and reconnects, so this is our reliable
    // "case opened" popup (with battery), not the case-present transition.
    let mut pending_card = false;
    loop {
        // (Re)open the driver each session. The driver's device object is
        // recreated when the AirPods reconnect (it now unloads cleanly on
        // disconnect), so a handle from a previous session points at a removed
        // device and every call on it fails — a fresh open binds the new one.
        let driver = match Driver::open() {
            Ok(d) => d,
            Err(_) => {
                state.lock().unwrap().connected = false;
                *driver_cell.lock().unwrap() = None;
                thread::sleep(Duration::from_secs(3));
                continue;
            }
        };
        *driver_cell.lock().unwrap() = Some(driver.clone()); // publish for the menu
        if !driver.connect(mac, aap::PSM_AACP).unwrap_or(false) {
            state.lock().unwrap().connected = false;
            thread::sleep(Duration::from_secs(3));
            continue;
        }
        let _ = driver.send(&aap::HANDSHAKE);
        thread::sleep(Duration::from_millis(300));
        let _ = driver.send(&aap::SET_FEATURES);
        thread::sleep(Duration::from_millis(300));
        // One-time notifications enable — this is what makes the AirPods push
        // ear-detection (and battery/ANC) events. It is sent ONCE at handshake;
        // repeating it periodically is what used to re-negotiate the audio
        // profile and cut the sound, so we never poll it.
        let _ = driver.send(&aap::REQUEST_NOTIFS);
        state.lock().unwrap().connected = true;
        pending_card = true;

        // Ear-detection auto-pause state: we only resume media that WE paused,
        // so we never fight a user who paused it themselves.
        let mut we_paused = false;
        let mut ticks = 0u32;
        loop {
            let mut got_data = false;
            if let Ok(n) = driver.recv(2000, &mut buf) {
                if n > 0 {
                    got_data = true;
                    let data = &buf[..n];
                    // Hi-res mic (Phase 3 de-risk): while enabled, count the 0x58
                    // uplink audio packets and confirm the stream actually started.
                    if mic_on.load(Ordering::Relaxed) {
                        if aap::is_audio_packet(data) {
                            // Bring up the decoder on the first audio packet and
                            // prime the shared mic pipe's ring with a small cushion.
                            if decoder.is_none() {
                                decoder = eld::Decoder::new();
                                if let Some(pp) = pipe.as_ref() {
                                    // We feed ~30 ms per-packet bursts while the
                                    // capture drains steadily, so an 80 ms head
                                    // start avoids underruns without much latency.
                                    pp.write(&[0i16; 3840]);
                                }
                            }
                            if let (Some(dec), Some(pp)) = (decoder.as_mut(), pipe.as_ref()) {
                                // Decode all the packet's AUs, then write once.
                                let mut out: Vec<i16> = Vec::new();
                                aap::for_each_au(data, |au| {
                                    out.extend_from_slice(dec.decode(au));
                                });
                                if !out.is_empty() {
                                    pp.write(&out);
                                }
                            }
                            audio_pkts = audio_pkts.saturating_add(1);
                        }
                    } else if decoder.is_some() {
                        // Mic disabled: drop the decoder.
                        decoder = None;
                        audio_pkts = 0;
                    }
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
                        // Case reports its battery only while it's "in the loop"
                        // (buds docked / lid open) — use that transition as a
                        // case opened/closed popup trigger.
                        let present = s.battery.case.is_some();
                        let batt = battery_text(&s.battery, s.connected);
                        drop(s);
                        if pending_card {
                            // First battery after a (re)connect = we just came
                            // back / the case was opened. Show a battery card.
                            overlay::show(&dev_name, &batt);
                            pending_card = false;
                        } else if last_case_present.is_some_and(|prev| prev != present) {
                            // Buds docked/undocked while the session stayed up.
                            let ev = if present { "Case opened" } else { "Case closed" };
                            overlay::show(&dev_name, &format!("{ev}  ·  {batt}"));
                        }
                        last_case_present = Some(present);
                    }
                    if let Some(m) = aap::parse_anc_mode(data) {
                        state.lock().unwrap().anc = m;
                        // Pop up on an actual mode change (not the first report).
                        if last_anc != 0 && m != last_anc {
                            overlay::show(&dev_name, aap::anc_name(m));
                        }
                        last_anc = m;
                    }
                    if let Some((primary, secondary)) = aap::parse_ear_detection(data) {
                        let wearing = primary.in_ear() || secondary.in_ear();
                        if !wearing {
                            // Both buds out of the ears (or in the case): pause,
                            // but only if something was actually playing.
                            if media::is_playing() {
                                media::pause();
                                we_paused = true;
                            }
                        } else if we_paused {
                            // A bud went back in: resume what we paused.
                            media::play();
                            we_paused = false;
                        }
                    }
                }
            }
            // No data this cycle: yield the CPU. The driver's ACL read completes
            // immediately with 0 bytes when idle (ACL_SHORT_TRANSFER_OK), so
            // without this the loop spins a core AND floods the Bluetooth stack
            // with back-to-back ACL reads that contend with the A2DP audio
            // (the crackle). Pushed events still arrive within ~150 ms.
            if !got_data {
                thread::sleep(Duration::from_millis(150));
            }
            // Passive session: no periodic L2CAP sends — those make Windows
            // re-negotiate the audio profile and cut/switch the output. Detect a
            // real disconnect via GET_STATUS, which reads a driver variable only
            // (no L2CAP I/O, so it never disturbs the audio).
            ticks += 1;
            if ticks >= 5 {
                ticks = 0;
                if !driver.status().map(|s| s == 2).unwrap_or(false) {
                    state.lock().unwrap().connected = false;
                    *driver_cell.lock().unwrap() = None; // drop the stale handle
                    // Reset per-session state so the next connect (e.g. lid
                    // reopened) re-fires the battery card + ANC/case events.
                    last_anc = 0;
                    last_case_present = None;
                    break; // reconnect
                }
            }
        }
        thread::sleep(Duration::from_secs(2));
    }
}

fn main() {
    // Single instance: if a LibrePods tray is already running, exit quietly so we
    // never end up with two icons fighting over the single-open driver / mic pipe.
    unsafe {
        let name: Vec<u16> = "Local\\LibrePodsTraySingleton\0".encode_utf16().collect();
        let _ = CreateMutexW(std::ptr::null(), 0, name.as_ptr());
        if GetLastError() == ERROR_ALREADY_EXISTS {
            return;
        }
    }

    volume::init(); // COM for Core Audio, on this (main) thread
    overlay::init(); // create the (hidden) centered popup window on this thread
    theme::apply_menu_theme(); // dark/light Win32 context menu, per the OS theme

    let state: Shared = Arc::new(Mutex::new(State::default()));

    let (mac, dev_name) = match bt::find_airpods() {
        Some((m, n)) => (Some(m), n),
        None => (None, "AirPods".to_string()),
    };
    // The current driver handle, shared with the tray menu. run_receiver
    // re-opens it every session (the device object is recreated on reconnect,
    // so a stale handle fails) and publishes the live one here.
    let driver_cell: Arc<Mutex<Option<Driver>>> = Arc::new(Mutex::new(None));

    // Whether the hi-res mic stream is enabled. Driven automatically by the
    // capture-activity poll below (on when an app records from the virtual mic),
    // read by the receive loop.
    let mic_on = Arc::new(AtomicBool::new(false));

    // Auto mode (default): the poll auto-enables/disables the mic on recording.
    // Users who prefer can turn it off and drive the mic manually.
    let auto_mode = Arc::new(AtomicBool::new(true));

    // The single (exclusive) handle to the virtual-mic control device, shared by
    // the receive thread (writes decoded audio) and the poll thread (reads the
    // capture-activity counter).
    let pipe: Option<Arc<micpipe::MicPipe>> = micpipe::MicPipe::open().map(Arc::new);

    // Start the background AAP session if the AirPods are paired. run_receiver
    // opens the driver itself and keeps retrying, so this works even if they
    // aren't connected yet at startup.
    if let Some(mac) = mac {
        let st = state.clone();
        let name = dev_name.clone();
        let cell = driver_cell.clone();
        let mic = mic_on.clone();
        let rx_pipe = pipe.clone();
        thread::spawn(move || run_receiver(mac, st, name, cell, mic, rx_pipe));

        // Auto-activate: the driver's capture counter advances while an app is
        // recording from the virtual mic. When it starts, enable the hi-res
        // stream; when it stops (debounced ~1.5 s), disable it and restore A2DP
        // stereo — so the mic "just works" when you join a call.
        let poll_pipe = pipe.clone();
        let poll_mic = mic_on.clone();
        let poll_cell = driver_cell.clone();
        let poll_name = dev_name.clone();
        let poll_auto = auto_mode.clone();
        thread::spawn(move || {
            let mut prev = poll_pipe.as_ref().map(|p| p.status()).unwrap_or(0);
            let mut idle = 0u32;
            let mut on = false;
            loop {
                thread::sleep(Duration::from_millis(500));
                let cur = match poll_pipe.as_ref() {
                    Some(p) => p.status(),
                    None => break,
                };
                let capturing = cur != prev;
                prev = cur;
                // Manual mode: don't auto-manage (but keep `prev` fresh above so
                // re-enabling auto doesn't fire a spurious transition).
                if !poll_auto.load(Ordering::Relaxed) {
                    on = poll_mic.load(Ordering::Relaxed);
                    continue;
                }
                if capturing {
                    idle = 0;
                    if !on {
                        on = true;
                        poll_mic.store(true, Ordering::Relaxed);
                        if let Some(drv) = poll_cell.lock().unwrap().clone() {
                            let _ = drv.send(&aap::START_AUDIO);
                        }
                        overlay::show(&poll_name, "Microphone in use — hi-res on");
                    }
                } else {
                    idle += 1;
                    if on && idle >= 3 {
                        on = false;
                        poll_mic.store(false, Ordering::Relaxed);
                        if let Some(drv) = poll_cell.lock().unwrap().clone() {
                            let _ = drv.send(&aap::STOP_AUDIO);
                        }
                        overlay::show(&poll_name, "Microphone released — restoring stereo…");
                        a2dp::reset(mac);
                        overlay::show(&poll_name, "Stereo restored");
                    }
                }
            }
        });
    }

    // --- Tray menu ---
    let title = MenuItem::new(&dev_name, false, None);
    let battery = MenuItem::new("Connecting…", false, None);
    let anc_header = MenuItem::new("Noise Control", false, None);
    let m_off = CheckMenuItem::new("Off", true, false, None);
    let m_anc = CheckMenuItem::new("Noise Cancellation", true, false, None);
    let m_trans = CheckMenuItem::new("Transparency", true, false, None);
    let m_adapt = CheckMenuItem::new("Adaptive", true, false, None);
    let vol_line = MenuItem::new("Volume: —", false, None);
    let m_vol_up = MenuItem::new("Volume  +", true, None);
    let m_vol_down = MenuItem::new("Volume  −", true, None);
    let m_mute = MenuItem::new("Mute / Unmute", true, None);
    // Hi-res mic: a status line + an "auto" toggle + a manual override.
    let m_mic = MenuItem::new("Microphone: idle", false, None);
    let m_auto = CheckMenuItem::new("Auto-enable on recording", true, true, None);
    let m_mic_manual = CheckMenuItem::new("Hi-res microphone (manual)", true, false, None);
    let m_open = MenuItem::new("Open App", true, None);
    let quit = MenuItem::new("Quit", true, None);

    let off_id = m_off.id().clone();
    let anc_id = m_anc.id().clone();
    let trans_id = m_trans.id().clone();
    let adapt_id = m_adapt.id().clone();
    let vol_up_id = m_vol_up.id().clone();
    let vol_down_id = m_vol_down.id().clone();
    let mute_id = m_mute.id().clone();
    let auto_id = m_auto.id().clone();
    let mic_manual_id = m_mic_manual.id().clone();
    let open_id = m_open.id().clone();
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
    menu.append(&vol_line).unwrap();
    menu.append(&m_vol_up).unwrap();
    menu.append(&m_vol_down).unwrap();
    menu.append(&m_mute).unwrap();
    menu.append(&PredefinedMenuItem::separator()).unwrap();
    menu.append(&m_mic).unwrap();
    menu.append(&m_auto).unwrap();
    menu.append(&m_mic_manual).unwrap();
    menu.append(&m_open).unwrap();
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
        let vol = if volume::is_muted() {
            "muted".to_string()
        } else {
            volume::get()
                .map(|p| format!("{p}%"))
                .unwrap_or_else(|| "—".into())
        };
        vol_line.set_text(format!("Volume: {vol}"));
        m_mic.set_text(if mic_on.load(Ordering::Relaxed) {
            "Microphone: recording (hi-res)"
        } else {
            "Microphone: idle"
        });
        m_auto.set_checked(auto_mode.load(Ordering::Relaxed));
        m_mic_manual.set_checked(mic_on.load(Ordering::Relaxed));
        let _ = tray.set_tooltip(Some(format!(
            "{dev_name} · {bt} · ANC: {} · Vol: {vol}",
            aap::anc_name(s.anc)
        )));
        // Tray icon: the average earbud battery % (falls back to the badge when
        // disconnected / unknown).
        let icon = match (s.connected, avg_battery(&s.battery)) {
            (true, Some(avg)) => battery_icon(avg),
            _ => make_icon(),
        };
        let _ = tray.set_icon(Some(icon));
        theme::apply_menu_theme(); // keep the menu theme in sync with the OS
    };

    unsafe {
        SetTimer(std::ptr::null_mut(), 1, 2000, None);
        let mut msg: MSG = std::mem::zeroed();
        loop {
            let r = GetMessageW(&mut msg, std::ptr::null_mut(), 0, 0);
            if r <= 0 {
                break; // WM_QUIT or error
            }
            // Our tray refresh is a thread timer (hwnd == null); the overlay's
            // auto-hide is a window timer (hwnd == overlay). Only the former
            // triggers refresh; everything else (incl. the overlay's WM_TIMER,
            // WM_PAINT, show request) must be dispatched to its window proc.
            if msg.message == WM_TIMER && msg.hwnd.is_null() {
                refresh();
            } else {
                TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
            while let Ok(ev) = MenuEvent::receiver().try_recv() {
                if ev.id == quit_id {
                    PostQuitMessage(0);
                } else if ev.id == open_id {
                    // Launch the full app (sibling librepods.exe) and quit the
                    // tray so the app can take the single-handle driver.
                    if let Ok(exe) = std::env::current_exe() {
                        if let Some(dir) = exe.parent() {
                            let _ =
                                std::process::Command::new(dir.join("librepods.exe")).spawn();
                        }
                    }
                    PostQuitMessage(0);
                } else if ev.id == vol_up_id {
                    volume::step(5);
                    refresh();
                } else if ev.id == vol_down_id {
                    volume::step(-5);
                    refresh();
                } else if ev.id == mute_id {
                    volume::toggle_mute();
                    refresh();
                } else if ev.id == auto_id {
                    let on = !auto_mode.load(Ordering::Relaxed);
                    auto_mode.store(on, Ordering::Relaxed);
                    m_auto.set_checked(on);
                    overlay::show(
                        &dev_name,
                        if on { "Microphone: auto mode" } else { "Microphone: manual mode" },
                    );
                } else if ev.id == mic_manual_id {
                    // Manual override: turn the hi-res mic on/off directly.
                    let on = !mic_on.load(Ordering::Relaxed);
                    m_mic_manual.set_checked(on);
                    if on {
                        mic_on.store(true, Ordering::Relaxed);
                        if let Some(drv) = driver_cell.lock().unwrap().clone() {
                            let _ = drv.send(&aap::START_AUDIO);
                        }
                        overlay::show(&dev_name, "Hi-res mic on");
                    } else {
                        // Same 1.5 s debounce as auto so the tail isn't clipped,
                        // then stop + restore stereo, keeping the card up through
                        // the whole reconnect.
                        let mic = mic_on.clone();
                        let cell = driver_cell.clone();
                        let name = dev_name.clone();
                        thread::spawn(move || {
                            thread::sleep(Duration::from_millis(3500));
                            mic.store(false, Ordering::Relaxed);
                            if let Some(drv) = cell.lock().unwrap().clone() {
                                let _ = drv.send(&aap::STOP_AUDIO);
                            }
                            overlay::show(&name, "Restoring stereo…");
                            if let Some(m) = mac {
                                a2dp::reset(m);
                            }
                            overlay::show(&name, "Stereo restored");
                        });
                        overlay::show(&dev_name, "Hi-res mic off");
                    }
                    refresh();
                } else if let Some(mode) = mode_for(&ev.id) {
                    if let Some(drv) = driver_cell.lock().unwrap().clone() {
                        let _ = drv.send(&aap::anc_command(mode));
                    }
                    state.lock().unwrap().anc = mode;
                    // Show the card immediately on the click — don't wait for the
                    // AirPods to echo the mode back (they may not).
                    overlay::show(&dev_name, aap::anc_name(mode));
                    refresh();
                }
            }
        }
    }
}
