![LibrePods Banner](/imgs/banner.png)

[![XDA Thread](https://img.shields.io/badge/XDA_Forums-Thread-orange)](https://xdaforums.com/t/app-root-for-now-airpodslikenormal-unlock-apple-exclusive-airpods-features-on-android.4707585/)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/kavishdevar/librepods)](https://github.com/kavishdevar/librepods/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/kavishdevar/librepods/total)](https://github.com/kavishdevar/librepods/releases)
[![GitHub stars](https://img.shields.io/github/stars/kavishdevar/librepods)](https://github.com/kavishdevar/librepods/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/kavishdevar/librepods)](https://github.com/kavishdevar/librepods/issues)
[![GitHub license](https://img.shields.io/github/license/kavishdevar/librepods)](https://github.com/kavishdevar/librepods/blob/main/LICENSE)
[![GitHub contributors](https://img.shields.io/github/contributors/kavishdevar/librepods)](https://github.com/kavishdevar/librepods/graphs/contributors)


> ## 🪟 New: Windows support (cross-platform)
>
> LibrePods now runs **natively on Windows** — battery, noise control, ear
> detection, auto-pause, conversational awareness and more — driven by an
> **open-source kernel driver** for Apple's AAP protocol, all from the **same
> Rust crate that powers Linux**. Validated on real AirPods Pro.
>
> **➡️ See [`crossplatform/README.md`](crossplatform/README.md) for what works,
> the architecture, and how to install.**

## What is LibrePods?

LibrePods unlocks Apple's exclusive AirPods features on non-Apple devices. Get access to noise control modes, adaptive transparency, ear detection, hearing aid, customized transparency mode, battery status, and more - all the premium features you paid for but Apple locked to their ecosystem.

## Device Compatibility

| Status | Device                | Features                                                   |
| ------ | --------------------- | ---------------------------------------------------------- |
| ✅      | AirPods Pro (2nd Gen) | Fully supported and tested                                 |
| ✅      | AirPods Pro (3rd Gen) | Fully supported (except heartrate monitoring)              |
| ⚠️      | Other AirPods models  | Basic features (battery status, ear detection) should work |

Most features should work with any AirPods. Currently, I've only got AirPods Pro 2 to test with.

## Key Features

- **Noise Control Modes**: Easily switch between noise control modes without having to reach out to your AirPods to long press
- **Ear Detection**: Controls your music automatically when you put your AirPods in or take them out, and switch to phone speaker when you take them out
- **Battery Status**: Accurate battery levels
- **Head Gestures**: Answer calls just by nodding your head
- **Conversational Awareness**: Volume automatically lowers when you speak
- **Hearing Aid\***
- **Customize Transparency Mode\***
- **Multi-device connectivity\*** (upto 2 devices)
- **Other customizations**:
  - Rename your AirPods
  - Customize long-press actions
  - Few accessibility features
  - And more!

See our [pinned issue](https://github.com/kavishdevar/librepods/issues/20) for a complete feature list and roadmap.

## Platform Support

### Linux

The Linux version runs as a system tray app. Connect your AirPods and enjoy:

- Battery monitoring
- Automatic Ear detection
- Conversational Awareness
- Switching Noise Control modes
- Device renaming

> [!NOTE]
> Work in progress, but core functionality is stable and usable.

For installation and detailed info, see the [Linux README](/linux/README.md).

### Windows

LibrePods runs **natively on Windows**, sharing the same Rust core as Linux. It
appears as a system-tray app (battery-% icon + noise control + volume + a
MagicPods-style popup), with a full window for detailed settings.

- Battery (L / R / Case)
- Noise control (Off / ANC / Transparency / Adaptive)
- Automatic ear detection (auto-pause) + Conversational Awareness
- **Hi-res AirPods microphone** — the AAC-ELD mic as a native Windows input for
  any app (Teams / Zoom / Discord / OBS), auto-named after the device
- BLE proximity **"connect?"** prompt — asks before connecting, never steals the
  AirPods from your phone

> [!CAUTION]
> Windows needs **two open-source kernel drivers** — one to open the AirPods' AAP
> control channel (classic-Bluetooth L2CAP, PSM `0x1001`), one for the virtual
> microphone. Microsoft exposes **no user-mode alternative**: L2CAP client
> connections are driver-only (`BRB_L2CA_OPEN_CHANNEL`), and there is no user-mode
> API to create a virtual audio device. For personal use the drivers are
> **test-signed** — which requires **Secure Boot off** + Test Mode. (Without the
> drivers you'd get only battery over BLE and the low-quality built-in HFP mic.)

For installation, what works, and the architecture, see the
[Windows README](/crossplatform/README.md).

> [!NOTE]
> **Use of AI.** The Windows port (drivers, daemon/IPC, hi-res mic, and docs) was
> built in pair-programming with **Claude** (Anthropic's Claude Code), fully
> human-directed and validated on real AirPods hardware. See the
> [Use of AI](/crossplatform/README.md#-use-of-ai) note for details. The Linux and
> Android sides are the upstream project's work.

### Android

#### Screenshots

|                                                                                        |                                                   |                                                                             |
| -------------------------------------------------------------------------------------- | ------------------------------------------------- | --------------------------------------------------------------------------- |
| ![Settings 1](/android/imgs/settings-1.png)                                            | ![Settings 2](/android/imgs/settings-2.png)       | ![Debug Screen](/android/imgs/debug.png)                                    |
| ![Battery Notification and QS Tile for NC Mode](/android/imgs/notification-and-qs.png) | ![Popup](/android/imgs/popup.png)                 | ![Head Tracking and Gestures](/android/imgs/head-tracking-and-gestures.png) |
| ![Long Press Configuration](/android/imgs/long-press.png)                              | ![Widget](/android/imgs/widget.png)               | ![Customizations 1](/android/imgs/customizations-1.png)                     |
| ![Customizations 2](/android/imgs/customizations-2.png)                                | ![accessibility](/android/imgs/accessibility.png) | ![transparency](/android/imgs/transparency.png)                             |
| ![hearing-aid](/android/imgs/hearing-aid.png)                                          | ![hearing-test](/android/imgs/hearing-test.png)   | ![hearing-aid-adjustments](/android/imgs/hearing-aid-adjustments.png)       |


here's a very unprofessional demo video

https://github.com/user-attachments/assets/43911243-0576-4093-8c55-89c1db5ea533

#### Root Requirement

> [!CAUTION]
> **You must have a rooted device with Xposed to use LibrePods on Android.** This is due to a [bug in the Android Bluetooth stack](https://issuetracker.google.com/issues/371713238). Please upvote the issue by clicking the '+1' icon on the IssueTracker page.
> 
> There are **no exceptions** to the root requirement until Google merges the fix.

Until then, you must xposed. I used to provide a non-xposed method too, where the module used overlayfs to replace the bluetooth library with a locally patched one, but that was broken due to how various devices handled overlayfs and a patched library. With xposed, you can also enable the DID hook enabling a few extra features.

## Bluetooth DID (Device Identification) Hook

Turns out, if you change the manufacturerid to that of Apple, you get access to several special features!

### Multi-device Connectivity

Upto two devices can be simultaneously connected to AirPods, for audio and control both. Seamless connection switching. The same notification shows up on Apple device when Android takes over the AirPods as if it were an Apple device ("Move to iPhone"). Android also shows a popup when the other device takes over.

### Accessibility Settings and Hearing Aid

Accessibility settings like customizing transparency mode (amplification, balance, tone, conversation boost, and ambient noise reduction), and loud sound reduction can be configured.

All hearing aid customizations can be done from Android, including setting the audiogram result. The app doesn't provide a way to take a hearing test because it requires much more precision. It is much better to use an already available audiogram result. 

To enable these features, enable App Settings -> `act as Apple Device`.

#### A few notes

- Due to recent AirPods' firmware upgrades, you must enable `Off listening mode` to switch to `Off`. This is because in this mode, louds sounds are not reduced.

- If you have take both AirPods out, the app will automatically switch to the phone speaker. But, Android might keep on trying to connect to the AirPods because the phone is still connected to them, just the A2DP profile is not connected. The app tries to disconnect the A2DP profile as soon as it detects that Android has connected again if they're not in the ear.

- When renaming your AirPods through the app, you'll need to re-pair them with your phone for the name change to take effect. This is a limitation of how Bluetooth device naming works on Android.

- If you want the AirPods icon and battery status to show in Android Settings app, install the app as a system app by using the root module.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=kavishdevar/librepods&type=Date)](https://star-history.com/#kavishdevar/librepods&Date)

# License

LibrePods - AirPods liberated from Apple’s ecosystem
Copyright (C) 2025 LibrePods contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program over [here](/LICENSE). If not, see <https://www.gnu.org/licenses/>.

All trademarks, logos, and brand names are the property of their respective owners. Use of them does not imply any affiliation with or endorsement by them. All AirPods images, symbols, and the SF Pro font are the property of Apple Inc.
