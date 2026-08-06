# Hi-res AirPods microphone on Windows — feature branch

**Branch:** `windows-hires-mic` (off `cross-platform`; merges back into it when done).

**Goal:** expose the AirPods' hi-res (AAC-ELD) microphone as a **native Windows
microphone**, so any app (Teams, Zoom, Discord, OBS…) can use it — the same
feature the Linux side is adding in
[PR #655](https://github.com/librepods-org/librepods/pull/655), but with a
self-contained **virtual audio driver** instead of PipeWire.

## Why a driver (not VB-Cable)

Windows has no API to "create a virtual microphone" — it needs a virtual audio
device driver. We already ship a signed kernel driver (`LibrePodsAAP`), so a
second one is the clean, dependency-free path (no third-party VB-Cable). Base it
on Microsoft's **SYSVAD** sample (a virtual audio device with capture + render
endpoints, no hardware). NOTE: audio drivers use **PortCls/AVStream** or the
newer **ACX** framework — different from the KMDF+BRB approach of `LibrePodsAAP`.

## Architecture

```
AirPods ──AAP / L2CAP──▶ LibrePodsAAP driver ──IOCTL──▶ app
                                                         │  decode AAC-ELD (FFmpeg / libavcodec)
                                                         │  → PCM
                                                         ▼
                                        LibrePodsMic virtual audio driver
                                                         │
                                                         ▼
                                   Windows sees a "LibrePods Microphone"
                                   (Teams / Zoom / Discord / OBS / …)
```

The protocol (enable-mic control command, uplink packet framing, AAC-ELD params)
is platform-neutral and comes from PR #655 — it lands in the shared
`crossplatform-rust` crate (`aacp`/`media_controller`), gated per platform for
the sink.

## Phases (incremental, each independently testable)

1. **Virtual-mic driver base** — build + test-sign + install SYSVAD (or a trimmed
   fork) so Windows shows a "LibrePods Microphone" capture endpoint. Prove it
   appears in Sound settings and apps. *(driver: `windows-driver/LibrePodsMic/`)*
2. **PCM bridge** — an IOCTL/shared-ring for user mode to push PCM samples into
   the driver; feed a test tone / a WAV → verify it's audible on the virtual mic
   (record in Voice Recorder / Audacity).
3. **Protocol port** — from PR #655: the AAP command that enables the hi-res mic,
   the uplink packet framing + watchdog, and AAC-ELD decoding via FFmpeg
   (libavcodec) — in the shared crate, `platform::MicSink` for the OS sink.
4. **Integration** — talk into the AirPods → decoded audio reaches the virtual
   mic. Conversation-awareness pause during capture, AGC toggle, settings
   persistence (mirror #655).

## Risks / open questions

- **Audio driver complexity** — WaveRT buffering, formats, timing. SYSVAD is a
  large sample; getting a stable capture endpoint is the bulk of the work.
- **AAC-ELD patents** — the decoder (FFmpeg) is patent-encumbered; distribution
  implications (PR #655 raises the same). Keep the decoder optional / documented.
- **Latency** — L2CAP → decode → driver ring; needs a small, steady buffer.
- **Test-signing** — same Test Mode requirement as the AAP driver.

## References

- Microsoft SYSVAD (virtual audio device sample), and the ACX audio samples.
- LibrePods PR #655 (Linux hi-res mic: AAC-ELD + PipeWire) — the protocol RE.
- `../windows-driver/LibrePodsAAP/` — our existing driver + build/sign/install loop.

## Status

Phase 1 not started. This branch holds the plan + (to come) the `LibrePodsMic`
driver and the shared-crate mic path.
