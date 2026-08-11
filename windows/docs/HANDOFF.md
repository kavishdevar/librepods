# LibrePods — cross-platform (Windows) handoff

Branch: `windows-hires-mic` (off `cross-platform`, pushed to
`github.com/arctumn/librepods`). Everything below is committed.

> **Architecture note (2026-08):** the Windows side is now a **daemon + thin
> clients**, NOT the old `librepods-tray`/`librepods-ui` (egui) split. The daemon
> (`librepodsd`) owns the exclusive driver + AAP session + hi-res mic + volume and
> is the single arbiter; the tray and the iced app are IPC clients. The older
> egui `ui` app and the tray-as-session are retired. The reverse-engineering
> section at the bottom is still current and useful.

## ✅ Resume — what's done & working (daemon era)

**Kernel driver — `LibrePodsAAP`** (`crossplatform/windows/drivers/aap/`)
KMDF profile driver bound to the AAP SDP service
`BTHENUM\{74ec2172-0bad-4d01-8f77-997b2be0722a}`; opens the L2CAP AAP channel
(PSM 0x1001) in kernel mode, bridges to user space via IOCTLs (CONNECT/SEND/
RECEIVE/GET_STATUS). Exclusive (one handle). Prebuilt package installs without
C++/WDK (`install.ps1`, Test Mode). **Second driver — `LibrePodsMic`** = the
virtual microphone endpoint the daemon writes decoded AAC-ELD PCM into.

**`librepodsd`** (`crossplatform/windows/daemon/`) — the headless daemon, the
single owner of the driver. Responsibilities:
- **AAP session** (`run_receiver`): passive hold (handshake once, then listen;
  liveness via GET_STATUS, no periodic L2CAP sends → audio stays crystal-clear).
