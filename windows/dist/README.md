# LibrePods for Windows 🪟 — ready-to-install

Native LibrePods for Windows, driven by two open-source kernel drivers —
`LibrePodsAAP` (control) and `LibrePodsMic` (virtual microphone). See
[`../../README.md`](../../README.md) for what works and the architecture.

## Contents

| File | What it is | Size |
| --- | --- | --- |
| `librepods.exe` | The full app (iced GUI: battery, ANC, audio settings, device info, volume slider) | ~14 MB |
| `librepods-tray.exe` | Lightweight tray app (battery-% icon, ANC, volume, ear-detection auto-pause, **hi-res mic**, popup overlay, "Open App") | ~0.4 MB |
| `lp-mic-rename.exe` | Names the virtual mic after the connected device (run by an elevated task) | ~0.9 MB |
| `driver/` | The `LibrePodsAAP` KMDF driver package (`.sys` / `.inf` / `.cat`) | — |
| `driver-mic/` | The `LibrePodsMic` virtual-audio driver package (`.sys` / `.inf` / `.cat`) | — |
| `tools/devcon.exe` | Microsoft device console — creates the `ROOT\AudioCodec` mic device | — |
| `install.ps1` | One-shot installer (both drivers + apps + mic-rename task + startup) | — |

## Install

> ⚠️ The driver is **test-signed**, so Windows must be in **Test Mode**. This
> requires disabling Secure Boot. Back up your BitLocker recovery key first.

1. **Enable Test Mode** (elevated PowerShell), then reboot:
   ```powershell
   bcdedit /set testsigning on
   ```
   (Secure Boot must be **off** in your BIOS for a test-signed driver to load.)
2. **Run the installer** (elevated) — signs + installs **both drivers**, copies
   the apps to `%LOCALAPPDATA%\LibrePods`, registers the mic-rename task, and adds
   the tray to startup:
   ```powershell
   .\install.ps1
   ```
   (Needs the Windows SDK/WDK present for `signtool`. `devcon` is bundled.)
3. **Reboot** to finish the driver install.
4. **Connect your AirPods.** The tray shows the battery % on its icon; its menu
   has noise control + volume, and **"Open App"** launches the full window. The
   AirPods also appear as a microphone in **Sound > Input**, auto-named after the
   device.

## Notes

- One handle only: the AAP driver is exclusive, so the tray and the full app
  can't run at once — "Open App" hands off (it quits the tray and launches the app).
- The virtual mic auto-activates when an app records and restores stereo playback
  when it stops.
- Built from `../../app` (app) and `../../windows-app` (tray) for
  `x86_64-pc-windows-gnu`, and `../../windows-driver` (both drivers).
- `tools/devcon.exe` is Microsoft's device console (from the WDK), bundled so the
  installer can create the mic device without the WDK installed.
