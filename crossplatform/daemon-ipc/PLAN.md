# LibrePods Windows daemon + IPC — plan

**Goal:** kill the exclusive-driver tug-of-war. Today both `librepods-tray.exe`
and `librepods.exe` want the single, **exclusive** driver handle, so only one can
run — hence the fragile "Open App" handoff, lingering daemon/zombie processes,
and duplicated AAP code in two binaries.

**Fix (the Gemini architecture):** one headless **daemon owns the driver + the
AAP session + the mic pipeline**; the tray and the full app become thin **IPC
clients**. They can run **at the same time**, nobody fights over the handle, and
battery/mic keep working even with no UI open.

```
                 ┌─ librepods-tray.exe   (UI client, light)
librepodsd.exe ──┤       IPC: \\.\pipe\LibrePods  (NDJSON)
(owns the driver)└─ librepods.exe        (UI client, iced GUI)
```

## Components

- **`librepodsd`** (new, `windows-app/librepodsd/`) — headless. Owns the driver
  handle + AAP session (today's tray `run_receiver`), the mic pipeline (decode
  AAC-ELD → feed `\\.\LibrePodsMic`), the auto-activate poll + A2DP reset, and
  the dynamic-rename trigger. Holds the **authoritative state**. Runs an IPC
  server. Single-instance (named mutex).
- **`librepods-ipc`** (new lib crate) — the shared `Command` / `Event` serde
  types + a tiny NDJSON framing helper, so daemon and clients agree on the wire.
- **`librepods-tray`** (refactored) — pure UI client: connects to the pipe
  (spawns the daemon if absent), renders the icon/menu/overlay from daemon
  events, sends commands. Its `driver`/`aap`/`eld`/`micpipe`/`a2dp` modules
  **move into the daemon**.
- **`librepods.exe`** (crossplatform-rust, Windows) — becomes an IPC client too
  (Phase 3): its `platform/windows` backend talks to the daemon instead of the
  driver directly. On **Linux nothing changes** (still `bluer`, no daemon).

## IPC protocol

- **Transport:** Windows **named pipe** `\\.\pipe\LibrePods`, duplex, message
  mode, **multi-instance** (one pipe instance per connected client). Access
  restricted to the current user.
- **Framing:** newline-delimited JSON (NDJSON) — one `serde_json` value per line.
- **Client → Daemon `Command`:**
  - `Hello { kind: "tray" | "app" }` — sent on connect; daemon replies with a
    full `State` snapshot.
  - `SetAnc(u8)` (1..=4)
  - `SetMicMode { auto: bool, manual: bool }`
  - `GetState`
  - (volume stays **client-side** via WASAPI — it's not the exclusive resource,
    so no need to route it through the daemon.)
- **Daemon → Client `Event`:**
  - `State(Snapshot { connected, battery{l,r,case,headphone}, anc, dev_name,
    mic_recording, auto_mode })` — pushed on every change, and once on connect.
  - `Overlay { title, body }` — a notification for the client to render (the
    daemon decides *when*; the tray/app draw it with their overlay UI).

## Lifecycle

- **Start:** the tray autostarts at login (as now) and **ensures the daemon is
  running** — if the pipe isn't there, it spawns `librepodsd.exe`, then connects.
  Client-spawns-daemon = no separate autostart entry, robust.
- **Single-instance:** the daemon holds a named mutex; a second spawn exits.
- **Death/restart:** a client that sees the pipe drop retries/reconnects (and
  re-spawns the daemon if needed). The daemon keeps running when UIs close.
- **Shutdown:** closing a UI just disconnects its pipe; the daemon lives on.
  (A tray "Quit LibrePods" can send a `Shutdown` that stops the daemon too.)

## Migration — incremental, nothing breaks between phases

1. **Daemon core.** New `librepodsd` + `librepods-ipc`. Move `run_receiver` +
   the auto-activate poll + the mic pipeline + `State` out of the tray into the
   daemon. Add the NDJSON named-pipe server; broadcast `State`/`Overlay`. Test
   the daemon standalone (it logs; the mic + battery work with no UI).
2. **Tray → client.** Strip the tray's driver/aap/eld/micpipe/a2dp; it connects
   to the daemon, renders from `Event`s, sends `Command`s, spawns the daemon if
   absent. **This alone ends the handle conflict for the tray** and deletes the
   "Open App handoff" hack.
3. **App → client.** Route crossplatform-rust's `platform/windows` L2CAP/session
   backend through the daemon IPC. Now tray + full app coexist. (Heaviest phase —
   touches the shared cross-platform code; Linux path untouched.)
4. **Polish.** Daemon single-instance + autostart-on-demand + reconnect; installer
   ships `librepodsd.exe` and drops the exclusive-handoff shortcut logic.

## Status — Phases 1–2 DONE ✅ (validated on hardware)

- **`librepodsd`** owns the driver + AAP session + hi-res mic; the **tray is a
  thin IPC client** and they coexist. Confirmed on hardware: battery / ANC /
  volume / mic all shown + controlled over IPC, auto-reconnect, overlay cards.
- **IPC = two one-directional named pipes** (`PIPE_EVENTS` daemon→client,
  `PIPE_CMDS` client→daemon), NDJSON, **async** (per-connection queue + writer
  thread each side). This was forced by two bugs hit on the way:
  1. **Sync-handle serialization / deadlock** — a single *duplex* pipe deadlocked:
     a Windows synchronous handle serializes I/O, so the daemon's blocking
     ReadFile (commands) stalled its WriteFile (events) on the same handle (it
     wrote 2 messages then hung). Split into two one-directional pipes.
  2. **UI freeze** — a synchronous blocking WriteFile on the tray's UI thread
     froze the menu. Both sides now decouple I/O onto their own threads.
  - Also: the named-pipe DACL must grant the same-user client (`D:(A;;GA;;;AU)…`);
    the default null descriptor denied it.
- **BLE 'connect?' prompt** (beyond the plan): `le.rs` watches (passive, and only
  while disconnected) for the AirPods proximity advertisement and sends
  `Event::ConnectPrompt`; the tray shows a clickable, accent-themed overlay card;
  a click → `Command::Connect`.
- **Never-steal policy**: the daemon never auto-connects — it waits for the
  prompt/Connect, and **releases on any drop** (never reconnects on its own,
  which would steal the AirPods back from the iPhone). Connect retries ~18 s for
  resilience.
- **Phase 3 (full app as a client)** — NOT done; the app still uses the
  Open-App handoff. The daemon's own session vs the app's session over one L2CAP
  channel is the open design question (state-client vs raw-L2CAP-proxy).

## Risks / notes

- **Multi-client pipe:** the daemon must serve several pipe instances at once
  (tray + app) and broadcast to all — one reader thread per client + a shared
  broadcast channel (or a small async runtime).
- **Mic ownership:** the daemon becomes the single owner of both the driver and
  `\\.\LibrePodsMic` — cleaner than today (the tray owned them).
- **Phase 3 is the big one** (shared code); Phases 1–2 already remove the pain
  and can ship on their own.
- **Shared modules:** move `aap`/`driver`/`eld`/`micpipe`/`a2dp` into the daemon
  (or a small `librepods-win-core` lib if the app later needs them in-proc too).
