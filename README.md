# LibrePods Wear

> **Work in progress.** This repository is an experimental Wear OS port of [LibrePods](https://github.com/librepods-org/librepods).

LibrePods Wear is intended to control AirPods **directly from a Wear OS watch**. The watch must be able to discover, connect to and communicate with AirPods without requiring a phone companion for normal operation.

## Project direction

The port keeps the original LibrePods AirPods protocol implementation where it is useful and removes platform-specific parts that do not belong on a watch.

Target architecture:

```text
Wear OS UI
    |
Wear service / lifecycle
    |
Wear Bluetooth layer
    |
ATT / AACP protocol core
    |
AirPods
```

The phone is optional and is not part of the core control path.

## Current status

- [x] Fork LibrePods upstream
- [x] Add Wear OS development roadmap
- [x] Remove Linux application implementation
- [x] Remove root-module packaging
- [x] Remove native L2CAP/Xposed hook build from the Wear branch
- [x] Remove legacy phone UI and Quick Settings components
- [x] Reduce Android manifest to Wear/Bluetooth requirements
- [x] Create a minimal Wear launcher
- [ ] Isolate reusable AirPods protocol core
- [ ] Implement direct Wear OS Bluetooth connection
- [ ] AirPods discovery and pairing flow
- [ ] ATT/AACP connection
- [ ] Battery status
- [ ] Noise Control modes
- [ ] Ear detection
- [ ] Reconnect/background lifecycle
- [ ] Wear OS Tile
- [ ] Advanced LibrePods features

See [`AI_ROADMAP_WEAR_2026-08-20.md`](./AI_ROADMAP_WEAR_2026-08-20.md) for the detailed development map.

## Build

The current branch is a **development cleanup stage** and has not yet been validated on a physical Wear OS device. Build/test work starts after the protocol and Wear Bluetooth layers are isolated.

## Upstream

Original project: [librepods-org/librepods](https://github.com/librepods-org/librepods)

This fork remains GPL-3.0 licensed. Keep the original license and contributor attribution when modifying or redistributing the code.
