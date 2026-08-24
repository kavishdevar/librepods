# LibrePods-Wear — Wear protocol roadmap (2026-08-23)

## Architecture decision

- System Bluetooth pairing/discovery is the authority; the app does not implement a competing pairing UI.
- Wear app works with already paired AirPods and owns the connection/protocol layer after selection.
- All connection state, AACP transport, protocol parsing and diagnostics are being moved into the Wear stack.
- The UI stays compact and scrollable; protocol diagnostics are secondary to the actual connection/status data.

## Current milestone

- System Bluetooth pairing flow is used to get AirPods onto the watch.
- Wear app selects an already paired AirPods device.
- Direct AACP classic L2CAP transport is owned by `AirPodsConnectionSession` / `WearBluetoothConnection`.
- AACP handshake is stateful: `IDLE -> HANDSHAKE_SENT -> FEATURES_SENT -> READY`.
- AACP packet callbacks are wired into `AirPodsController` using the actual `AACPManager.Companion` nested packet types.
- Battery packets are validated and decoded for left/right/case.
- Ear-detection packets are validated and decoded.
- Raw/unknown packets are retained in controller diagnostics for reverse-engineering.
- BLE advertisements remain a secondary status source for battery/ear/case data.
- Callback type mismatch in `AirPodsController` is fixed against the current AACP API.

## Protocol work queue

1. **AACP stream framing** — make fragmented/coalesced L2CAP reads safe using known frame layouts; never guess unknown lengths.
2. **Device information** — decode metadata and expose model/name/firmware/serial fields where the packet layout is verified.
3. **Battery/status** — finish stable left/right/case percentage + charging + connected state handling.
4. **Ear detection** — finish stable left/right in-ear state and merge it with BLE status without stale overwrites.
5. **Listening mode** — implement verified ANC / Transparency / Off read/write commands.
6. **Conversation Awareness** — implement verified read/write commands.
7. **Stem/button control** — implement verified command/status handling.
8. **Connected devices / ownership** — decode and expose ownership and connected-device state.
9. **Reconnect** — bounded backoff, socket reset and clean AACP session restart.
10. **ATT** — add only after AACP status is stable; do not make ATT a prerequisite for the first working connection.
11. **Protocol test vectors** — add captured packet fixtures; no guessed packet layouts.
12. **Wear UI** — keep the compact connection/status view and expose protocol stage + last opcode/hex only for debugging.

## Next implementation block

- Verify the callback-type fix with a clean Wear build.
- Then implement robust AACP stream framing so one `read()` cannot be incorrectly treated as exactly one protocol packet.
- Preserve raw frames for diagnostics while only decoding layouts that are verified.
- After framing is stable, move to device-information and battery/status completion.

## Acceptance target

`Paired AirPods -> Connect -> AACP READY -> left/right/case percentages -> charging flags -> left/right in-ear state -> clean disconnect/reconnect`.

After that, expand the verified AACP control surface before touching secondary ATT/Find My functionality.
