# LibrePods-Wear — Wear protocol roadmap (2026-08-21)

## Current milestone

- System Bluetooth pairing is the discovery/pairing authority.
- Wear app selects an already paired AirPods device.
- Direct AACP L2CAP transport is owned by the Wear stack.
- AACP handshake is stateful: `IDLE -> HANDSHAKE_SENT -> FEATURES_SENT -> READY`.
- AACP packet callbacks are wired into `AirPodsController`.
- Battery packets are validated and decoded for left/right/case.
- Ear-detection packets are validated and decoded.
- Raw/unknown packets are retained in controller diagnostics for reverse-engineering.
- BLE advertisements remain a secondary status source for battery/ear/case data.

## Next protocol work

1. Make AACP stream framing robust against fragmented/coalesced L2CAP reads.
2. Add metadata/device-information decoding to Wear state.
3. Add listening-mode read/write (ANC / Transparency / Off).
4. Add conversational-awareness read/write.
5. Add stem/button command handling.
6. Add connected-device / ownership state.
7. Add reconnect with bounded backoff and transport reset.
8. Add ATT/Find My channel only after AACP status is stable.
9. Add protocol test vectors from captured packets; no guessed layouts.
10. Expose protocol stage and packet diagnostics in the compact Wear UI.

## Acceptance target

A successful first protocol milestone is: paired AirPods -> Connect -> AACP READY -> left/right/case battery percentages -> charging flags -> left/right in-ear state -> clean disconnect/reconnect.
