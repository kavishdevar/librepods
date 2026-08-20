# LibrePods Wear — AI Roadmap

Date: 2026-08-20

## Goal

Build a fully autonomous Wear OS AirPods client. The watch communicates with AirPods directly; the phone is not required for normal operation.

## Current architecture

```text
Wear UI
   |
AirPodsController
   |
AirPodsState / AirPodsCommand / AirPodsEvent
   |
Wear Bluetooth transport
   |
ATT / BLE / AACP protocol
   |
AirPods
```

## Completed

- Removed Linux product code from the Wear branch.
- Removed phone-only boot, media, gesture, sensor, billing and root/Xposed plumbing.
- Added `AirPodsController` as the Wear-facing core boundary.
- Added observable `AirPodsState` / `AirPodsStateStore`.
- Added typed `AirPodsCommand` and `AirPodsEvent` boundaries.
- Added `WearBluetoothScanner`.
- Added `WearBluetoothConnection` transport boundary.
- Added `LibrePodsWearService` lifecycle boundary.
- Initialized AACP/BLE managers inside the Wear service.
- Kept the existing ATT/AACP/BLE protocol implementation intact while migrating lifecycle ownership.
- Kept UI intentionally thin while the core is being stabilized.

## Phase 1 — autonomous transport/core

- Audit all AACP/ATT/BLE assumptions about `BluetoothConnectionManager`.
- Replace global phone socket ownership with a Wear-owned connection session.
- Connect ATT reader to the Wear transport session.
- Connect AACP reader/writer to the same session.
- Implement AirPods discovery filtering and candidate selection.
- Implement direct L2CAP connect for AACP and ATT.
- Implement clean disconnect and resource ownership.
- Implement connection state events.
- Implement reconnect/backoff after range loss and Bluetooth restart.

## Phase 2 — state and controls

- Parse battery notifications into `AirPodsState`.
- Parse ear detection.
- Parse listening mode.
- Implement ANC / Transparency / Off commands.
- Implement ear detection command.
- Implement conversational awareness command/state.
- Refresh state after connection.

## Phase 3 — legacy service migration

- Move required callbacks and packet routing from legacy `AirPodsService`.
- Remove phone media, notification, telephony, widget, takeover and root-specific branches.
- Delete the legacy service only after all required protocol paths are migrated.

## Phase 4 — UI

- Main AirPods screen.
- Battery display.
- ANC / Transparency / Off.
- Ear detection.
- Conversational awareness.
- Device selection.
- Connection/reconnect screen.
- Tile and optional complication.

## Future

- Extract reusable protocol core for Android, Wear OS, Linux and Windows where practical.
- Build a native LibrePods Windows client after the Wear OS port is stable.

## Development rules

- Code comments: English.
- Roadmap and architecture documentation: English.
- Focused commits for cleanup and architecture changes.
- Never delete protocol code without checking dependencies first.
- Do not mix protocol changes with large UI refactors.
- The phone must never become a required runtime dependency of the Wear core.
