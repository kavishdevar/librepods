# LibrePods on Windows

Open-source AirPods control for Windows: read battery and switch noise-control
modes (Off / Noise Cancellation / Transparency / Adaptive) from the system tray.

It has two parts:

1. **`LibrePodsAAP` kernel driver** ([`../windows-driver/LibrePodsAAP`](../windows-driver/LibrePodsAAP)) —
   opens the Apple Accessory Protocol (AAP) L2CAP channel to the AirPods in
   kernel mode, which normal Windows apps cannot do, and exposes it via IOCTLs.
2. **`librepods-tray` app** (this folder) — a tiny background app that talks to
   the driver and puts battery + noise-control in the system tray.

Works with any AirPods (2/3, Pro 1/2/3, Max) and Apple Beats — the driver binds
to the AAP service every AirPod advertises, not to a specific model. Features
shown depend on the model (e.g. only Pro/Max have noise control).

---

## 1. Install the driver (one-time)

> ⚠️ The driver isn't signed by Microsoft, so Windows must run in **Test Mode**
> (same requirement as the commercial MagicAAP driver). This lowers a security
> setting. **Advanced users only** — a system restore point is recommended.

### a) Prerequisites to build it
- Visual Studio 2022/2026 with **Desktop development with C++** + **Spectre
  x64/x86 libs** + a **Windows 11 SDK** and the **matching WDK** (SDK & WDK
  build numbers must match, e.g. `28000`).
- Build the driver package (`.sys` + `.inf`, then `inf2cat` a `.cat`) — see
  [`../windows-driver/LibrePodsAAP/README.md`](../windows-driver/LibrePodsAAP/README.md).

### b) Turn on Test Mode
1. Back up your **BitLocker recovery key** (if BitLocker is on) and make a
   **restore point**.
2. Disable **Secure Boot** in your firmware/BIOS (it blocks test-signed drivers).
3. In an **admin** PowerShell: `bcdedit /set testsigning on` → **reboot**.
   You should see "Test Mode" in the bottom-right of the desktop.

### c) Install
In an **admin** PowerShell, run the helper (it test-signs and installs, removing
any previous version):
```powershell
& "<path>\LibrePodsAAP\install.ps1" -PackageDir "<path>\LibrePodsAAP\package"
```
Success shows `Driver package installed on device: BTHENUM\{74ec2172-...}`.
Check it loaded (should be `OK`, not error 52):
```powershell
Get-PnpDevice -FriendlyName "LibrePods AAP*" | Select Status
```

### Uninstall / revert
```powershell
pnputil /delete-driver oem<N>.inf /uninstall   # find <N> with: pnputil /enum-drivers
bcdedit /set testsigning off                    # then re-enable Secure Boot in BIOS
```

---

## 2. Run the tray app

Build (from WSL/Linux, cross-compiled): `cargo build --release --target x86_64-pc-windows-gnu`
(binary at `target/x86_64-pc-windows-gnu/release/librepods-tray.exe`), or build
natively on Windows. Just run `librepods-tray.exe` — a teal icon appears in the
tray. To start it with Windows, drop a shortcut in
`shell:startup` (Win+R → `shell:startup`).

### How it works
- On start it finds your paired AirPods, opens the driver, and starts an **AAP
  session** (connect → handshake → request notifications).
- A background thread reads the packets the AirPods push and keeps battery +
  noise-mode state up to date.
- **Left-click / right-click the tray icon** for the menu:
  - a line showing **Left / Right / Case** battery,
  - **Noise Control**: Off · Noise Cancellation · Transparency · Adaptive
    (the current one is checked; click to switch — sends the AAP command),
  - **Quit**.
- Hover the icon for a tooltip with battery + current mode.
- If the link drops it reconnects automatically.

### Bonus
Keeping the AAP session alive (app running) tends to **stabilize the audio** —
the AirPods stop bouncing between the HFP (mono, "static") and A2DP (stereo)
profiles, because a proper AAP host is talking to them.

### Notes / limits
- The driver is **exclusive** — only one app (this one, or `lp-driver-test`) can
  hold the channel at a time.
- No system-volume control yet (that's a Windows audio API, separate from AAP).
- Requires the driver installed and Test Mode on.
