# LibrePods for Windows 🪟 — ready-to-install

Native LibrePods for Windows, driven by the open-source `LibrePodsAAP` kernel
driver. See [`../../README.md`](../../README.md) for what works and the
architecture.

## Contents

| File | What it is | Size |
| --- | --- | --- |
| `librepods.exe` | The full app (iced GUI: battery, ANC, audio settings, device info, volume slider) | ~14 MB |
| `librepods-tray.exe` | Lightweight tray app (battery-% icon, ANC, volume, ear-detection auto-pause, popup overlay, "Open App") | ~0.4 MB |
| `driver/` | The `LibrePodsAAP` KMDF driver package (`.sys` / `.inf` / `.cat`) | — |
| `install.ps1` | One-shot installer (driver + apps + startup) | — |

## Install

> ⚠️ The driver is **test-signed**, so Windows must be in **Test Mode**. This
> requires disabling Secure Boot. Back up your BitLocker recovery key first.

1. **Enable Test Mode** (elevated PowerShell), then reboot:
   ```powershell
   bcdedit /set testsigning on
   ```
   (Secure Boot must be **off** in your BIOS for a test-signed driver to load.)
2. **Run the installer** (elevated) — signs + installs the driver, copies both
   apps to `%LOCALAPPDATA%\LibrePods`, and adds the tray to startup:
   ```powershell
   .\install.ps1
   ```
3. **Reboot** to finish the driver install.
4. **Connect your AirPods.** The tray shows the battery % on its icon; its menu
   has noise control + volume, and **"Open App"** launches the full window.

## Notes

- One handle only: the driver is exclusive, so the tray and the full app can't
  run at once — "Open App" hands off (it quits the tray and launches the app).
- Built from `../../crossplatform-rust` (app) and `../../windows-app` (tray) for
  `x86_64-pc-windows-gnu`, and `../../windows-driver` (driver).
