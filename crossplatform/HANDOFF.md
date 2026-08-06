# LibrePods — cross-platform (Windows) handoff

Branch: `cross-platform` (pushed to `github.com/arctumn/librepods`). Everything
below is committed + green.

## ✅ Resume — what's done & working

**Windows AirPods control (proven on real AirPods Pro 3):**
- **`LibrePodsAAP` kernel driver** (`crossplatform/windows-driver/LibrePodsAAP/`)
  — KMDF profile driver bound to the AAP SDP service
  `BTHENUM\{74ec2172-0bad-4d01-8f77-997b2be0722a}`; opens the L2CAP AAP channel
  (PSM 0x1001) in kernel mode and bridges to user space via IOCTLs. Reads
  battery, controls ANC. Universal across AirPods models. Prebuilt package in
  `prebuilt/` so it installs **without C++/WDK** (just `install.ps1`, Test Mode).
- **`librepods-tray`** (`crossplatform/windows-app/librepods-tray/`) — native
  tray menu: battery (L/R/Case) + ANC + system volume.
- **`librepods-ui`** (`crossplatform/windows-app/librepods-ui/`) — egui window:
  battery bars, ANC segmented buttons, **volume slider**.
- **KEY finding:** holding the AAP channel via a **passive session** (handshake
  once, then only listen; liveness via GET_STATUS, no periodic L2CAP sends)
  makes the audio **crystal-clear** (kills the HFP/A2DP "static") WITHOUT
  cutting/switching the output. A periodic REQUEST_NOTIFS poll was the culprit.

**App port to Windows (Linux stays green each step):**
- Phases **A** (AppPaths) · **B** (DeviceId) · **C** (L2capTransport) ·
  **D** (discovery + connection watcher + adapter power-on) ✅.
- `main.rs` now has **zero direct `bluer`/`dbus`** — all Bluetooth goes through
  `platform::`. Linux backends in `platform/linux/`, Windows backends TODO.

**Repo:** everything under `crossplatform/` (crossplatform-rust = the Rust app,
was linux-rust; + windows-driver + windows-app). CI = ci-crossplatform-rust.yml.

## 📋 TODO — pick up here

### Driver
- [x] **`EvtFileClose` added & VALIDATED ON HARDWARE** → the driver closes the
  L2CAP channel when the app closes its handle (clean exit OR crash), so the
  channel no longer leaks ("Can't disconnect" bug is GONE — confirmed: connect →
  quit app → Windows disconnected the AirPods immediately). `WDF_FILEOBJECT_CONFIG_INIT`
  in `Driver.c` (`AutoForwardCleanupClose = WdfFalse`) + `LpEvtFileClose` in
  `Ioctl.c` → `LpDisconnect(ctx)`. Built clean + installed (needed one reboot to
  swap the live driver). `prebuilt/` refreshed (16896-byte `.sys`, new `.cat`).

### App port (finish making the crate compile for Windows)
- [x] **Phase E done** — LE monitor abstracted. `platform::watch_le_advertisements()`
  returns a channel of `LeAdvertisement{address, apple_data}` (Linux backend
  `platform/linux/le_scan.rs` = the old `bluer::monitor` + per-device
  `dev.events()` source); `bluetooth/le.rs` now consumes that stream and keeps
  the RPA/IRK match + AES decode + battery/in-ear parse + tray update. The
  `bluetoothctl connect` shell-out moved to `platform::connect_device(&DeviceId)`.
  `start_le_monitor` no longer returns `bluer::Result`. Linux `cargo check` green,
  no new warnings. Windows = WinRT `BluetoothLEAdvertisementWatcher` (Phase I/J).
- [x] **Phase F done** — `AudioRouter` trait; libpulse (A2DP profile + sink
  volume) moved to `platform/linux/audio.rs`. `media_controller.rs` delegates.
- [x] **Phase G done** — `MediaControl` trait; MPRIS moved to
  `platform/linux/media.rs`. `media_controller.rs` has zero direct dbus/libpulse.
