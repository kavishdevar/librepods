# LibrePods on Windows 🪟 — native, open-source, cross-platform

This directory brings the full **LibrePods** app to **Windows**, natively and
100% open-source, while **keeping Linux working from the same code**. It builds
on the Linux Rust app ([PR #241](https://github.com/librepods-org/librepods/pull/241))
and extends it into a single **cross-platform** crate plus two open-source
**Windows kernel drivers** — one for the Apple Accessory Protocol (AAP), one that
exposes the AirPods as a native **microphone**.

> Apple's AAP runs over a classic-Bluetooth L2CAP channel (PSM `0x1001`) that
> Windows won't let user-mode apps open. So we wrote a signed **KMDF profile
> driver** (`LibrePodsAAP`) that opens the channel in kernel mode and bridges it
> to user space — the same protocol LibrePods already speaks, now on Windows.

## ✅ What works (validated on real AirPods Pro on Windows)

**Control & status**

| Feature | Status |
| --- | --- |
| **Battery** (L / R / Case, charging state) | ✅ matches iPhone |
| **Noise control** (Off / ANC / Transparency / Adaptive) | ✅ read + control |
| **Ear detection** | ✅ |
| **Device info** (model, serials, firmware) | ✅ |
| **LE battery** via BLE advertisement (IRK/enc keys) | ✅ |

**Audio**

| Feature | Status |
| --- | --- |
| **Auto-pause / resume** when you remove/insert a bud | ✅ via SMTC |
| **Conversational Awareness** (volume ducking when you speak) | ✅ via WASAPI |
| **Hi-res microphone** — the AirPods AAC-ELD mic as a native Windows input | ✅ see below |

**App & UX**

| Feature | Status |
| --- | --- |
| **Full iced GUI** running natively | ✅ |
| **System tray** | ✅ (`tray-icon`) |
| **MagicPods-style centered popup** (connect / ANC / case) | ✅ (v1) |
| **Auto-start at login** | ✅ |

The whole app also **compiles and links as a Windows `.exe`**
(`cargo build --target x86_64-pc-windows-gnu`), and **Linux stays green** at
every step.

## 🎙️ The hi-res microphone

Windows has no API to "create a virtual microphone", so we ship a **second**
kernel driver — `LibrePodsMic`, a virtual audio device (ACX) that appears as a
real capture endpoint. The tray reads the AirPods' uplink audio over AAP, decodes
it, and streams PCM into that device, so **any app (Teams, Zoom, Discord, OBS…)
can use the AirPods mic** — the Windows counterpart to Linux
[PR #655](https://github.com/librepods-org/librepods/pull/655).

```
AirPods ──AAP/L2CAP──▶ LibrePodsAAP ──IOCTL──▶ tray: decode AAC-ELD → resample → PCM
                                                          │
                                                          ▼
                                          LibrePodsMic virtual mic ──▶ any app
```

| Capability | Status |
| --- | --- |
| AAC-ELD decode (FFmpeg/libavcodec, LGPL) + resample to 48 kHz | ✅ clean & in-tune |
| **Auto-activate** — hi-res stream turns on when an app records, off when it stops | ✅ (+ manual mode) |
| **A2DP auto-reset** — restores stereo playback after the mic degrades it to mono | ✅ |
| **Make-up gain** — the mic isn't quiet | ✅ (×3, soft-limited) |
| **Dynamic name** — the mic shows the connected device ("AirPods Pro de …", Beats…) | ✅ auto, no UAC |
| Minimal FFmpeg build (aac-only) — avcodec **69 MB → 0.7 MB** | ✅ |

The dynamic name is set by `lp-mic-rename` (writes the endpoint's `DeviceDesc` the
same way the Sound "Rename" UI does), launched by the tray through an elevated
on-demand scheduled task — so it happens automatically with no UAC prompt.

## How it's built — one crate, two operating systems

All OS-specific integration lives behind a `platform/` abstraction selected at
compile time (`#[cfg(target_os)]`). The protocol/UI code (`aacp`, `att`, `le`,
`media_controller`, the iced GUI) is **identical** on both platforms.

| Concern | Linux backend | Windows backend |
| --- | --- | --- |
| L2CAP transport | `bluer` SeqPacket | **LibrePodsAAP driver** via IOCTL |
| Discovery / adapter / watcher | BlueZ + D-Bus | Win32 / WinRT Bluetooth |
| LE advertisements | `bluer::monitor` | WinRT `BluetoothLEAdvertisementWatcher` |
| Audio routing / volume | PulseAudio | WASAPI (Core Audio) |
| Media control | MPRIS | SMTC |
| Microphone | PipeWire virtual input | **LibrePodsMic** virtual audio driver |
| System tray | `ksni` | `tray-icon` |
| Paths | XDG | `%APPDATA%` |

## Layout

- **`app/`** — the shared LibrePods app (Linux + Windows), with
  the `platform/` abstraction layer.
- **`windows/drivers/aap/`** — the open-source KMDF AAP kernel driver
  (`+ prebuilt/` so it installs without the WDK; Test Mode).
- **`windows/drivers/mic/`** — the virtual-microphone audio driver (ACX),
  plus **`lp-mic-rename/`** (names the mic after the connected device).
- **`windows/`** — a lightweight native **tray app** (battery + ANC + volume
  + ear-detection auto-pause + the hi-res mic + the popup overlay) and an egui
  window, plus the `lp-driver-test` CLI.

## Install (Windows, Test Mode)

**Ready-to-install builds are in [`windows/dist/`](windows/dist/)** — the apps,
the driver, and a one-shot `install.ps1`. The drivers are test-signed, so they
need Secure Boot **off** + `bcdedit /set testsigning on` + reboot; then run the
installer (elevated). Full steps are in
[`windows/dist/README.md`](windows/dist/README.md); build instructions + the
technical log are in `HANDOFF.md`.

> **Signing note.** Kernel drivers must be signed to load. For personal use we
> test-sign (above). Distributing to others without Test Mode needs an **EV
> certificate + Microsoft Partner Center** attestation (Azure Trusted Signing
> covers the user-mode app for SmartScreen, but **not** kernel drivers).

## Why two kernel drivers (no user-mode alternative)

Both drivers exist because **Microsoft exposes no user-mode API** for what they
do — verified against the Windows driver docs:

- **AAP control (`LibrePodsAAP`)** — AAP runs over a classic-Bluetooth **L2CAP**
  channel (PSM `0x1001`). Windows only lets you open an L2CAP client connection
  from a **kernel profile driver** (`BRB_L2CA_OPEN_CHANNEL`); there is **no
  WinRT/Winsock user-mode L2CAP client** (a Winsock `AF_BTH` spike returned
  `WSAENETDOWN`). So ANC/battery/controls need the driver.
- **Hi-res microphone (`LibrePodsMic`)** — Windows has **no user-mode API to
  create a virtual audio device**. Everything that does (VB-Cable, SysVAD, ours)
  is a WDM/kernel driver. User-mode **APOs** can only post-process *existing*
  devices, not add a new input.

What you *could* do driverless, and why it's not enough:

| User-mode option | Gives | Loses |
| --- | --- | --- |
| BLE (GATT / advertisements) | Battery (we use it for the LE battery + proximity prompt) | **No** ANC/controls (those are L2CAP) |
| Windows' built-in HFP mic | A mic with no driver | Low quality (mono, narrowband) **and** it degrades A2DP playback |

So the drivers aren't over-engineering — they're what unlocks **ANC control** and
the **hi-res mic**. The cost is Test Mode (personal) or an EV cert (distribution).

## Not done yet / in progress

- ~~Fold the `LibrePodsMic` driver install into the one-shot `install.ps1`.~~ ✅
  `install.ps1` now installs **both** drivers + the mic-rename task in one shot.
- One-click installer polish: drop the Windows SDK requirement (bundle
  `signtool`), and wrap it in a plain double-click `.cmd`.
- Unify the interim tray app and the iced window into one app.
- Release build packaging; precise BLE lid-open detection; heart-rate RE
  (AirPods Pro 3).

See **`HANDOFF.md`** for the full technical log, remaining TODOs, and the
reverse-engineering notes.
