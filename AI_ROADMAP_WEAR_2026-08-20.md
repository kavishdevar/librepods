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
AirPodsProtocolTransport
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
- Added `AirPodsProtocolTransport` so protocol code can consume streams without owning Android Bluetooth sockets.
- Added `AirPodsConnectionTarget` for negotiated UUID/PSM transport parameters.
- Added bounded `AirPodsReconnectManager` with exponential backoff using the negotiated target.
- Added `LibrePodsWearService` and moved transport ownership into the service.
- Converted `BluetoothConnectionManager` into a deprecated compatibility facade bound to the owned session.
- Injected `AirPodsProtocolTransport` into `ATTManager`; ATT no longer owns or looks up a global socket.
- Added `AACPTransportBridge` for transitional packet writes/dispatch against the Wear-owned transport.
- Initialized AACP/BLE managers inside the Wear service.
- Kept existing AACP/BLE parsing and command logic intact while migrating lifecycle ownership.
- Added strict AACP handshake sequencing: handshake -> ACK -> feature flags -> feature ACK -> notification request.
- Started the AACP reader before the handshake write so a fast AirPods ACK cannot be lost.
- Reverted an unverified packet-framing abstraction rather than guessing AACP framing rules.
- Added a diagnostic Wear UI showing connection state and Left/Right/Case battery values.
- Treat battery value `255` as unknown in the UI instead of displaying an invalid percentage.
- Made Wear BLE discovery unfiltered and observable.
- Added BLE callback/error diagnostics to the Wear UI.
- Added Apple manufacturer detection and advertised service metadata to discovered devices.
- Added compact multi-device selection UI for Wear.

## Phase 1 — autonomous transport/core — IN PROGRESS

- Migrate `AACPManager.sendPacket()` from the compatibility facade to `AirPodsProtocolTransport`.
- Add an AACP reader loop owned by the connection session/controller.
- Route AACP received packets into `AACPManager.receivePacket()`.
- Resolve model-compatible L2CAP UUID/PSM parameters from existing discovery logic.
- Connect ATT reader lifecycle to session start/stop.
- Connect AACP reader lifecycle to session start/stop.
- Emit connection/protocol events from the session.
- Handle connection loss and invoke bounded reconnect.
- Remove `BluetoothConnectionManager` after AACP migration is complete.
- Remove remaining phone-owned connection lifecycle.

## Phase 2 — state and controls

- Parse L2CAP battery notifications into `AirPodsState` — NEXT.
- Verify component mapping: Right=`0x02`, Left=`0x04`, Case=`0x08`.
- Map battery charging/disconnected status into the state model.
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

- Diagnostic connection + battery screen — DONE.
- BLE scan and device selection — DONE.
- Main AirPods screen.
- ANC / Transparency / Off.
- Ear detection.
- Conversational awareness.
- Connection/reconnect screen.
- Tile and optional complication.

## Immediate next test

1. Build the Wear APK.
2. Install on the watch.
3. Put AirPods into pairing/discovery mode.
4. Tap `Scan`.
5. Confirm `callbacks > 0`.
6. Confirm at least one device appears.
7. If the list is empty, report the on-screen callback count and scan error.
8. Tap the AirPods row to start the Wear-owned connection path.

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
- Do not invent protocol framing, UUIDs or PSM values; extract them from the existing implementation or protocol discovery.