- [x] **Phase H done** — `TrayHandle` type + `platform::spawn_tray`; ksni isolated
  to Linux-gated code (`ui/tray.rs` impl + `platform/linux/tray.rs`).
- [x] **Phase I done — THE CRATE COMPILES FOR WINDOWS.**
  `cargo check --target x86_64-pc-windows-gnu` = 0 errors, Linux still green.
  `bluer`/`dbus`/`libpulse`/`ksni` gated to `cfg(target_os="linux")`; Windows
  stub backends for every trait in `platform/windows/{watcher,discovery,le_scan,
  audio,media,tray}.rs`; `aacp.rs`/`att.rs` use `std::io::{Error,Result}` not
  bluer; local MAC via `platform::local_adapter_address()`.

### Phase J — real Windows backends (replace the stubs) + app entrypoint
Cross-check target from WSL: `cargo check --target x86_64-pc-windows-gnu` (green).
The main crate's Windows deps now include the full `windows` crate (SMTC + Core
Audio + WinRT Bluetooth) and `Win32_Devices_Bluetooth` on windows-sys.
- [x] **SMTC media** (`platform/windows/media.rs`) — MediaControl via
  GlobalSystemMediaTransportControlsSession (is_playing/pause/pause_all/resume/
  Next+Previous), COM ensured per call.
- [x] **WASAPI volume** (`platform/windows/audio.rs`) — AudioRouter volume via
  IAudioEndpointVolume on the default render endpoint; A2DP no-op.
- [x] **WinRT LE watcher** (`platform/windows/le_scan.rs`) —
  BluetoothLEAdvertisementWatcher → Apple 0x004C manufacturer data → shared
  `bluetooth/le.rs` decoder.
- [x] **local_adapter_address** (`platform/windows/watcher.rs`) — WinRT
  BluetoothAdapter.BluetoothAddress.
- [x] **Device discovery** (`platform/windows/discovery.rs`) — Win32
  BluetoothFindFirstDevice; find_connected_airpods + find_other_managed_devices.
- [x] **watch_connections** — poll `find_connected_airpods` every 3s, emit
  Connected/Disconnected (synthesizes the AAP UUID so the main filter matches).
- [x] **tray-icon backend** — `WindowsTrayHandle`/`spawn_tray` render a real tray
  icon on a dedicated Win32-message-loop thread from the shared `MyTray`; menu
  routes ANC/conversation via `command_tx`, Open Window via `ui_tx`. WM_TIMER
  refresh each second.

**Every platform trait now has a real Windows backend.**
`cargo check --target x86_64-pc-windows-gnu` = 0 errors; Linux green.

- [x] **The `.exe` builds & links** — `cargo build --target x86_64-pc-windows-gnu`
  Finished (iced/wgpu/winit all link on the gnu toolchain). Output:
  `target/x86_64-pc-windows-gnu/debug/librepods.exe`.

- [x] **✅ VALIDATED ON HARDWARE (headless `--no-tray`)** — driver → transport
  IOCTL → AAP works end-to-end on real AirPods Pro: battery (L20/R17), ear
  detection, ANC state, proximity keys, and **ear-detection auto-pause via SMTC**
  (remove a bud → pauses, reinsert → resumes). The recv fix (cec20cf) made the
  session hold; the dedup guard fired. Test flow: quit `librepods-tray` (driver is
  EXCLUSIVE), reconnect the AirPods for a fresh channel (avoids the 0xC00000B5
  connect-timeout that rapid restarts cause), then `librepods.exe --no-tray`.

