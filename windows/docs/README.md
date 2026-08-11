# LibrePods — Windows docs

Docs are grouped **by operating system**, because the Windows stack shares
one codebase but each OS has its own integration story, drivers, and gotchas.

## [`windows/`](windows/) 🪟
The Windows port (the bulk of the porting effort — Linux already worked).
- **[`windows/HANDOFF.md`](windows/HANDOFF.md)** — the technical handoff/log:
  daemon + IPC architecture, drivers, hi-res mic, features, TODOs, and the
  reverse-engineering notes.
- **[`windows/daemon-ipc/PLAN.md`](windows/daemon-ipc/PLAN.md)** — the daemon +
  named-pipe IPC design (why `librepodsd` owns the driver and the UIs are clients).
- **[`windows/hires-mic/PLAN.md`](windows/hires-mic/PLAN.md)** — the AAC-ELD
  virtual-microphone plan (protocol, decode, driver).

## Linux 🐧
Linux is the original desktop target and needs no port-specific driver docs — it
uses BlueZ/PulseAudio directly. See the **[`linux/` README](../../linux/README.md)**
at the repo root for setup and usage.

## Android 🤖
See the repo-root **[README](../../README.md)** (Android section) and the Android
app sources.

## Protocol (OS-agnostic)
The AAP/AACP protocol notes live at the repo root and apply to every platform:
**[`AAP Definitions.md`](../../AAP%20Definitions.md)**,
**[`docs/control_commands.md`](../../docs/control_commands.md)**,
**[`Proximity Pairing Message.md`](../../Proximity%20Pairing%20Message.md)**.
