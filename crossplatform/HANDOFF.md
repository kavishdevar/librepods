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
- [ ] **Add `EvtFileClose`** to the driver → close the L2CAP channel when the
  app closes its handle. Currently the channel LEAKS on app exit → Windows says
  "Can't disconnect" until manual disconnect/reboot. This is the top fix.
  (KMDF: `WDF_FILEOBJECT_CONFIG_INIT` + `EvtFileClose` → `LpDisconnect(ctx)`.)
  Then rebuild + re-sign + re-install (Test Mode cycle).

### App port (finish making the crate compile for Windows)
- [ ] **Phase E** — LE monitor: abstract `bluetooth/le.rs` (`bluer::monitor`
  Apple 0x004C scan). The RPA/IRK decode stays; only the advert source moves to
  `platform::`. Windows = WinRT `BluetoothLEAdvertisementWatcher`.
- [ ] **Phase F/G** — `media_controller.rs`: `AudioRouter` (libpulse) +
  `MediaControl` (MPRIS). Windows: WASAPI + SMTC (or no-op audio; media via SMTC).
- [ ] **Phase H** — tray: abstract `ksni` (`ui/tray.rs`). Windows = `tray-icon`.
- [ ] **Phase I** — move `bluer`/`dbus`/`libpulse`/`ksni` under
  `[target.'cfg(target_os="linux")']`; add Windows stubs so
  `cargo check --target x86_64-pc-windows-msvc` compiles. THEN the crate builds
  for Windows.
- [ ] Windows backends for D–H (WinRT adapter/watcher/LE, SMTC, tray-icon) +
  wire the app entrypoint. `platform/windows/transport.rs` already bridges to the
  driver via IOCTL — reuse for the app's L2CAP.

### Windows apps polish
- [ ] Dedupe: the tray + ui apps each copy `driver.rs`/`aap.rs`/`bt.rs`/
  `volume.rs`. Make a shared lib crate (`librepods-win-core`).
- [ ] `librepods-ui`: minimize-to-tray (integrate `tray-icon` into the eframe
  winit loop), auto-start (shell:startup), nicer theme.
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
