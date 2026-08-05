//! LibrePods egui window for Windows: battery, ANC control and a volume slider.
//! Talks to the LibrePodsAAP driver (needs it installed, Test Mode).

#![windows_subsystem = "windows"]

mod aap;
mod bt;
mod driver;
mod volume;

use std::sync::{Arc, Mutex};
use std::thread;
use std::time::Duration;

use driver::Driver;
use eframe::egui;

#[derive(Default)]
struct State {
    connected: bool,
    battery: aap::Battery,
    anc: u8,
}
type Shared = Arc<Mutex<State>>;

/// Passive AAP session (handshake once, then only listen — same approach as the
/// tray app so the audio stays clean). Liveness via GET_STATUS (no L2CAP I/O).
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
            if let Ok(n) = driver.recv(2000, &mut buf) {
                if n > 0 {
                    let data = &buf[..n];
                    if let Some(b) = aap::parse_battery(data) {
                        let mut s = state.lock().unwrap();
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
            if ticks >= 5 {
                ticks = 0;
                if !driver.status().map(|s| s == 2).unwrap_or(false) {
                    state.lock().unwrap().connected = false;
                    break;
                }
            }
        }
        thread::sleep(Duration::from_secs(2));
    }
}

struct App {
    state: Shared,
    driver: Option<Driver>,
    dev_name: String,
    vol: f32,
}

fn battery_row(ui: &mut egui::Ui, label: &str, level: Option<u8>) {
    ui.horizontal(|ui| {
        ui.add_sized([54.0, 18.0], egui::Label::new(label));
        match level {
            Some(p) => {
                ui.add(
                    egui::ProgressBar::new(p as f32 / 100.0)
                        .text(format!("{p}%"))
                        .desired_width(210.0),
                );
            }
            None => {
                ui.label("—");
            }
        }
    });
}

impl eframe::App for App {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        ctx.request_repaint_after(Duration::from_millis(500));

        // Read live volume unless the user is dragging the slider.
        if !ctx.memory(|m| m.is_anything_being_dragged()) {
            if let Some(v) = volume::get() {
                self.vol = v as f32;
            }
        }

        let (connected, battery, anc) = {
            let s = self.state.lock().unwrap();
            (s.connected, s.battery, s.anc)
        };

        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_space(4.0);
            ui.heading(&self.dev_name);
            ui.colored_label(
                if connected {
                    egui::Color32::from_rgb(0x2e, 0xcc, 0x71)
                } else {
                    egui::Color32::GRAY
                },
                if connected { "● Connected" } else { "○ Connecting…" },
            );
            ui.add_space(8.0);

            battery_row(ui, "Left", battery.left);
            battery_row(ui, "Right", battery.right);
            battery_row(ui, "Case", battery.case);

            ui.add_space(10.0);
            ui.separator();
            ui.add_space(6.0);

            ui.label(egui::RichText::new("NOISE CONTROL").weak().small());
            ui.horizontal(|ui| {
                for (mode, name) in [(1u8, "Off"), (2, "ANC"), (3, "Transp."), (4, "Adaptive")] {
                    if ui.selectable_label(anc == mode, name).clicked() {
                        if let Some(d) = &self.driver {
                            let _ = d.send(&aap::anc_command(mode));
                        }
                        self.state.lock().unwrap().anc = mode;
                    }
                }
            });

            ui.add_space(12.0);
            ui.label(egui::RichText::new("VOLUME").weak().small());
            if ui
                .add(
                    egui::Slider::new(&mut self.vol, 0.0..=100.0)
                        .suffix(" %")
                        .trailing_fill(true),
                )
                .changed()
            {
                volume::set(self.vol.round() as u8);
            }
        });
    }
}

fn main() -> eframe::Result<()> {
    volume::init();

    let state: Shared = Arc::new(Mutex::new(State::default()));
    let (mac, dev_name) = match bt::find_airpods() {
        Some((m, n)) => (Some(m), n),
        None => (None, "AirPods".to_string()),
    };
    let driver = Driver::open().ok();
    if let (Some(mac), Some(drv)) = (mac, driver.clone()) {
        let st = state.clone();
        thread::spawn(move || run_receiver(drv, mac, st));
    }

    let vol = volume::get().unwrap_or(50) as f32;
    let app = App {
        state,
        driver,
        dev_name,
        vol,
    };

    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([340.0, 360.0])
            .with_resizable(false),
        ..Default::default()
    };
    eframe::run_native("LibrePods", options, Box::new(|_cc| Ok(Box::new(app))))
}