- **Battery / ANC / ear-detection** parse → pushes `Snapshot` to clients.
- **Hi-res microphone** (AAC-ELD): enables the 0x58 uplink, decodes via FFmpeg
  libavcodec (64→48 kHz), writes to `\\.\LibrePodsMic`. Auto-enables on recording
  (`poll_mic`) or manual. **SDU watchdog** (PR #655): re-arms the uplink if audio
  SDUs stall >2 s (kills mid-call static).
- **Volume owner** (`volume.rs`): reads/writes the WASAPI default-render volume,
  reports it in the Snapshot, serves StepVolume/ToggleMute, and **ducks for
  Conversational Awareness** (0x4B event → ConvDuck, Apple-style 25%→15%,
  restores on end; also restores if CA is toggled off mid-duck).
- **Feature toggles** (`SetFeature`): Conversational Awareness (0x28), Adaptive
  Volume (0x26), Allow-Off (0x34) — generic `control_command`; checkmarks sync
  from the AirPods' own status echoes.
- **Never-steal + BLE prompt** (`le.rs`): passive LE watch while disconnected;
  on proximity, sends a `ConnectPrompt` — the session starts ONLY after the user
  accepts (never pulls the AirPods off the iPhone). Gives up after ~18 s.
- **Ear-detection auto-pause**: pauses on a single-bud removal (Apple-style,
  transition-based), resumes when both are back in.
- **Notifications** (overlays): Connected (with battery) / Disconnected, ANC
  change, case open/close, **low battery** (≤20%, hysteresis), **charging**
  (names the bud + levels) / **fully charged**.
- **Settings toggles + controls**: Conversational Awareness, Adaptive Volume,
  Allow-Off, **Adaptive noise strength** (0x2E, Low/Med/High). CA does NOT duck
  while the hi-res mic is in use (a call — you're talking but the audio mustn't
  drop). **Rename** is via the iced app's Device-name field (over the L2CAP proxy).
- **App L2CAP proxy** (Phase 3): forwards raw AAP packets to the iced app over
  two pipes so the app can run its own session over the daemon's channel; caches
  + replays battery/ANC on attach.

**`librepods-tray`** (`crossplatform/windows/tray/`) — thin IPC client: tray
icon/menu/overlay from the daemon's `State` events; sends commands. Single
instance. Spawns the daemon if absent. "Open App" launches the iced app (they
coexist).

**`librepods` app** (`crossplatform/app/`) — the shared iced app. On Windows it's
a client of the daemon (AAP session over the L2CAP proxy; MediaController/LE/tray
no-op'd via `cfg`). Window-close quits the process; single-instance guard.
Linux keeps its native bluer session unchanged (everything Windows-specific is
`#[cfg(windows)]`-gated — Linux never uses these drivers).

**`librepods-ipc`** (`crossplatform/windows/ipc/`) — shared protocol: NDJSON over
named pipes. Two one-directional pipes per channel (a sync duplex handle
deadlocks). `Command`/`Event`/`Snapshot`; DACL lets same-user clients connect.

## 📋 TODO — pick up here

### Features — done this cycle
- [x] Notifications: Connected/Disconnected, charging (names the bud + levels) /
  fully-charged, low battery.
- [x] Adaptive noise strength (0x2E) — tray submenu Low/Med/High via a generic
  `SetControl { id, value }` IPC command.
- [x] Rename — the app's Device-name field sends `send_rename_packet` over the
  L2CAP proxy on Windows (no daemon change needed).
- [x] CA suppressed while the hi-res mic is in use; charging state accumulated
  across partial battery packets.

### Features — queue
- [ ] **Case low battery** notification (buds low is done).
- [ ] Sync the Adaptive-noise submenu to the device's current 0x2E value (parse
  the status echo) + reflect the daemon `dev_name` after an in-app rename.
- [ ] **Stem / press-and-hold controls** — StemConfig 0x39 (bitmask
  single=0x01/double=0x02/triple=0x04/long=0x08); press modes 0x14/0x15/0x16.
- [ ] **One-bud ANC** (0x1B), **Volume Swipe** (0x25) — the app only *parses*
  these, so the SET encoding is UNPROVEN. Capture (PacketLogger, differential)
  before implementing — do NOT guess.
- [ ] **Loud Sound Reduction** — via ATT (PSM 0x1F), not AAP.

### Polish / known issues
- [ ] Tray "app starting" busy cursor for ~2-5 s on launch (Windows default for a
  windowless tray app while the daemon spins up — cosmetic).
- [ ] Realtek RTL8852BE output static is BLE/Wi-Fi coexistence (hardware). User
  is swapping to **Intel AX210** (works on AMD; BE200/Wi-Fi 7 does NOT on AMD).

### Driver / installer
- [x] One-click installer bundling both drivers + the app (`windows/dist/`).
- [ ] Attestation/WHQL signing for third-party distribution (currently Test Mode).

## 🔧 How to continue (commands / gotchas)

- **Build a Windows binary (cross-compile from WSL):**
  `cd crossplatform/windows/<daemon|tray> && cargo build --release --target x86_64-pc-windows-gnu`
  (the iced app: `crossplatform/app`). Runs on the Windows host via WSL interop.
- **Deploy:** copy the `.exe`s to
  `C:\Users\<user>\AppData\Local\LibrePods\`. The tray respawns the daemon, so to
  swap the daemon **kill both**: `Stop-Process -Name librepods,librepods-tray,librepodsd -Force`.
  A running `.exe` is locked → "Permission denied" on copy means it's still up.
- **Logs:** `%LOCALAPPDATA%\LibrePods\daemon.log` + `tray.log`.
- **Build the driver:** VS2026 + WDK on Windows (`windows/drivers/aap/build-wsl.cmd`);
  install with `install.ps1` (Test Mode: Secure Boot off + `bcdedit /set testsigning on`).
- **FFmpeg (hi-res mic):** BtbN `ffmpeg-master-latest-win64-lgpl-shared` (LGPL, no
  libfdk-aac). Bundle `avcodec-*.dll` + `avutil-*.dll`.
- **Gotchas:** driver is EXCLUSIVE (daemon owns it — clients go through IPC/proxy).
  Keep the AAP session PASSIVE (no periodic sends) or audio cuts. Each daemon
  thread touching WASAPI must `volume::init()` (COM MTA per thread). Two
  one-directional pipes per IPC channel (sync duplex handle deadlocks).

Full driver blueprint (WDK BRB API, IOCTL contract) is in the plan file referenced
by the `cross-platform-port` memory; the mic protocol is in `hires-mic-protocol`.

## 🔬 Reverse-engineering unknown AAP opcodes (Pro 3 settings, heart rate)

The AirPods Pro 3 (H2, model A3064) push control commands LibrePods doesn't map:
ids `0x37`, `0x38`, `0x3b`, `0x3e`, plus opcodes `0x44` (an AAP *Send* from the
Mac) and `0x4e`, and "EQ Data". `HrmState` (heart-rate monitor!) is already
decoded and appears in the log — a great target.

**Method that works (validated 2026-08-06):** capture with **PacketLogger** on a
Mac (needs Apple's "Bluetooth" logging profile + reconnect — macOS doesn't log
HCI by default). PacketLogger decodes the AAP L2CAP channel (ID 0x060D) and
labels rows **AAP Send/Receive**; filter for `AAP`. Control commands are the
**11-byte** packets `04 00 04 00 09 00 <ID> <VALUE> 00 00 00`; the big 559-byte
`...17...` packet is the full settings dump. Differential RE: clear → change ONE
setting → read the short packet's `<ID>`/`<VALUE>`. CONFIRMED:
`04 00 04 00 09 00 0D 02 00 00 00` = Listening Mode (id 0x0D) = 0x02 (Noise
Cancellation). NOTE: macOS changes some settings via **GATT/ATT (BLE)**, not AAP —
but the AirPods still echo the new state as AAP control commands, which is what we
map.

**RE session 2026-08-06 — every mainstream setting maps to an ALREADY-KNOWN
identifier:** Listening Mode = 0x0D; Conversation Awareness = opcode 0x4B (status
byte at packet[9], 1=start/2/3/4=end, drives volume ducking); Personalized Volume
= 0x26 (AdaptiveVolumeConfig); Adaptive Audio noise slider = 0x2E (AutoAncStrength,
0..100); Head Gestures = **GATT** (ATT handle 0x1B) not AAP (control path
unconfirmed). Still-unmapped ids `0x37/0x38/0x3b/0x3e` are likely Pro 3-exclusive
automatic features with no simple Settings toggle (heart rate — HrmState 0x30
already appears; Live Translation; Hearing Health). Chase them in those scenarios.

### Heart rate (Pro 3) — protocol CONFIRMED correct; readings blocked by environment, not code (2026-08-08)

Two independent lines of evidence pin this down:

1. **PacketLogger capture of a live iOS Fitness/Strava workout** (real Pro 3):
   our SensorDataWX START (`…17 00 00 00 10 00 10 00 08 E3 46 42 0B 08 13 10 02
   1A 05 01 40 42 0F 00`, service 0x13=19, 1 s, seq 0x2363) is **byte-identical**
   to the frame the iPhone sends. During the workout the AirPods stream **79-byte**
   reading frames (payload len 0x43); standalone we get only **21-byte status
   heartbeats** (log_type 3, service 19, command `4a 02 08 13`, no 18-byte reading
   payload). The other iOS sends are NOT HR triggers: `0x44` (`04 00 04 00 44 00
   04 00 02 00 03 07`) is a capabilities/mode negotiation (we tried it → only a
   ~489-byte "VendorID" caps dump); `0x10` (86 B) is **Nearby-Audio** telemetry
   (cleartext `idleTime`/`btAddress`/`btName="iPhone"`/`nearbyAudioScore` — audio
   handoff arbitration).

2. **`tomppi/airpods_rtbuddy_v37_probe` (v39, Android) reads real BPM standalone**
   with a flow **byte-identical to ours**: AACP CONNECT/CAPS service 0 & 4 (delays
   180/220/180/220), `HRM_ENABLE 0x30`, **one** SensorDataWX HEARTRATE(19) 1 s
   start (seq 0x2363), then passive listen — no 0x44, no 0x893/DEVMOTION6, no
   re-pulsing. Decoder rule (matches ours): descriptor 0x00100000, service 19, cmd
   payload len 18, outer log_type 3, status tail `10 00 00`, bpm = payload[1];
   reject transient frames (`10 82 81`, `10 02 81`).

**Conclusion:** the protocol and decoder are CORRECT — a byte-identical sequence
reads HR on another Pro 3. Our daemon streams the HEARTRATE service (status
frames) but never receives the 18-byte reading payload, so on this unit the sensor
isn't producing samples. Since identical bytes work elsewhere, the block is
**environmental, not the code**. Most likely, in order of suspicion:

- **Nearby iPhone arbitration.** AirPods run audio-handoff arbitration (the `0x10`
  Nearby-Audio telemetry). Every HR test here had the owner's iPhone active and
  nearby (it was driving the PacketLogger captures). **RETEST with the iPhone's
  Bluetooth OFF / powered off / left in another room**, AirPods connected ONLY to
  Windows — the strongest untested variable.
- **Physical fit / skin contact** — both buds seated firmly, worn still for 60 s+.
- **Firmware version** differences between units.

Daemon flow now matches v39 exactly: init svc0/4 → `0x30` → single START → hold the
stream open passively (no 0x44, no keepalive re-pulsing). Do NOT re-add those —
they were tested and don't help. **Update:** retested with the iPhone in Airplane
mode AND powered off — still only status heartbeats, no readings. So the nearby
iPhone is NOT the cause either; the remaining variable is hardware-side (optical
sensor fit/contact — leading suspect — or firmware version). Not code. Revisit by
just enabling HR (no redeploy); do not re-open the protocol.

## 🐞 Known open issues (WinUI client)

1. **Tray context-menu clipping.** The H.NotifyIcon.WinUI tray menu clips its items
   horizontally (background + text cut at a narrow fixed width). Confirmed NOT
   fixed by `MenuFlyoutPresenterStyle MinWidth=240` +
   `menu.ShouldConstrainToRootBounds = false` + `ContextMenuMode.SecondWindow`
   (all in `Tray/TrayIcon.cs`, verified in the deployed build). The SecondWindow
   host window is too narrow and clips the flyout. Next approach: replace the XAML
   flyout with a **native Win32 popup menu** (CreatePopupMenu/TrackPopupMenu on
   right-click). Localization is fine (menu shows pt-PT strings).
2. **Notification spam.** Daemon overlay/toast notifications (low battery,
   charging, connect/disconnect, "in case") fire repeatedly rather than once per
   state transition. Add a one-shot-per-transition guard on the notification path
   (mirror the existing battery `low_warned`/`case_low_warned` hysteresis in
   `run_receiver`).