Polish / remaining:
- [ ] Smoke-test the **iced GUI** + **tray-icon** on hardware (only the headless
  Bluetooth path is validated; the GUI renders but wasn't functionally tested).
- [ ] Provision the config dir + empty `devices.json` at startup so the GUI stops
  spamming "Failed to read devices file" every frame (this accumulation drove the
  debug build to ~5.6GB; a fresh run is ~200MB; `--no-tray` avoids it entirely).
- [ ] Only insert into `device_managers` on a SUCCESSFUL connect — a failed
  initial connect currently blocks the watcher's retry via the dedup guard.
- [ ] Build a **release** exe (opt-level=s/lto/strip already set) — the real light
  deliverable (the tray release was 400KB vs the 580MB debug).
- [ ] Handle unknown AAP opcodes 0x3e/0x37/0x38/0x3b gracefully (logged as errors).

### Windows apps — features
- [x] **Ear-detection auto-pause (SMTC) — VALIDATED ON HARDWARE** in
  `librepods-tray` — pauses media when both AirPods leave your ears (confirmed:
  removed the buds → playback paused), resumes when one goes back in
  (MagicPods-style; no A2DP toggle, Windows manages that).
  `aap::parse_ear_detection` (opcode 0x06, bytes [6]/[7]) + `media.rs` (WinRT
  `GlobalSystemMediaTransportControlsSession` Pause/Play/PlaybackStatus) + a
  `we_paused` state machine in `run_receiver` (only resumes what WE paused). The
  one-time `REQUEST_NOTIFS` at handshake is what makes the buds push 0x06 events.
  Battery/ANC merge path unchanged.

### Windows apps polish
- [ ] **Unify into ONE app that supports both window + tray system.** Merge
  `librepods-tray` and `librepods-ui` into a single app that has the egui
  **window** AND a **system-tray icon**:
  - close/minimize → hide the window to the tray (keep running in background,
    passive AAP session stays alive so audio stays clean);
  - tray left-click / double-click → reopen the window;
  - tray right-click → quick menu (battery / ANC / quit);
  - integrate `tray-icon` into the eframe/winit event loop (poll
    `TrayIconEvent`/`MenuEvent`; `ViewportCommand::Visible` to hide/show);
  - optional: auto-start via `shell:startup`.
- [ ] Dedupe: the tray + ui apps each copy `driver.rs`/`aap.rs`/`bt.rs`/
  `volume.rs`. Make a shared lib crate (`librepods-win-core`) — do this as part
  of the unify step above (one app, one set of modules).
- [ ] Nicer egui theme (rounded, accent colors, dark/light).
- [ ] Heart rate (AirPods Pro 3): RE the AAP opcode — the apps already receive
  all packets; capture Mac↔AirPods with macOS PacketLogger during a workout to
  find the HR opcode/enable command. Uncertain but a cool target.

## 🔧 How to continue (commands / gotchas)

- **Build the Rust app (Linux):** no cargo in PATH — use nix devshell
  (`cd crossplatform/crossplatform-rust && nix develop -c cargo check`). rustup
  IS installed in WSL for cross-compiles: `. ~/.cargo/env`.
- **Build a Windows app (cross-compile from WSL):**
  `cd crossplatform/windows-app/<app> && cargo build --release --target x86_64-pc-windows-gnu`.
  Run it via WSL interop (it executes on the Windows host).
- **Build the driver:** needs VS2026 + WDK on Windows (SDK/WDK build numbers must
  match, e.g. 28000). Build via interop: `windows-driver/LibrePodsAAP/build-wsl.cmd`
  then `inf2cat` a package folder. `SignMode=Off` in the vcxproj.
- **Install the driver (Test Mode):** Secure Boot OFF + `bcdedit /set testsigning on`
  + reboot, then `install.ps1 -PackageDir <package-or-prebuilt>` (elevated).
- **Debug a BSOD:** dumps in `C:\Windows\Minidump` (admin-only — copy to Downloads,
  then `kd -z <dump> -c "!analyze -v"` with MS symbols).
- **Gotchas:** driver is EXCLUSIVE (one handle — kill other app first). egui/winit
  needs COM **STA** (`COINIT_APARTMENTTHREADED`), not MTA. Keep the AAP session
  PASSIVE (no periodic sends) or audio cuts.

Full technical blueprint (WDK BRB API, IOCTL contract, etc.) is in the plan file
referenced by the `cross-platform-port` memory.
