# Heart rate on Windows — why it doesn't work

**Short version:** AirPods Pro 3 will *accept* the heart-rate request on Windows
and *acknowledge* it, but they never send any readings. This is an **intentional
Apple-ecosystem restriction**, not a bug in LibrePods or in the Windows driver.
The feature is therefore **off by default** and hidden behind a warning in
**Settings ▸ Experimental**.

---

## What we send (byte-identical to the working clients)

The daemon drives the exact same AAP sequence, byte for byte, that the working
Android/iOS clients use — same opcodes, same constants, same ordering:

| Step | Frame |
|------|-------|
| connect service 0 | `00 00 00 00 01 00 03 …` |
| capabilities 0    | `04 00 00 00 01 00 00` |
| connect service 4 | `00 00 04 00 01 00 03 …` |
| capabilities 4    | `04 00 04 00 01 00 00` |
| `HRM_STATE` (0x30) enable | `04 00 04 00 09 00 30 01 00 00 00` |
| `HEART_RATE_START_1S`     | `04 00 04 00 17 00 00 00 10 00 10 00 08 e3 46 42 0b 08 13 10 02 1a 05 01 40 42 0f 00` |

(The `e3 46` in the START frame is Android's exact varint constant for the 1 s
period; we pinned it to rule out a value mismatch.)

The AirPods reply with the ACK `4a 02 08 13` — i.e. they *understood and
accepted* the enable — but the data frame that carries a reading
(`08 13 1A 12 <18-byte payload>`, BPM = `payload[1]`) **never arrives**.

## What we ruled out (this is not our code)

Every plausible transport/host cause was investigated and eliminated:

- **Not the sequence / timing.** Byte-identical to the working clients, with the
  same inter-step delays and quiet period. Both buds in-ear, re-paired,
  rebooted.
- **Not L2CAP ERTM (Enhanced Retransmission Mode).** We tried opening the AAP
  channel with `BRB_L2CA_OPEN_ENHANCED_CHANNEL` + `CM_RETRANSMISSION_AND_FLOW`.
  Windows `bthport` does **not** serialize the ERTM RFC option to the wire for a
  client profile driver (proven across two driver builds). And it wouldn't have
  mattered: the working Android runs AAP over **Basic mode** anyway
  (`l2c_link_adjust_chnl_allocation: FCR Mode:0`), which is what our driver uses.
- **Not encryption.** We opened the AAP channel with encryption *required* —
  `CF_LINK_ENCRYPTED` in the BRB `ChannelFlags` — and confirmed it connects,
  encrypted; the AirPods still ACK service 19 and stream **zero** readings. (An
  earlier attempt also put `CF_LINK_ENCRYPTED` in `ConfigOut/In.Flags`, which are
  a `CFG_*` option bitmask — not link flags — and that broke every connect with
  `STATUS_INVALID_PARAMETER 0xC000000D`. That was a field-placement bug, not
  encryption; with it in the correct field the encrypted channel changes nothing.)
- **Not a missing capability.** The AirPods themselves advertise the `HRM_STATE`
  (0x30) capability to us and ACK the enable — they simply withhold the data.
- **Not descriptor enumeration.** Proper protobuf parsing of
  `request_all_descriptors` confirmed our unit returns only `devmotion6`, never
  the `HEARTRATE` descriptor — even on a unit that a working host *does* get HR
  from. The gate is applied by the AirPods, per-host.

Even the LibrePods author gets no data from his unit on a non-Apple host.

## Why it's a host gate (and why Android can bypass it)

AirPods restrict biometric (heart-rate) data to **Apple hosts**. The working
Android setups don't beat this with a better protocol — they use an **Xposed
module that spoofs the Bluetooth vendor ID inside `com.android.bluetooth`**
(Fluoride). Because Android's Bluetooth stack is open, a hook *inside* the stack
can make the phone present itself as an Apple host, and the AirPods then stream
HR.

Windows can't do the equivalent:

- The Microsoft Bluetooth stack (`bthport`) is **closed**; there's no in-stack
  hook point like Fluoride's.
- The one vendor knob Windows exposes — the local **Device ID (PnP/SDP 0x1200)**
  record under `BTHPORT\Parameters` — was set to the **complete** Apple record a
  real AirPod advertises (`DIDVendorIDSource=1` SIG, `DIDVendorID=0x4C`,
  `DIDProductID=0x2027`, `DIDVersion=0x100`) and republished with a reboot. It
  changed **nothing**: the channel connects identically with or without it, and HR
  stays blocked. The AirPods gate on the host being a real Apple device, which
  this record alone does not establish.

So on Windows the request goes through, the AirPods acknowledge it, and — because
the host isn't (and can't easily be made to look like) an Apple device — they
return no readings.

## What the toggle does

**Settings ▸ Experimental ▸ "Show heart-rate monitoring (experimental)"** only
un-hides the heart-rate card on the device page. It does **not** make the feature
work. Expect the card to stay empty. It's kept for the day the Apple-host gate
can actually be bypassed on Windows.

## References

- `crossplatform/windows/drivers/aap/L2cap.c` — the driver runs the AAP channel
  in L2CAP **Basic** mode (the ERTM/encryption experiments are documented in the
  comments there).
- `crossplatform/windows/daemon/src/main.rs` — `hr_retry_campaign` / the HR
  enable + start sequence and constants (`HR_START_SEQ`, `HR_INIT_QUIET_MS`, …).
