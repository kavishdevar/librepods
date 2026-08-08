# AAP Definitions (As per AirPods Pro 2 (USB-C) Firmware 7A305)

AAP runs on top of L2CAP, with a PSM of 0x1001 or 4097.

# Handshake
This packet is necessary to establish a connection with the AirPods. Or else, the AirPods will not respond to any packets.

```plaintext
00 00 04 00 01 00 02 00 00 00 00 00 00 00 00 00
```

# Setting specific features for AirPods Pro 2

> *may work for airpods 4 anc also, not tested*

Since apple likes to wall off some features behind specific OS versions, and apple silicon devices, some packets are necessary to enable these features.

I captured the following packet only accidentally, because Apple being Apple decided to hide *this* and *the handshake* from packetlogger, but sometimes it shows up.

*Captured using PacketLogger on an Intel Mac running macOS Sequoia 15.0.1*
```plaintext
04 00 04 00 4d 00 ff 00 00 00 00 00 00 00
```

This packet enables conversational awareness when playing audio. (CA works without this packet only when no audio is playing)

It also enables the Adaptive Transparency feature. (We can set Adaptive Transparency, but it doesn't respond with the same packet See [Noise Cancellation](#changing-noise-control))

# Requesting notifications

This packet is necessary to receive notifications from the AirPods like ear detection, noise control mode, conversational awareness, battery status, etc.

*Captured using PacketLogger on an Intel Mac running macOS Sequoia 15.0.1*
```plaintext
04 00 04 00 0F 00 FF FF FE FF
```

This packet also works.

```plaintext
04 00 04 00 0F 00 FF FF FF FF
```

# Notifications

## Battery

AirPods occasionally send battery status packets. The packet format is as follows:

```plaintext
04 00 04 00 04 00 [battery count] ([component] 01 [level] [status] 01) times the battery count
```

| Components      | Byte value |
|-----------------|------------|
| Headphone*      | 01         |
| Case            | 08         |
| Left            | 04         |
| Right           | 02         |

*The `Headphone` component only exists on over-ear models (AirPods Max). On
earbuds (AirPods Pro/regular) the slot may still be reported with a **level of
`0xFF` (255)**, which means *absent* — treat it as "no such component" and skip
it (otherwise it renders as a bogus "255%").

| Status               | Byte value |
|----------------------|------------|
| Unknown              | 00         |
| Charging             | 01         |
| Discharging          | 02         |
| Disconnected         | 04         |
| Charging (in case)** | 05         |

**`0x05` is reported for an earbud that is **charging inside the case** (the
iPhone shows it charging with its current level). Discovered empirically — it was
previously only handled in the app's parser (`aacp.rs`), not documented here nor
in the Windows daemon's separate parser. Treat `0x01` and `0x05` both as
*charging*.


Example packet from AirPods Pro 2

```plaintext
04 00 04 00 04 00 03 02 01 64 02 01 04 01 63 01 01 08 01 11 02 01
```

| Byte      | Interpretation                     |
|-----------|------------------------------------|
| 7th byte  | Battery Count - 3                  |
| 8th byte  | Battery type - Left                |
| 9th byte  | Spacer, value = 0x01               |
| 10th byte | Battery level 100%                 |
| 11th byte | Battery status - Discharging       |
| 12th byte | Battery component end value = 0x01 |
| 13th byte | Battery type - Right               |
| 14th byte | Spacer, value = 0x01               |
| 15th byte | Battery level 99%                  |
| 16th byte | Battery status - Charging          |
| 17th byte | Battery component end value = 0x01 |
| 18th byte | Battery type - Case                |
| 19th byte | Spacer, value = 0x01               |
| 20th byte | Battery level 17%                  |
| 21st byte | Battery status - Discharging       |
| 22nd byte | Battery component end value = 0x01 |

## Noise Control

The AirPods Pro 2 send noise control packets when the noise control mode is changed (either by a stem long press or by the connected device, see [Changing noise control](#changing-noise-control)). The packet format is as follows:

```plaintext
04 00 04 00 09 00 0D [mode] 00 00 00
```

| Noise Control Mode    | Byte value |
|-----------------------|------------|
| Off                   | 01         |
| Noise Cancellation    | 02         |
| Transparency          | 03         |
| Adaptive Transparency | 04         |

## Ear Detection

AirPods send ear detection packets when the ear detection status changes. The packet format is as follows:
```plaintext
04 00 04 00 06 00 [primary pod] [secondary pod]
```

If primary is removed, mic will be changed and the secondary will be the new primary, so the primary will be the one in the ear, and the packet will be sent again.

| Pod Status | Byte value |
|------------|------------|
| In Ear     | 00         |
| Out of Ear | 01         |
| In Case    | 02         |

## Conversational Awareness

AirPods send conversational awareness packets when the person wearing them start speaking. The packet format is as follows:

```plaintext
04 00 04 00 4B 00 02 00 01 [level]
```

| Level Byte Value    | Meaning                                                 |
|---------------------|---------------------------------------------------------|
| 01/02               | Person Started Speaking; greatly reduce volume          |
| 03                  | Person Stopped Speaking; increase volume back to normal |
| Intermediate values | Intermediate volume levels                              |
| 08/09               | Normal Volume                                           |
### Reading Conversational Awareness State

After requesting notifications, the AirPods send a packet indicating the current state of Conversational Awareness (CA). This packet is only sent once after notifications are requested, not when the CA state is changed.

The packet format is:

```plaintext
04 00 04 00 09 00 28 [status] 00 00 00
```

- `[status]` is a single byte at offset 7 (zero-based), immediately after the header.
    - `0x01` — Conversational Awareness is **enabled**
    - `0x02` — Conversational Awareness is **disabled**
    - Any other value — Unknown/undetermined state

**Example:**
```plaintext
04 00 04 00 09 00 28 01 00 00 00
```
Here, `01` at the 8th byte (offset 7) means CA is enabled.

## Metadata

This packet contains device information like name, model number, etc. The packet format is:

```plaintext
04 00 04 00 1d [strings...]
```

The strings are null-terminated UTF-8 strings in the following order:

1. Bluetooth advertising name (varies in length)
2. Model number 
3. Manufacturer
4. Serial number
5. Firmware version
6. Firmware version 2 (the exact same as before??)
7. Software version   (1.0.0 why would we need it?)
8. App identifier     (com.apple.accessory.updater.app.71 what?)
9. Serial number 1
10. Serial number 2
11. Unknown numeric value
12. Encrypted data
13. Additional encrypted data

Example packet:
```plaintext
040004001d0002d5000400416972506f64732050726f004133303438004170706c6520496e632e0051584e524848595850360036312e313836383034303030323030303030302e323731330036312e313836383034303030323030303030302e3237313300312e302e3000636f6d2e6170706c652e6163636573736f72792e757064617465722e6170702e3731004859394c5432454632364a59004833504c5748444a32364b3000363335373533360089312a6567a5400f84a3ca234947efd40b90d78436ae5946748d70273e66066a2589300035333935303630363400```

The packet contains device identification and version information followed by some encrypted data whose format is not known.
```

# Writing to the AirPods

## Changing Noise Control

We can send a packet to change the noise control mode. The packet format is as follows:

```plaintext
04 00 04 00 09 00 0D [mode] 00 00 00
```

| Noise Control Mode    | Byte value |
|-----------------------|------------|
| Off                   | 01         |
| Noise Cancellation    | 02         |
| Transparency          | 03         |
| Adaptive Transparency | 04         |

The airpods will respond with the same packet after the mode has been changed.

> But if your airpods support Adaptive Transparency, and you haven't sent that [special packet](#setting-specific-features-for-airpods-pro-2) to enable it, the airpods will respond with the same packet but with a different mode (like 0x02).

## Renaming AirPods

We can send a packet to rename the AirPods. The packet format is as follows:

```plaintext
04 00 04 00 1A 00 01 [size] 00 [name]
```

## Toggle case charging sounds

> *This feature is only for cases with a speaker, i.e. the AirPods Pro 2 and the new AirPods 4. Tested only on AirPods Pro 2*

We can send a packet to toggle if sounds should be played when the case is connected to a charger. The packet format is as follows:

```plaintext
12 3A 00 01 00 08 [setting]
```

| Byte Value | Sound |
|------------|-------|
| 00         | On    |
| 01         | Off   |

## Toggle Conversational Awareness

> *This feature is only for AirPods Pro 2 and the new AirPods 4 with ANC. Tested only on AirPods Pro 2*

We can send a packet to toggle Conversational Awareness. If enabled, the AirPods will switch to Transparency mode when the person wearing them starts speaking (and sends packet for notifying the device to reduce volume). The packet format is as follows:

```plaintext
04 00 04 00 09 00 28 [setting] 00 00 00
```

| Byte Value | C.A. |
|------------|------|
| 01         | On   |
| 02         | Off  |

## Adaptive Audio Noise

> *This feature is only for AirPods Pro 2 and the new AirPods 4 with ANC. Tested only on AirPods Pro 2*

The new firmware `7A305` for app2 has a new feature called Adaptive Audio Noise. This allows us to control how much noise is passed through the AirPods when the noise control mode is set to Adaptive. The packet format is as follows:

```plaintext
04 00 04 00 09 00 2E [level] 00 00 00
```

The level can be any value between 0 and 100, 0 to allow maximum noise (i.e. minimum noise filtering), and 100 to filter out more noise.

> This feature is only effective when the noise control mode is set to Adaptive.

*I find it quite funny how I have greater control over the noise control on the AirPods on non-Apple devices than on Apple devices, becuase on Apple Devices, there are just 3 options More Noise (0), Midway through (50), and Less Noise (100), but here I can set any value between 0 and 100.*

## Accessiblity Settings

## Headphone Accomodation
```
04 00 04 00 53 00 84 00 02 02 [Phone] [Media]
[EQ1][EQ2][EQ3][EQ4][EQ5][EQ6][EQ7][EQ8]
duplicated thrice for some reason
```

| Data                | Type          | Value range                 |
|---------------------|---------------|-----------------------------|
| Phone               | Decimal       | 1 (Enabled) or 2 (Disabled) |
| Media               | Decimal       | 1 (Enabled) or 2 (Disabled) |
| EQ                  | Little Endian | 0 to 100                    |

## Customize Transparency mode

```
12 18 00 [enabled]
<left bud>
[EQ1][EQ2][EQ3][EQ4][EQ5][EQ6][EQ7][EQ8]
[Amplification]
[Tone]
[Conversation Boost]
[Ambient Noise Reduction]
<repeat for right bud>
```


All values are formatted as IEEE 754 floats in little endian order.
| Data                    | Type          | Range |
|-------------------------|---------------|-------|
| Enabled                 | IEEE754 Float | 0/1   |
| EQ                      | IEEE754 Float | 0-100 |
| Amplification           | IEEE754 Float | 0-2   |
| Tone                    | IEEE754 Float | 0-2   |
| Conversation Boost      | IEEE754 Float | 0/1   |
| Ambient Noise Reduction | IEEE754 Float | 0-1   |
| Ambient Noise Reduction | IEEE754 Float | 0-1   |

> [!IMPORTANT]
> Also send the [Headphone Accomodation](#headphone-accomodation) after this.


## Configure Stem Long Press

I have noted all the packets sent to configure what the press and hold of the steam should do. The packets sent are specific to the current state. And are probably overwritten everytime the AirPods are connected to a new (apple) device that is not synced with icloud (i think)... So, for non-Apple device too, the configuration needs to be stored and overwritten everytime the AirPods are connected to the device. That is the only way to keep the configuration.

This is also the only way to control the configuration as the previous state needs to be known, and then the new state can be set. 

The packets sent (based on the previous states) are as follows:

<details>
<summary>Toggling Adaptive</summary>

<code>04 00 04 00 09 00 1A 0B 00 00 00</code> - Turns on Adaptive from O and ANC  
<code>04 00 04 00 09 00 1A 0D 00 00 00</code> - Turns on Adaptive from O and T  
<code>04 00 04 00 09 00 1A 0E 00 00 00</code> - Turns on Adaptive from T and ANC  
<code>04 00 04 00 09 00 1A 0F 00 00 00</code> - Turns on Adaptive from O, T, ANC  

<code>04 00 04 00 09 00 1A 03 00 00 00</code> - Turns off Adaptive from O and ANC (and Adaptive)  
<code>04 00 04 00 09 00 1A 05 00 00 00</code> - Turns off Adaptive from O and T (and Adaptive)  
<code>04 00 04 00 09 00 1A 06 00 00 00</code> - Turns off Adaptive from T and ANC (and Adaptive)  
<code>04 00 04 00 09 00 1A 07 00 00 00</code> - Turns off Adaptive from O, T, ANC (and Adaptive)  

</details>

<details>
<summary>Toggling Transparency</summary>

<code>04 00 04 00 09 00 1A 07 00 00 00</code> - Turns on Transparency from O and ANC  
<code>04 00 04 00 09 00 1A 0D 00 00 00</code> - Turns on Transparency from O and Adaptive  
<code>04 00 04 00 09 00 1A 0E 00 00 00</code> - Turns on Transparency from Adaptive, and ANC  
<code>04 00 04 00 09 00 1A 0F 00 00 00</code> - Turns on Transparency from O and Adaptive and ANC  

<code>04 00 04 00 09 00 1A 03 00 00 00</code> - Turns off Transparency from O and ANC (and Transparency)  
<code>04 00 04 00 09 00 1A 09 00 00 00</code> - Turns off Transparency from O and Adaptive (and Transparency)  
<code>04 00 04 00 09 00 1A 0A 00 00 00</code> - Turns off Transparency from Adaptive, and ANC (and Transparency)  
<code>04 00 04 00 09 00 1A 0B 00 00 00</code> - Turns off Transparency from O and Adaptive and ANC (and Transparency)  

</details>

<details>
<summary>Toggling ANC</summary>

<code>04 00 04 00 09 00 1A 07 00 00 00</code> - Turns on ANC from O, and Transparency  
<code>04 00 04 00 09 00 1A 0B 00 00 00</code> - Turns on ANC from O, and Adaptive  
<code>04 00 04 00 09 00 1A 0E 00 00 00</code> - Turns on ANC from Adaptive, and Transparency  
<code>04 00 04 00 09 00 1A 0F 00 00 00</code> - Turns on ANC from O and Adaptive and Transparency  

<code>04 00 04 00 09 00 1A 05 00 00 00</code> - Turns off ANC from O and Transparency (and ANC)  
<code>04 00 04 00 09 00 1A 09 00 00 00</code> - Turns off ANC from O and Adaptive (and ANC)  
<code>04 00 04 00 09 00 1A 0C 00 00 00</code> - Turns off ANC from Adaptive, and Transparency (and ANC)  
<code>04 00 04 00 09 00 1A 0D 00 00 00</code> - Turns off ANC from O and Adaptive and Transparency (and ANC)  

</details>

<details>
<summary>Toggling O</summary>

<code>04 00 04 00 09 00 1A 07 00 00 00</code> - Turns on O from Transparency, and ANC  
<code>04 00 04 00 09 00 1A 0B 00 00 00</code> - Turns on O from Adaptive, and ANC  
<code>04 00 04 00 09 00 1A 0D 00 00 00</code> - Turns on O from Transparency, and Adaptive  
<code>04 00 04 00 09 00 1A 0F 00 00 00</code> - Turns on O from Transparency, and Adaptive, and ANC  

<code>04 00 04 00 09 00 1A 06 00 00 00</code> - Turns off O from Transparency, and ANC (and O)  
<code>04 00 04 00 09 00 1A 0A 00 00 00</code> - Turns off O from Adaptive, and ANC (and O)  
<code>04 00 04 00 09 00 1A 0C 00 00 00</code> - Turns off O from Transparency, and Adaptive (and O)  
<code>04 00 04 00 09 00 1A 0E 00 00 00</code> - Turns off O from Transparency, and Adaptive, and ANC (and O)  

</details>

> *i do hate apple for not hardcoding these, like there are literally only 4^2 - ${\binom{4}{1}}$ - $\binom{4}{2}$*

# Head Tracking

## Start Tracking

This packet initiates head tracking. When sent, the AirPods begin streaming head tracking data (e.g. orientation and acceleration) for live plotting and analysis.

```plaintext
04 00 04 00 17 00 00 00 10 00 10 00 08 A1 02 42 0B 08 0E 10 02 1A 05 01 40 9C 00 00
```

## Stop Tracking

This packet stops the head tracking data stream.

```plaintext
04 00 04 00 17 00 00 00 10 00 11 00 08 7E 10 02 42 0B 08 4E 10 02 1A 05 01 00 00 00 00
```
## Received Head Tracking Sensor Data

Once tracking is active, the AirPods stream sensor packets with the following common structure:
  
| Field                    | Offset | Length (bytes) |
|--------------------------|--------|----------------|
| orientation 1            | 43     | 2              |
| orientation 2            | 45     | 2              |
| orientation 3            | 47     | 2              |
| Horizontal Acceleration  | 51     | 2              |
| Vertical Acceleration    | 53     | 2              |

# Starting and Stopping Sensor Streams

Captured from **iOS 26.5.2 ↔ AirPods Pro 3 (firmware 8B41)** with `idevicebtlogger`,
across three sessions. See `crossplatform/docs/aap-packet-discovery.md` for the method.

Sensor streams are started and stopped with the **same `0x17` … `42 0B` frame family as Head
Tracking above** — not with a different mechanism. The frame carries a stream id and a
sampling period, and **a period of zero is the stop**:

```plaintext
04 00 04 00 17 00 00 00 10 00 11 00 08 70 10 02 42 0b 08 53 10 02 1a 05 01 40 42 0f 00
                              ^^^^^    ^^^^^ ^^^^^       ^^^^^          ^^ ^^^^^^^^^^^
                              len=17   seq   10 02       stream id      md period µs LE
```

| Field | Meaning |
|---|---|
| length | `11 00` = **17**, little-endian |
| seq | varint, increments per control frame |
| stream id | `08 <id>` inside the `42 0B` block |
| mode | `1`, `2` or `4` — meaning unresolved |
| period | little-endian u32, **microseconds**; `0` = stop the stream |

Observed stream ids, consistent across all three captures:

| Stream id | Period sent | Rate | Carries |
|---|---|---|---|
| `0x53` | `1000000` | 1 Hz | **heart rate** (data type 19) |
| `0x50` | `20000` | 50 Hz | raw PPG (data type 16) |
| `0x52` | `200` | — | the worn-state sensor (data type 18) |
| `0x10`, `0x12` | `10` / `0` | — | seen with mode 4 / mode 2 around reconnection |

The id appears to be the data type with bit `0x40` set: type 19 (`0x13`) → id `0x53`, type 16
(`0x10`) → id `0x50`, type 18 (`0x12`) → id `0x52`. That also matches the Head Tracking stop
frame documented above (`08 4E`, i.e. type 14 | `0x40`). Note that bare `0x10` and `0x12` also
occur, so the bit is not simply part of the id.

## Worked example — a full heart-rate session

From the third capture, one clock, showing that the `0x17` frame is what actually drives the
stream:

```plaintext
t=126.78  →  44 00 04 00 02 00 03 07            opcode 0x44 (see below)
t=126.80  →  17 … 08 53 … period 1000000        start heart rate at 1 Hz
t=126.96  →  17 … 08 50 … period 20000          start raw PPG at 50 Hz
t=128.68  ←  first heart-rate frame                        (1.88 s after the start frame)
   …
t=209.99  →  17 … 08 50 … period 0              stop raw PPG
t=241.48  →  17 … 08 53 … period 0              stop heart rate
t=241.65  ←  last heart-rate frame                         (170 ms after the stop frame)
```

## Corrections to `crossplatform/windows/daemon/src/aap.rs`

`HR_START` and `HR_STOP` are close but not correct. Against the captured frames:

| | `aap.rs` | captured |
|---|---|---|
| length field | `10 00` (16) | **`11 00` (17)** |
| field after seq | *absent* | **`10 02`** |
| stream id | `08 13` (19) | **`08 53` (83)** |
| period (start) | `01 40 42 0F 00` | `01 40 42 0F 00` — **correct**, 1 Hz |
| period (stop) | `01 00 00 00 00` | `01 00 00 00 00` — **correct** |

So the sampling period was right all along; the length, the `10 02` field and the stream id
are wrong. `HR_STOP` additionally reuses the start id rather than switching to `0x53`.

## Opcode 0x44

Distinct from the above and **not** the mechanism that starts streams. Framing is settled:

```plaintext
04 00 04 00 44 00 04 00 02 00 03 07
            ^^^^^ ^^^^^ ^^^^^^^^^^^
            op    len   payload
```

The length field is confirmed by a 14-byte variant:

```plaintext
04 00 04 00 44 00 0e 00 03 00 02 01 00 00 23 0c 77 6a 00 00 00 00
```

**Payload semantics remain unresolved.** In the 4-byte form the first three bytes were
`02 00 03` in every occurrence across all three captures and only the trailing byte varied
(`01`, `02`, `06`, `07`). It is *not* a sensor id list: `02 00 03 07` occurs without any stream
starting, and `02 00 03 01` / `02 00 03 06` start nothing at all. It does consistently appear
tens of milliseconds *before* the `0x17` control frames at a session change — 20 ms before in
the worked example above — so it reads as session or configuration signalling that brackets a
stream change rather than causing it.

Observed sensors (`field 2` of the `0x17` protobuf):

| Sensor id | Data type | Rate | Notes |
|---|---|---|---|
| 3 | 16 | ~50 Hz | raw PPG samples |
| 3 | 19 | 1 Hz | **heart rate** (see below) |
| 7 | 18 | ~5 Hz | **tracks the worn state** — see below |
| 1, 2 | — | bursty | seen only around reconnection and case transitions |

### Sensor 7 follows the worn state, not audio playback

Worth recording as a method note, because the two captures differ in exactly the confounding
variable and it would otherwise be easy to get this wrong.

| | audio playing | worn | sensor 7 streaming |
|---|---|---|---|
| Capture 1 (workout) | no | entire 93 s — zero ear-detection events | **0.0 → 93.0 s**, i.e. throughout |
| Capture 2 (case) | yes, until ~52 s | until 53.06 s | **0.0 → 52.8 s** |

In capture 2 alone, sensor 7 stopping looks like it tracks playback — the music was stopped
immediately before the buds were removed, so the two events are 260 ms apart and
indistinguishable. Capture 1 separates them: **no audio played at any point, yet sensor 7
streamed for the full 93 s while the buds stayed in the ears.**

So sensor 7 is tied to the buds being worn, not to playback. Its last packet precedes the
first ear-detection state change by 260 ms, which fits a motion or proximity sensor reacting
before the in-ear determination is published.

## Received Heart Rate Data

Sensor 3, protobuf field `0x3a`, inner type `19` (`0x13`), one packet per second:

```plaintext
04 00 04 00 17 00 00 00 10 00 <len> 00 08 <seq> 10 03 3a <n> 08 13 1a 12 01 5D E1 07 00 02 …
                                                            ^^^^^          ^^ ^^ ^^    ^^
                                                            type 19       bpm cf sq  state
                                                                          93 225  7  locked
```

`1a 12` introduces an **18-byte payload**. Offsets below are relative to that payload,
matching `HEART_RATE_BPM_OFFSET` / `HEART_RATE_STATUS_TAIL_OFFSET` in
`crossplatform/windows/daemon/src/hr.rs`:

| Field | Offset | Length | Meaning |
|---|---|---|---|
| subtype | 0 | 1 | `01` |
| **heart rate** | 1 | 1 | BPM, unsigned, direct value |
| confidence | 2 | 1 | `20` while settling, rises to ~`236` once locked |
| counter | 3 | 1 | increments by 1 per reading (confirms 1 Hz) |
| state | 5 | 1 | `1` = acquiring, `2` = locked |
| timestamp | 6 | 6 | little-endian |
| status tail | 15 | 3 | see below |

**The first readings must be discarded.** In the reference capture the sequence opened
169 → 136 → 98 → 93 BPM with `confidence = 20` and `state = 1`, then settled at 91 BPM the
moment `state` flipped to `2` and confidence jumped to 189 — four readings, ~4 s from stream
start. The remaining 58 readings spanned 86–102 BPM, mean 93.1, tracking a plausible curve
for light activity.

The existing status-tail filter in `hr.rs` already does exactly this job. Tails observed:

| Tail | n | In `KNOWN_HEART_RATE_STATUS_TAILS` | `state` |
|---|---|---|---|
| `10 00 00` | 58 | yes | 2 (locked) |
| `10 02 81` | 3 | no | 1 (acquiring) |
| `10 82 81` | 1 | no | 1 (acquiring) |

So the decoder accepted 58/62 and rejected precisely the four settling readings — the tail
filter and a `state == 2` test agree exactly on this capture. `10 02 81` / `10 82 81` are new
variants of the known `20 02 80` / `20 82 80` pair; they should **not** be added to the accept
list, since they mark unlocked readings.

# Case and Charging Transitions

Second capture, same rig (iOS 26.5.2 ↔ AirPods Pro 3, fw 8B41). Flow: **worn with music
playing** → playback stopped → removed from ears → placed in the open case → case
interaction → lid closed → lid reopened → idle. 176 s, 810 AAP packets.

The music phase was not part of the intended test, and playback stopping happened to coincide
with the buds being removed — see the sensor 7 note below for why that nearly produced a wrong
conclusion.

## Charging status: what selects `0x01` versus `0x05`

`## Battery` above already documents `0x05` as *charging (in case)* and advises treating it
and `0x01` alike. This capture supports that advice and adds why it is needed: the two are not
alternative encodings chosen by firmware or model, they are **consecutive states of the same
charging session**, so an implementation that handles only one of them will work for part of
the time and then stop working.

| t (s) | observation |
|---|---|
| 13.1 | baseline — both buds `not-charging` (`0x02`), case `disconnected` (`0x04`) |
| 78.1 | first bud enters case → **`0x05` charging-in-case**, immediately |
| 79.0 | case starts reporting a real level (one transient `0xFF` frame first) |
| 80.3 | second bud enters case → **`0x05`** |
| 106.8 | **both buds flip to `0x01` charging**, simultaneously, ~26 s after insertion |
| 145.9 | both levels have risen by 1 % — charging did occur |

**`0x05` is not a mandatory precursor to `0x01`.** A controlled follow-up refuted the obvious
reading of the table above. One bud was seated in the case with the lid open while the other
stayed in an ear as a control, and nothing was touched for 240 s:

| t (s) | observation |
|---|---|
| 94.2 | bud seated → **`0x01` charging immediately**, no `0x05` at any point; case reporting 60 % |
| 255.6 | cased bud 80 % → 81 %, still `0x01` |
| 305.5 | 81 % → 82 %, still `0x01` |
| 334.5 | worn control bud 79 % → 78 %, `not-charging` throughout, as expected |

So the transition is **not driven by elapsed time** — 240 s hands-off produced no state change
at all — and a bud can report `0x01` from the instant it is seated. Charging genuinely
occurred throughout while `0x01` was reported.

A second controlled run — **both** buds seated, lid open, untouched for 162 s — also produced
`0x01` from the instant each bud was seated and **never once reported `0x05`**. That kills the
number-of-buds explanation.

Across four captures:

| Capture | Battery packets | Components reporting `0x05` |
|---|---|---|
| Workout | 1 | 0 |
| Case — buds seated, **case and lid handled** | 15 | **12** |
| Battery — one bud, hands-off 240 s | 7 | 0 |
| Battery — both buds, hands-off 162 s | 10 | 0 |

**Seating buds in the case does not by itself produce `0x05`.** Three hypotheses are now
refuted: it is not elapsed time (240 s of observation, no change), not the number of buds in
the case, and not whether the case is detected — a bud reported `0x01` while the case was
still sending `255 %` / `disconnected`. The only capture that produced `0x05` is also the only
one in which the case and its lid were physically interacted with, which makes that the
remaining candidate, untested.

The practical consequence is unchanged and is what `## Battery` already advises — **treat
`0x01` and `0x05` both as charging.** The value that appears is not predictable from elapsed
time or from the case's own reported state.

## Ear detection: two values beyond the documented three

`## Ear Detection` above lists `00` in-ear, `01` out-of-ear and `02` in-case. Removing the buds
and casing them produced this sequence of `(primary, secondary)` pairs:

```plaintext
00 01  →  01 01  →  01 04  →  04 04  →  04 01  →  01 01  →  02 01  →  01 02  →  02 02
```

and later, with one bud powered down in the closed case, `02 03`.

So **`03` and `04` are the two values not in that table**. `04` appears only while a bud is in
motion between resting states and never at rest, so it reads as a transitional value alongside
the documented `01`. `03` was seen at rest, on the bud that had dropped off the link — it
behaves as *disconnected* rather than as a position.

# Undocumented Opcodes Observed

Everything below appeared in the captures and is **not** described elsewhere in this document.
Listed so the next person does not have to rediscover that they exist; payload semantics are
mostly unresolved.

| Opcode | Count | What can be said |
|---|---|---|
| `0x4F` | 186 | Accessory asset/firmware protocol — request/response pairs carrying `HSML`, `VERS`, `FTAB` tags, version tables and per-language asset manifests |
| `0x44` | 20 | Brackets stream changes — see above |
| `0x2E` | 14 | Carries Bluetooth addresses of the linked devices; emitted around reconnection |
| `0x0C` | 14 | Carries a Bluetooth address plus two status bytes |
| `0x0E` | 11 | Carries a Bluetooth address plus one status byte |
| `0x4C` | 8 | Short status records, emitted in pairs on reconnect |
| `0x08` | 8 | 4-byte payload, emitted alongside battery updates |
| `0x55` | 5 | 4-byte payload, constant across captures |
| `0x59` | 4 | Two 8-byte little-endian values; seen immediately before `0x44` |
| `0x01` `0x02` `0x0D` `0x1B` `0x22` `0x23` `0x24` `0x29` `0x2B` `0x2D` `0x4E` `0x54` | 3 each | Emitted together as one burst during the reconnection handshake |
| `0x1F` `0x52` | 1 each | Single occurrence during reconnection |

Two opcodes that showed up in the captures are **already documented above** and are noted here
only because the observations corroborate the existing entries:

- **`0x1D`** — `## Metadata`. Field order matched the documented list exactly across six
  occurrences. A second variant carries `com.apple.accessory.updater.app.multiasset.71` as the
  app identifier where the documented example has `…updater.app.71`.
- **`0x53`** — `## Headphone Accomodation`. The payload is the documented
  `84 00 02 02 [Phone][Media]` followed by eight EQ float32 values, repeated three times, which
  matches the "duplicated thrice for some reason" note.

> **Note for anyone sharing captures:** `0x1D` transmits the device serial number and the
> user-assigned device name in plaintext, and `0x2E` / `0x0C` / `0x0E` carry Bluetooth
> addresses. A raw `.pklg` is personally identifying even with MAC addresses stripped from
> the HCI layer. Publish derived protocol facts, not capture files.

## Payload layouts (identifying fields redacted)

**`0x1D` — device identity.** A 7-byte header followed by a run of NUL-terminated strings.
Field order was stable across all six occurrences:

```plaintext
1d 00 02 fc 00 08 00
  "AirPods Pro ****"        user-assigned device name          [REDACTED]
  "A3064"                   model number
  "Apple Inc."              manufacturer
  "**********"              device serial                      [REDACTED]
  "81.26750000750000….6877" firmware version string
  "81.26750000750000….6877" firmware version string (repeated)
  "1.0.0"
  "com.apple.accessory.updater.app.multiasset.71"   asset bundle id
  "******************"      per-unit module id                 [REDACTED]
  "******************"      per-unit module id                 [REDACTED]
  "*******"                 part/build number                  [REDACTED]
  <32 bytes binary>         opaque, likely a key or digest     [REDACTED]
  "1770731447"              unix timestamp
  "1770731447"              unix timestamp (repeated)
```

A second variant carries `02 f1 00 04 00` in the header and the bundle id
`com.apple.accessory.updater.app.71`; all other fields match.

**`0x2E` — linked-device addresses.** Two 6-byte Bluetooth addresses:

```plaintext
2e 00 01 00 02 XX XX XX XX XX XX 02 07 YY YY YY YY YY YY 00 01
              ^^^^^^^^^^^^^^^^^^       ^^^^^^^^^^^^^^^^^^
              addr A [REDACTED]        addr B [REDACTED]
```

**`0x0C` / `0x0E` — per-device status.** One Bluetooth address plus trailing status bytes:

```plaintext
0c 00 XX XX XX XX XX XX 00 02      0e 00 XX XX XX XX XX XX 00
      ^^^^^^^^^^^^^^^^^^                 ^^^^^^^^^^^^^^^^^^
      [REDACTED]                         [REDACTED]
```

**`0x59` — two 64-bit values.** No identifying content; seen immediately before `0x44`:

```plaintext
59 00 11 00 01 6f 0c 77 6a 00 00 00 00 50 09 78 6a 00 00 00 00
```

## Undocumented control command ids (opcode `0x09`)

| Id | Direction | Value(s) seen | Notes |
|---|---|---|---|
| `0x0B` | phone → buds | `0x3C`, `0x96` (60, 150) | sent while worn, before any workout or case interaction |
| `0x38` | buds → phone | `0x52` | emitted twice, once while worn and once after entering the case |
| `0x3B` | phone → buds | `0x01` | 260 ms before the `0x05` → `0x01` charging flip |
| `0x1A` `0x32` `0x3D` | phone → buds | `0x0E`, `0x01`, `0x01` | always sent as a trio during the reconnection handshake |

# LICENSE

LibrePods - AirPods liberated from Apple’s ecosystem
Copyright (C) 2025 LibrePods contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
