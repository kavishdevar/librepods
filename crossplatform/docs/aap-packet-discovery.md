# AAP Packet Discovery — field guide (macOS / iOS)

**Goal:** capture the *ground-truth* AirPods AAP protocol traffic so we stop guessing.
We reverse-engineer AAP (the AirPods control protocol over classic-Bluetooth
L2CAP **PSM `0x1001`**) from the Android app and community PRs. macOS talks AAP
**natively**, so sniffing its Bluetooth traffic shows us the **exact bytes Apple
sends** — for confirming what we know and discovering what we don't.

**What this best unblocks**
1. 🫀 **Heart-rate enable sequence** (the stubborn one — capture on *iOS*, see Part B).
2. 🔋 **Charging bit** in the battery packet (confirm `0x01`/`0x05`/`0x02`).
3. ❓ Any **unknown opcodes** not yet in `AAP Definitions.md`.

> This guide lives in the repo on purpose — do the git import on the Mac, open
> Claude Code there, and point it at this file. Everything below is runnable on
> the Mac.

---

## 0. What you need

- **This repo** (git import done ✅).
- A **Mac** with Bluetooth + your AirPods paired.
- **PacketLogger** — ships in Apple's *Additional Tools for Xcode*:
  [developer.apple.com/download/all](https://developer.apple.com/download/all/) →
  search **"Additional Tools for Xcode"** → download the DMG → `PacketLogger.app`
  is inside the **Hardware/** folder. (Free Apple ID is enough.)
- **(HR only)** an **iPhone** + Apple's **Bluetooth logging profile** (Part B).
- **Optional but handy:** Wireshark/tshark to parse `.pklg` — `brew install wireshark`.

---

## 1. The method: one action, one diff

The whole trick is **isolation**. Capture with a clean log, do **one** action,
stop, and see which bytes appeared/changed. That single-action delta *is* the
opcode for that action. Never toggle three things at once — you won't know which
bytes belong to which.

Keep a scratch note like:

```
t+3.2s  toggled ANC  Off → Noise Cancellation
t+7.8s  put left bud in ear
t+12.0s put both in case, closed lid
```

Then line the timestamps up against the capture.

---

## 2. Part A — capture macOS ↔ AirPods

Covers **battery, charging, ANC, ear-detection, Conversational Awareness,
Adaptive Volume, features**.

1. Open **PacketLogger** → it starts a live capture (or `File → New → Live Capture`).
2. `Edit → Clear` to zero it.
3. Do **one action at a time**, noting each timestamp:
   - Connect the AirPods.
   - Cycle **ANC**: Off → Noise Cancellation → Transparency → Adaptive (one step at a time).
   - **Ear detection**: take one bud out, put it back.
   - **Case**: put both in the case, then **open the lid**, then close it — watch for the **charging** transition (this is the `0x01`/`0x05` bit we want to confirm).
   - In *System Settings → your AirPods*: toggle **Conversational Awareness**, **Adaptive/Personalized Volume**.
4. `File → Save…` → `airpods-macos.pklg`.

---

## 3. Part B — capture iOS ↔ AirPods (the HR sequence)

macOS almost certainly **doesn't** drive heart-rate — the iPhone's Fitness /
Workout flow does. So HR must be captured on the **iPhone**:

1. On the iPhone, install Apple's **Bluetooth** logging profile:
   [developer.apple.com/bug-reporting/profiles-and-logs](https://developer.apple.com/bug-reporting/profiles-and-logs/)
   → **Bluetooth** → install. It lands in **Settings → General → VPN & Device Management**.
2. **Reboot** the iPhone (logging starts on boot).
3. Connect the **AirPods Pro 3** to the iPhone, wear them, open **Fitness** and start
   a **Workout** that reads heart rate. Let it read for ~30 s.
4. Trigger a **sysdiagnose**: press **Vol-Up + Vol-Down + Side** together briefly (feel a short buzz). Wait ~5 min while it builds.
5. Retrieve it: **Settings → Privacy & Security → Analytics & Improvements →
   Analytics Data → `sysdiagnose_…`** → share/**AirDrop** to the Mac.
6. Inside the sysdiagnose `.tar.gz` the Bluetooth trace is a **PacketLogger `.pklg`**
   (under `bluetooth/` or `system_logs`). That's your HR capture.

> Alternative: some Xcode/PacketLogger versions can capture a **connected iOS
> device** directly (`PacketLogger → File → New iOS Trace`). If yours offers it,
> skip the sysdiagnose dance.

---

## 4. Part C — read & filter

AAP rides L2CAP SDUs on **PSM `0x1001`**. Framing cheatsheet (from our code —
`crossplatform/windows/daemon/src/aap.rs`):

| Direction | Looks like | Meaning |
|---|---|---|
| host → device | `04 00 04 00 09 00 <id> <val> 00 00 00` | control command (ANC `id=0D`, CA `28`, AdaptiveVol `26`, AllowOff `34`, AdaptiveNoise `2E`, HRM `30`) |
| device → host | `04 00 04 00 04 …` | battery (status byte per component: `01`=charging, `02`=not, `04`=disconnected, **`05`=charging in case**) |
| device → host | `04 00 04 00 06 <a> <b> …` | ear detection (`00`=in-ear, `02`=in-case, `03`=disconnected) |
| device → host | `04 00 04 00 4B … <s>` | Conversational Awareness event |
| either | `04 00 04 00 17 00 …` (RTBuddy) | **heart-rate** frames (SensorDataWX) |

**Parse it two ways:**

- **tshark** (quick grep):
  ```bash
  tshark -r airpods-macos.pklg -Y btl2cap \
         -T fields -e frame.time_relative -e btl2cap.cid -e data \
    | grep -Ei '0400 0400|04000400'
  ```
- **Claude on the Mac** (richer): open the `.pklg` in **Wireshark**, then
  `File → Export Packet Dissections → As JSON` and hand the JSON to Claude to
  parse + diff. Or use the helper below.

---

## 5. Part D — the helper parser

`crossplatform/docs/aap_extract.py` — feed it a text/hex export (from tshark, a
Wireshark "Export as plain text", or a copy-paste hex dump) and it pulls out the
AAP-looking packets and annotates the opcode:

```bash
tshark -r airpods-macos.pklg -Y btl2cap -T fields -e frame.time_relative -e data \
  | python3 crossplatform/docs/aap_extract.py
```

It's deliberately simple/forgiving about input format — adapt it on the Mac with
Claude if your export looks different.

---

## 6. Part E — what to hunt (priorities)

1. **HR enable sequence** (iOS capture) — the exact frames the iPhone sends
   *before* the `17 00 …` RTBuddy stream begins. Diff against our
   `HR_CONNECT_SERVICE_*`, `HR_ENABLE`, `HR_START` in `aap.rs`. This is the one
   that would let us finally make `set_heart_rate` work.
2. **Charging bit** — in a `…04` battery packet, watch the per-component status
   byte flip when a bud is charging in the (open) case. Confirms `01` vs `05`.
3. **Unknown opcodes** — anything with header `04 00 04 00` and an opcode not in
   the table above / `AAP Definitions.md`.

---

## 7. Part F — bring it back

- Protocol findings → append to **`AAP Definitions.md`** (the protocol doc).
- HR frames → **`crossplatform/windows/daemon/src/aap.rs`** (the `HR_*` consts).
- **Keep the per-action diff notes** — "these bytes changed when I did X" is the
  most valuable artifact; paste them into the PR / commit message so the
  provenance is clear.

Happy hunting. 🎯
