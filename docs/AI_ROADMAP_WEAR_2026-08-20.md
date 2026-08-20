# LibrePods-Wear AI Roadmap — 2026-08-20

## Goal
Build a fully autonomous Wear OS LibrePods implementation. The watch owns the AirPods control connection directly; the phone is not required for the protocol stack.

## Current architecture

```text
Wear UI
  ↓
AirPodsController
  ├── AirPodsState / StateFlow
  ├── BLEManager              → advertisement/status data
  └── AACPManager             → AirPods protocol
          ↓
    WearBluetoothConnection
          ↓
      L2CAP / AACP
          ↓
        AirPods
```

## Completed

- [x] Wear-first cleanup.
- [x] WearBluetoothConnection as protocol transport.
- [x] AirPodsConnectionSession introduced.
- [x] AirPodsController owns connection orchestration.
- [x] Old phone-side Bluetooth manager removed from the active Wear path.
- [x] BLE status monitoring for battery, lid and ear state.
- [x] AACP session state machine.
- [x] Strict handshake: HANDSHAKE → HANDSHAKE ACK → SET_FEATURE_FLAGS → FEATURES ACK → REQUEST_NOTIFICATIONS.
- [x] AACP reader starts before the handshake.

## Next: first real protocol milestone

1. [ ] Validate handshake transitions on physical AirPods.
2. [ ] Add robust L2CAP packet framing/deframing; one InputStream.read() is not guaranteed to equal one protocol packet.
3. [ ] Decode BATTERY_INFO into left/right/case state.
4. [ ] Decode charging flags and case/lid state.
5. [ ] Decode EAR_DETECTION.
6. [ ] Merge AACP and BLE telemetry into one authoritative AirPodsState.
7. [ ] Add connection timeout and protocol diagnostics.
8. [ ] Add reconnect with stale-reader/session protection.

## Control layer

- [ ] Listening mode: Off / ANC / Transparency / Adaptive.
- [ ] Ear detection configuration.
- [ ] Stem/button configuration.
- [ ] Conversation Awareness.
- [ ] Rename.
- [ ] Device information / firmware metadata.
- [ ] Multi-device / ownership switching.

## Wear UI

- [ ] Connection screen with explicit protocol state.
- [ ] Battery card: L / R / Case.
- [ ] Charging and lid indicators.
- [ ] Listening mode control.
- [ ] Ear detection status.
- [ ] Error/diagnostic screen.
- [ ] Reconnect/disconnect actions.

## Reliability

- [ ] Survive screen-off and process lifecycle correctly.
- [ ] Use foreground/ongoing service only where required by Wear OS.
- [ ] Prevent duplicate AACP sessions.
- [ ] Serialize L2CAP writes.
- [ ] Debug protocol logging.
- [ ] Unit tests with captured packets.

## Later

- [ ] Audio-routing / ownership switch between phone and watch.
- [ ] Optional companion phone integration.
- [ ] Advanced AirPods features.
- [ ] Performance and battery optimization.

## Development rule

Every meaningful architecture/protocol change updates this roadmap and receives a new commit SHA. Source-code comments stay in English.
