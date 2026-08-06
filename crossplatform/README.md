# LibrePods on Windows 🪟 — native, open-source, cross-platform

This directory brings the full **LibrePods** app to **Windows**, natively and
100% open-source, while **keeping Linux working from the same code**. It builds
on the Linux Rust app ([PR #241](https://github.com/librepods-org/librepods/pull/241))
and extends it into a single **cross-platform** crate plus an open-source
**Windows kernel driver** for the Apple Accessory Protocol (AAP).

> Apple's AAP runs over a classic-Bluetooth L2CAP channel (PSM `0x1001`) that
> Windows won't let user-mode apps open. So we wrote a signed **KMDF profile
> driver** (`LibrePodsAAP`) that opens the channel in kernel mode and bridges it
> to user space — the same protocol LibrePods already speaks, now on Windows.

## ✅ What works (validated on real AirPods Pro on Windows)

| Feature | Status |
| --- | --- |
| **Battery** (L / R / Case, charging state) | ✅ matches iPhone |
| **Noise control** (Off / ANC / Transparency / Adaptive) | ✅ read + control |
| **Ear detection** | ✅ |
| **Auto-pause / resume** when you remove/insert a bud | ✅ via SMTC |
| **Conversational Awareness** (volume ducking when you speak) | ✅ via WASAPI |
| **Device info** (model, serials, firmware) | ✅ |
| **LE battery** via BLE advertisement (IRK/enc keys) | ✅ |
| **Full iced GUI** running natively | ✅ |
| **System tray** | ✅ (`tray-icon`) |
| **MagicPods-style centered popup** (connect / ANC / case) | ✅ (v1) |
| **Auto-start at login** | ✅ |

The whole app also **compiles and links as a Windows `.exe`**
(`cargo build --target x86_64-pc-windows-gnu`), and **Linux stays green** at
every step.

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
| System tray | `ksni` | `tray-icon` |
| Paths | XDG | `%APPDATA%` |

## Layout

- **`crossplatform-rust/`** — the shared LibrePods app (Linux + Windows), with
  the `platform/` abstraction layer.
- **`windows-driver/LibrePodsAAP/`** — the open-source KMDF AAP kernel driver
  (`+ prebuilt/` so it installs without the WDK; Test Mode).
- **`windows-app/`** — a lightweight native **tray app** (battery + ANC + volume
  + ear-detection auto-pause + the popup overlay) and an egui window, plus the
  `lp-driver-test` CLI.

## Install (Windows, Test Mode)

The driver is test-signed, so it needs Secure Boot **off** +
`bcdedit /set testsigning on` + reboot. Then run `install.ps1` (elevated) from
`windows-driver/LibrePodsAAP/prebuilt/`, and start the tray app.
Details + build instructions are in `HANDOFF.md` and the per-directory READMEs.

## Not done yet / in progress

- One-click redistributable installer (bundle driver + app).
- Unify the interim tray app and the iced window into one app.
- Release build packaging; precise BLE lid-open detection; heart-rate RE
  (AirPods Pro 3).

See **`HANDOFF.md`** for the full technical log, remaining TODOs, and the
reverse-engineering notes.
