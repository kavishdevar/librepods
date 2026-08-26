# LibrePods-Wear — Wear protocol roadmap (2026-08-25)

## Architecture decision

- System Bluetooth pairing/discovery is the authority; the app does not implement a competing pairing UI.
- Wear app works with already paired AirPods and owns the connection/protocol layer after selection.
- All connection state, AACP transport, protocol parsing and diagnostics live in the Wear stack.
- The UI stays compact and scrollable; protocol diagnostics are secondary to actual connection/status data.

## Current milestone

- System Bluetooth pairing flow is used to get AirPods onto the watch.
- Wear app selects an already paired AirPods device.
- Direct AACP classic L2CAP transport is owned by `AirPodsConnectionSession` / `WearBluetoothConnection`.
- Modern Android `BluetoothDevice.createL2capChannel(0x1001)` is preferred, with the legacy constructor path retained as fallback.
- AACP handshake is stateful: `IDLE -> HANDSHAKE_SENT -> FEATURES_SENT -> READY`.
- AACP packet callbacks are wired into `AirPodsController` using the actual `AACPManager.Companion` nested packet types.
- Battery packets are validated and decoded for left/right/case.
- Ear-detection packets are validated and decoded.
- Raw/unknown packets are retained in controller diagnostics for reverse-engineering.
- BLE advertisements remain a secondary status source for battery/ear/case data.
- Verified control commands are now exposed for listening mode, ear detection and conversational awareness.
- Bounded reconnect (3 attempts) is now part of the Wear controller.

## Protocol work queue

1. **AACP stream framing** — finish conservative framing for fragmented/coalesced reads. Only emit frames with a verified layout; never guess unknown lengths.
2. **Handshake framing** — make the handshake ACK and feature ACK survive split/coalesced reads without relying on Bluetooth `read()` boundaries.
3. **Device information** — decode metadata and expose model/name/firmware/serial fields where the packet layout is verified.
4. **Battery/status** — finish stable left/right/case percentage + charging + connected state handling.
5. **Ear detection** — finish stable left/right in-ear state and merge it with BLE status without stale overwrites.
6. **Listening mode** — verified write commands are implemented: Off=1, ANC=2, Transparency=3. Add verified read/status parsing.
7. **Conversation Awareness** — verified enable/disable write is implemented. Add verified status parsing.
8. **Stem/button control** — implement verified command/status handling.
9. **Connected devices / ownership** — decode and expose ownership and connected-device state.
10. **Reconnect** — bounded backoff, socket reset and clean AACP session restart are implemented; validate on hardware.
11. **ATT** — add only after AACP status is stable; do not make ATT a prerequisite for the first working connection.
12. **Protocol test vectors** — add captured packet fixtures; no guessed packet layouts.
13. **Wear UI** — keep the compact connection/status view and expose protocol stage + last opcode/hex only for debugging.

## Current implementation block

- Prefer the public Android L2CAP channel API for PSM `0x1001`.
- Add verified AACP control writes for listening mode, ear detection and conversation awareness.
- Add bounded reconnect after an unexpected AACP socket close.
- Keep unknown packet handling conservative until verified frame lengths are available.

## Acceptance target

`Paired AirPods -> Connect -> AACP READY -> left/right/case percentages -> charging flags -> left/right in-ear state -> verified ANC/Transparency/Off control -> clean disconnect/reconnect`.

After that, expand the verified AACP control surface before touching secondary ATT/Find My functionality.
