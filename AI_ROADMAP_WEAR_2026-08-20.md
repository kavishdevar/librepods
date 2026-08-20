# LibrePods Wear — AI Roadmap

Date: 2026-08-20

## Goal

Turn this fork of LibrePods into a fully autonomous Wear OS application for direct AirPods control. The watch communicates with AirPods directly; a phone companion is not required for normal operation.

## Architecture target

```text
Wear UI
   |
AirPodsController
   |
State / Commands
   |
Wear Bluetooth transport
   |
ATT / BLE / AACP protocol core
   |
AirPods
```

## Completed

- Removed Linux product code from the Wear branch.
- Removed phone-only boot, media, gesture, sensor, billing and root/Xposed plumbing.
- Added `AirPodsController` as the Wear-facing core boundary.
- Added `AirPodsState` / `AirPodsStateStore`.
- Added typed `AirPodsCommand` boundary.
- Added `WearBluetoothScanner`.
- Added `WearBluetoothConnection` transport boundary.
- Added `LibrePodsWearService` lifecycle boundary.
- Kept the existing AACP/ATT/BLE implementations intact for incremental migration.
- Simplified the Wear Gradle target.

## Phase 1 — Finish autonomous core

- Audit AACP, ATT, BLE and connection dependencies.
- Replace phone-owned socket/service lifecycle with Wear-owned lifecycle.
- Implement AirPods discovery filtering.
- Implement direct connect/disconnect.
- Wire ATT notifications into AACP packet parsing.
- Map battery notifications to `AirPodsState`.
- Map listening modes to state and commands.
- Add ear detection and conversational awareness state/commands.
- Implement reconnect/backoff and connection-loss recovery.
- Migrate required logic out of legacy `AirPodsService`.
- Delete legacy `AirPodsService` only after migration is complete.

## Phase 2 — First controls

- AirPods identification.
- Left/right/case battery.
- Connection state.
- ANC / Transparency / Off.
- Ear detection.

## Phase 3 — Advanced protocol

- Conversational Awareness.
- Head gestures where supported.
- Press/hold configuration.
- Custom EQ/accessibility controls where supported.
- Rename/configuration.
- Additional state synchronization.

## Phase 4 — UI

- Main AirPods screen.
- Round/rectangular layouts.
- Large touch targets.
- Fast connection screen.
- Tile for listening-mode control.
- Optional complication.
- Battery-efficient background operation.

## Phase 5 — Compatibility

- Direct connection without phone.
- Reconnect after Bluetooth toggle, watch restart, AirPods case open/close and range loss.
- Samsung Galaxy Watch hardware first.
- Wear OS 3/4/5/6 where practical.
- Safe protocol logging without exposing private keys.

## Future

- Extract reusable protocol core for Android, Wear OS, Linux and Windows where practical.
- Build a native LibrePods Windows client after the Wear OS port is stable.

## Development rules

- Code comments: English.
- Roadmap and architecture documentation: English.
- Focused commits for cleanup and architecture changes.
- Never delete protocol code without checking dependencies first.
- Do not mix protocol changes with large UI refactors.
