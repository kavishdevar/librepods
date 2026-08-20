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
State / Commands / Events
   |
AirPodsConnectionSession
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
- Added `WearBluetoothConnection` facade.
- Added `AirPodsConnectionSession` with owned AACP/ATT socket lifecycle.
- Added bounded `AirPodsReconnectManager` with exponential backoff.
- Added `LibrePodsWearService` and moved transport ownership into the service.
- Initialized AACP/BLE managers inside the Wear service.
- Kept the existing ATT/AACP/BLE protocol implementation intact while migrating lifecycle ownership.
- Kept UI intentionally thin while the core is being stabilized.

## Phase 1 — autonomous transport/core — IN PROGRESS

- Replace global `BluetoothConnectionManager` socket access in ATT/AACP.
- Route ATT reads/writes through the connection session.
- Route AACP reads/writes through the connection session.
- Implement AirPods protocol discovery and candidate selection.
- Implement direct L2CAP connect using protocol UUID/PSM values.
- Emit connection events from the session.
- Attach ATT reader lifecycle to session start/stop.
- Attach AACP reader lifecycle to session start/stop.
- Handle connection loss and invoke bounded reconnect.
- Remove remaining phone-owned connection lifecycle.

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
