# LibrePods Wear OS — AI Development Roadmap

Date: 2026-08-20

## Goal

Turn this fork of LibrePods into a fully autonomous Wear OS application for direct AirPods control. The watch must communicate with AirPods directly; a phone companion must not be required for normal operation.

## Current state audit

The repository is a direct fork of LibrePods and currently contains the full Android application plus the Linux implementation. The Android app is a phone-oriented application with Compose UI, billing/Play dependencies, Quick Settings, widgets, foreground services, boot handling, and root/Xposed-related Bluetooth workarounds.

The most valuable reusable layer is the existing Bluetooth/protocol implementation:

- `AACPManager.kt` — Apple Accessory Communication Protocol packet construction/parsing.
- `ATTManager.kt` — AirPods ATT channel handling.
- `BLEManager.kt` — BLE discovery/GATT support.
- `BluetoothConnectionManager.kt` — connection lifecycle.
- `data/` — AirPods models, capabilities and state.
- `services/` — parts of the device/background lifecycle that can be adapted after audit.

## Phase 0 — Safe cleanup

- Preserve GPL-3.0 and upstream attribution.
- Keep the fork relationship and original protocol implementation history.
- Remove Linux-only application code from the Wear OS product branch.
- Remove root-module packaging and Android root/Xposed-only native hooks from the Wear target.
- Remove phone-only billing, Quick Settings, widgets, telephony and other irrelevant permissions/features.
- Do not delete protocol code until dependency usage has been checked.

## Phase 1 — Wear OS build skeleton

- Convert the Android application module into a Wear OS-compatible application.
- Set a Wear OS appropriate package/application identity while preserving upstream notices.
- Use current Android/Wear OS SDKs compatible with the target watch.
- Add a minimal launcher activity using Compose for Wear OS.
- Keep the first build intentionally small and dependency-light.

## Phase 2 — Direct Bluetooth connection

- Implement Wear-specific Bluetooth adapter layer.
- Discover AirPods.
- Connect directly from the watch.
- Establish BLE/ATT and AACP channels.
- Add connection timeout, retry and reconnect handling.
- No phone relay.

## Phase 3 — First useful controls

- AirPods identification.
- Left/right/case battery.
- Connection state.
- Listening mode: Noise Cancellation / Transparency / Off where supported.
- Ear detection.

## Phase 4 — Advanced protocol features

- Conversational Awareness.
- Head gestures.
- Press/hold configuration.
- Custom EQ/accessibility controls where supported.
- Rename/configuration.
- Additional notifications/state synchronization.

## Phase 5 — Wear OS UX

- Round and rectangular watch layouts.
- Large touch targets.
- Fast connection screen.
- Tile for quick listening-mode control.
- Optional complication.
- Battery-efficient background service.
- Screen must remain usable during pairing/connection flows without unnecessary timeout behavior.

## Phase 6 — Compatibility and hardening

- Test Wear OS 3/4/5/6 where practical.
- Test Samsung Galaxy Watch hardware first.
- Test direct connection without phone.
- Test reconnect after Bluetooth toggles, watch restart, AirPods case open/close and range loss.
- Add protocol logging suitable for reverse-engineering/debugging without leaking private keys.

## Rule for cleanup

Do not blindly port the Android application UI or root workarounds. First preserve the protocol/core behavior, then build the Wear OS platform layer around it.

## Documentation convention

- Code comments: English.
- Roadmap and architecture documentation: English.
- Each major cleanup or architecture change gets a focused commit.
- Do not mix protocol changes with large UI refactors.
