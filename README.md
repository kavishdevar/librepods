# ALN - AirPodsLikeNormal
*Bringing AirPods' Apple-exclusive features on linux and android!*

## [XDAForums Thread](https://xdaforums.com/t/app-root-for-now-airpodslikenormal-unlock-apple-exclusive-airpods-features-on-android.4707585/)

## Tested device(s)
- AirPods Pro 2

Other devices might work too. Features like ear detection and battery should be available for any AirPods! Although the app will show unsupported features/settings. I will not be able test any other devices than the ones I already have (i.e. the AirPods Pro 2).

## Features

Check the [pinned issue](https://github.com/kavishdevar/aln/issues/20) for a list. 


## CrossDevice Stuff

> [!IMPORTANT]
> This feature is still in development and might not work as expected. No support is provided for this feature.

### Features

- **Battery Status**: Get battery status on any device when you connect your AirPods to one of them.
- **Control AirPods**: Control your AirPods from either of your device when you connect to one, like changing the noise control mode, toggling conversational awareness, and more.
- **Automatic Device Switching**: Automatically switch between your Linux and Android device, like when you receive a call, start playing music on Android while you're connected to Linux, and more!

Check out the demo below!

https://github.com/user-attachments/assets/d08f8a51-cd52-458b-8e55-9b44f4d5f3ab


## Linux

The Linux version is a simple tray-app, with a modern and adaptive ui. Still WIP, but most things work (battery, ear-detection, auto-pause, rename, etc.)

Check out the README file in [linux](/linux) folder for more info.

## Android

> Can I use aln without root?

**No, it's not possible to use aln without root.** You will have to root your device if you want to use aln, and there is no way around it. **No exceptions.**

### Screenshots

| | | |
|-------------------|-------------------|-------------------|
| ![Settings 1](/android/imgs/settings-1.png) | ![Settings 2](/android/imgs/settings-2.png) | ![Debug Screen](/android/imgs/debug.png) |
| ![Battery Notification and QS Tile for NC Mode](/android/imgs/notification-and-qs.png) | ![Popup](/android/imgs/popup.png) | ![Head Tracking and Gestures](/android/imgs/head-tracking-and-gestures.png) |
| ![Long Press Configuration](/android/imgs/long-press.png) | ![Widget](/android/imgs/widget.png) | ![Customizations](/android/imgs/customizations.png) |
| ![audio-popup](/android/imgs/audio-connected-island.png) | | |

### Installation

Currently, there's a [bug in the Android Bluetooth stack](https://issuetracker.google.com/issues/371713238) that prevents the app from working (upvote the issue - click the '+1' icon on the top right corner of IssueTracker).

> [!CAUTION]
> Until Google merges the fix **you will only be able to use aln if you are rooted**. There are **no exceptions**, don't ask about it.

In order to use aln you will have to install the module using your favorite root manager in OverlayFS mode (KernelSU, Apatch, or Magisk). If you don't know what this means, no support is provided: you will have to search by yourself on Google or ask in some Android rooting communities on Telegram. 

The module to install is available in the releases section under the name `btl2capfix.zip`.

### Android – features

#### Renaming the Airpods
When you rename the Airpods using the app, you'll need to re-pair it with your phone. Currently, user-level apps cannot directly rename a Bluetooth device. After re-pairing, your phone will display the updated name!

#### Noise Control Modes

- Active Noise Cancellation (ANC): Blocks external sounds using microphones and advanced algorithms for an immersive audio experience; ideal for noisy environments.
- Transparency Mode: Allows external sounds to blend with audio for situational awareness; best for environments where you need to stay alert.
- Off Mode: Disables noise control for a natural listening experience, conserving battery in quiet settings.
- Adaptive Transparency: Dynamically reduces sudden loud noises while maintaining environmental awareness, adjusting seamlessly to fluctuating noise levels.

> [!IMPORTANT]
> Due to recent AirPods' firmware upgrades, you must enable `Off listening mode` to switch to `Off`. This is because in this mode, louds sounds are not reduced!

#### Conversational Awareness

Automatically lowers audio volume and enhances voices when you start speaking, making it easier to engage in conversations without removing your AirPods.

#### Automatic Ear Detection

Recognizes when the AirPods are in your ears to automatically play or pause audio and adjust functionality accordingly.

## Check out the packet definitions at [AAP Definitions](/AAP%20Definitions.md)

# License

AirPodsLikeNormal (ALN) - Bringing Apple-only features to Linux and Android for seamless AirPods functionality!
Copyright (C) 2024 Kavish Devar

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program over [here](/LICENSE). If not, see <https://www.gnu.org/licenses/>.
