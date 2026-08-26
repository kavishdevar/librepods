# LibrePods-Wear — AI Roadmap 2026-08-20

## Goal

Build a fully autonomous Wear OS LibrePods client. The watch owns the AirPods connection, AACP/ATT protocol stack, BLE status processing, state model, and UI. No phone-side LibrePods service is required for normal operation.

## Current architecture

- `AirPodsController` — Wear-facing orchestration and state lifecycle.
- `AirPodsState` / `AirPodsStateStore` — single observable device state.
- `WearBluetoothConnection` — Wear transport facade.
- `AirPodsConnectionSession` — Classic Bluetooth L2CAP/ATT session ownership.
- `AACPManager` — protocol packet engine.
- `BLEManager` — passive AirPods BLE status monitoring.

## Phase 1 — Build and connection foundation

- [x] Remove obsolete phone-only architecture from the Wear path.
- [x] Make AACP transport explicitly Wear-owned.
- [x] Restore a clean debug build.
- [x] Connect to a bonded AirPods device from Wear OS.
- [ ] Implement the complete AACP handshake sequence: handshake ACK -> feature flags -> notification subscription.
- [ ] Add robust AACP stream/frame handling.
- [ ] Add connection timeout and deterministic disconnect/reconnect handling.
- [ ] Add protocol logging suitable for real-device debugging.

## Phase 2 — Device status

- [ ] Parse AACP battery packets for Left / Right / Case.
- [ ] Parse AACP ear-detection packets.
- [ ] Merge AACP and BLE status into `AirPodsState` without stale-value regressions.
- [ ] Expose charging and case-lid state.
- [ ] Show first reliable Connected + L/R/Case status in the Wear UI.

## Phase 3 — Controls

- [ ] Listening mode: ANC / Transparency / Off.
- [ ] Ear detection configuration.
- [ ] Conversation Awareness.
- [ ] One-bud ANC.
- [ ] Stem/control configuration.
- [ ] Rename and device information.

## Phase 4 — Reliability

- [ ] Automatic reconnect after Bluetooth loss.
- [ ] Handle AirPods switching between bonded/active devices.
- [ ] Prevent duplicate AACP sessions.
- [ ] Keep BLE monitoring alive independently from AACP.
- [ ] Persist the last known AirPods identity safely.

## Phase 5 — Wear UX

- [ ] Compact connection screen.
- [ ] Battery cards for L/R/Case.
- [ ] Listening mode control.
- [ ] Connection diagnostics screen.
- [ ] No unnecessary screen-off during pairing/connection flows.
- [ ] Minimize phone dependency and background work.

## Phase 6 — Advanced LibrePods features

- [ ] Proximity keys / encrypted BLE status.
- [ ] Advanced AirPods information.
- [ ] Custom EQ / accommodation.
- [ ] Head tracking where technically useful on Wear OS.
- [ ] Further LibrePods feature parity after the core stack is stable.

## Rules for this port

1. Prefer the existing LibrePods protocol implementation over inventing new packet formats.
2. Keep protocol logic independent from Compose/UI.
3. Keep comments and development-map entries in English.
4. Every meaningful architecture change gets a small commit.
5. Update this roadmap whenever a phase materially changes.
6. A build passing is not considered a connection milestone; real AACP packet exchange is.
