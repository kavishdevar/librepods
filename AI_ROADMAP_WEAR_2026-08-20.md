# LibrePods Wear OS — AI Development Roadmap

Date: 2026-08-20

## Goal

Turn this fork of LibrePods into a fully autonomous Wear OS application for direct AirPods control. The watch must communicate with AirPods directly; a phone companion must not be required for normal operation.

## Architecture target

```text
Wear OS UI
    |
Wear connection/service layer
    |
Bluetooth transport
    |
ATT / AACP protocol core
    |
AirPods
```

The phone is not part of the normal control path.

## Phase 0 — Cleanup status

Completed in `wearos/initial-cleanup`:

- Preserved GPL-3.0 and upstream attribution.
- Removed Linux product code from the Wear branch.
- Removed Android-specific issue templates and funding metadata.
- Removed the boot receiver and boot-completion requirement.
- Removed phone media-control utility.
- Removed phone UI gesture/sensor helpers.
- Removed native-module/reverse-engineering helper utilities from the Wear target.
- Removed the rootless workaround helper from the Wear target.
- Reduced the Gradle version catalog to dependencies currently relevant to the Wear target.
- Kept the AirPods protocol implementation intact for the next isolation pass.

Still to audit before deletion:

- `AirPodsService.kt` — split lifecycle/service code from protocol logic instead of deleting it wholesale.
- Remaining `utils/` classes — keep only protocol/debug/security helpers that are actually referenced.
- `res/` — remove phone-only assets/resources after checking references.
- Native/root/Xposed remnants — remove only after confirming no protocol dependency remains.

## Phase 1 — Protocol/core isolation

- Keep `AACPManager.kt`.
- Keep `ATTManager.kt`.
- Keep `BLEManager.kt`.
- Keep `BluetoothConnectionManager.kt` as the starting point for the Wear transport.
- Keep AirPods data models and command definitions.
- Move reusable protocol code away from phone-specific application lifecycle.
- Remove phone-only APIs from the core.

## Phase 2 — Wear OS Bluetooth

- Implement Wear-specific Bluetooth adapter layer.
- Discover AirPods.
- Connect directly from the watch.
- Establish BLE/ATT and AACP channels.
- Add timeout, retry and reconnect handling.
- No phone relay.

## Phase 3 — First useful controls

- AirPods identification.
- Left/right/case battery.
- Connection state.
- Listening mode: Noise Cancellation / Transparency / Off where supported.
- Ear detection.

## Phase 4 — Advanced protocol features

- Conversational Awareness.
- Head gestures where the AirPods protocol supports them.
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
- Keep the screen usable during pairing/connection flows without unnecessary timeout behavior.

## Phase 6 — Compatibility and hardening

- Test Wear OS 3/4/5/6 where practical.
- Test Samsung Galaxy Watch hardware first.
- Test direct connection without phone.
- Test reconnect after Bluetooth toggles, watch restart, AirPods case open/close and range loss.
- Add protocol logging suitable for reverse-engineering/debugging without leaking private keys.

## Future projects

- Extract a reusable protocol core for Android, Wear OS, Linux and Windows where practical.
- Build a native LibrePods Windows client after the Wear OS port is stable.

## Development rules

- Code comments: English.
- Roadmap and architecture documentation: English.
- Focused commits for cleanup and architecture changes.
- Do not delete protocol code merely because it is not used by the current UI; verify dependencies first.
- Do not mix protocol changes with large UI refactors.
