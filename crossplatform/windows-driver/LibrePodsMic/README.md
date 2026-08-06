# LibrePodsMic — virtual audio (microphone) driver

The virtual-microphone driver for the [hi-res mic feature](../../hires-mic/PLAN.md):
Windows sees a "LibrePods Microphone" that the app feeds with the AirPods'
decoded AAC-ELD audio, so any app (Teams, Zoom, Discord, OBS…) can use it.

## Origin / license

This is based on the **Microsoft ACX `AudioCodec` sample** from
[microsoft/Windows-driver-samples](https://github.com/microsoft/Windows-driver-samples/tree/main/audio/Acx/Samples)
(**MIT licensed**), chosen because:

- it's a **ROOT-enumerated** (software, no-hardware) audio device — i.e. virtual;
- it already has a **capture circuit** (`Common/CaptureCircuit.cpp`) — a mic;
- it uses **ACX** on top of **KMDF** — the same framework family as our
  `LibrePodsAAP` driver (unlike SYSVAD's older PortCls).

We will trim it to capture-only and swap the audio source (`Common/WaveReader.cpp`)
for a PCM feed pushed from the app over an IOCTL/ring buffer.

## Status

- [x] **Builds** with VS2026 + WDK 28000 (ACX headers present) — `AudioCodec.sys`.
- [ ] Install (ROOT device) + verify a virtual mic appears in Sound settings.
- [ ] Rename AudioCodec → LibrePodsMic (INF device description).
- [ ] Trim to capture-only; add the IOCTL PCM bridge (Phase 2).

## Build

`build-wsl.cmd` on the Windows host (VS + WDK) →
`AudioCodec/Driver/x64/Release/AudioCodec.sys`. See
[`../../hires-mic/PLAN.md`](../../hires-mic/PLAN.md) for the roadmap.
