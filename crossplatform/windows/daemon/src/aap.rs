//! AAP protocol: outgoing commands + parsers for the packets the AirPods push.

pub const PSM_AACP: u16 = 0x1001;

pub const HANDSHAKE: [u8; 16] = [
    0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
pub const SET_FEATURES: [u8; 14] = [
    0x04, 0x00, 0x04, 0x00, 0x4D, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
pub const REQUEST_NOTIFS: [u8; 10] =
    [0x04, 0x00, 0x04, 0x00, 0x0F, 0x00, 0xFF, 0xFF, 0xFF, 0xFF];

/// Enable the hi-res (AAC-ELD) microphone stream — the AirPods start pushing
/// 0x58 uplink audio packets. From LibrePods PR #655.
pub const START_AUDIO: [u8; 19] = [
    0x04, 0x00, 0x04, 0x00, 0x58, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x01, 0x82, 0x00, 0x00, 0x00,
    0x04, 0x96, 0x00,
];
/// Stop the hi-res microphone stream.
pub const STOP_AUDIO: [u8; 12] = [
    0x04, 0x00, 0x04, 0x00, 0x58, 0x00, 0x00, 0x00, 0x02, 0x00, 0x03, 0x01,
];

// ---- AirPods Pro 3 RTBuddy heart-rate (PR #702) ----
//
// Enable = the AACP 1.3 init handshake (the four CONNECT/CAPABILITIES packets,
// sent RAW via the driver like `sendPacket` on Android) then the HRM_STATE
// control command (0x30 on) and finally the START frame. The four init packets
// and START/STOP already carry the `04 00 04 00` header inline (on Android the
// init packets go through `sendPacket` as-is; START/STOP go through
// `sendDataPacket` which *prepends* the header — we bake it in here so every
// constant is a ready-to-send driver packet). Order + delays mirror
// `HeartRateMonitor.initializeAacpSession()` / `startStreamAttempt()`.

/// AACP 1.3 init, service 0 — CONNECT (raw `sendPacket`).
pub const HR_CONNECT_SERVICE_0: [u8; 16] = [
    0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
/// AACP 1.3 init, service 0 — CAPABILITIES (raw `sendPacket`).
pub const HR_CAPABILITIES_SERVICE_0: [u8; 7] = [0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00];
/// AACP 1.3 init, service 4 — CONNECT (raw `sendPacket`).
pub const HR_CONNECT_SERVICE_4: [u8; 16] = [
    0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
/// AACP 1.3 init, service 4 — CAPABILITIES (raw `sendPacket`).
pub const HR_CAPABILITIES_SERVICE_4: [u8; 7] = [0x04, 0x00, 0x04, 0x00, 0x01, 0x00, 0x00];

/// HRM_STATE control command (id 0x30), value 0x01 = on. This is exactly
/// `control_command(0x30, 0x01)`; the AirPods echo it back as a status.
pub const HR_ENABLE: [u8; 11] = [0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x30, 0x01, 0x00, 0x00, 0x00];

/// RTBuddy SensorDataWX HEARTRATE(19) START frame (1s cadence). Includes the
/// `04 00 04 00` header (Android's `sendDataPacket` prepends it).
pub const HR_START: [u8; 28] = [
    0x04, 0x00, 0x04, 0x00, // header
    0x17, 0x00, 0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x08, 0xE3, 0x46, 0x42, 0x0B, 0x08, 0x13, 0x10,
    0x02, 0x1A, 0x05, 0x01, 0x40, 0x42, 0x0F, 0x00,
];
/// RTBuddy SensorDataWX HEARTRATE(19) STOP frame. Includes the header.
pub const HR_STOP: [u8; 28] = [
    0x04, 0x00, 0x04, 0x00, // header
    0x17, 0x00, 0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x08, 0xED, 0x46, 0x42, 0x0B, 0x08, 0x13, 0x10,
    0x02, 0x1A, 0x05, 0x01, 0x00, 0x00, 0x00, 0x00,
];

/// True if `data` is a 0x58 uplink audio packet (carries AAC-ELD frames).
pub fn is_audio_packet(data: &[u8]) -> bool {
    data.len() >= 8
        && data[0] == 0x04
        && data[2] == 0x04
        && data[4] == 0x58
        && data[6] == 0x01
        && data[7] == 0x00
}

/// 0x58 packet layout (PR #655): a 22-byte header, then one or more access
/// units, each a 5-byte record header (length at byte 4) followed by the AU
/// payload. Calls `f` with each AU's AAC-ELD bytes.
pub fn for_each_au(sdu: &[u8], mut f: impl FnMut(&[u8])) {
    const HEADER_LEN: usize = 22;
    let mut off = HEADER_LEN;
    while off + 5 <= sdu.len() {
        let au_len = sdu[off + 4] as usize;
        let start = off + 5;
        let end = start + au_len;
        if au_len == 0 || end > sdu.len() {
            break;
        }
        f(&sdu[start..end]);
        off = end;
    }
}

/// Listening-mode (ANC) control command. value = mode (1 off, 2 anc, 3 transparency, 4 adaptive).
pub fn anc_command(mode: u8) -> [u8; 11] {
    control_command(0x0D, mode)
}

/// Generic AAP control command (opcode 0x09): `[HEADER, 0x09, 0x00, id, value, 0,0,0]`.
/// ANC and the boolean feature toggles are all this shape.
pub fn control_command(id: u8, value: u8) -> [u8; 11] {
    [0x04, 0x00, 0x04, 0x00, 0x09, 0x00, id, value, 0x00, 0x00, 0x00]
}

/// Boolean feature toggle: 0x01 = on, 0x02 = off (matches the AirPods encoding).
pub fn feature_command(id: u8, on: bool) -> [u8; 11] {
    control_command(id, if on { 0x01 } else { 0x02 })
}

/// If `data` is a control-command status for `id` (opcode 0x09), return its value byte.
pub fn parse_control_value(data: &[u8], id: u8) -> Option<u8> {
    if data.len() >= 8 && data[..4] == HEADER && data[4] == 0x09 && data[6] == id {
        Some(data[7])
    } else {
        None
    }
}

/// Parse a rename packet the app sends over the proxy: `[HEADER, 0x1A, 0x00,
/// 0x01, size, 0x00, ...name]`. Returns the new device name.
pub fn parse_rename(data: &[u8]) -> Option<String> {
    if data.len() >= 9 && data[..4] == HEADER && data[4] == 0x1A {
        let size = data[7] as usize;
        if data.len() >= 9 + size {
            return String::from_utf8(data[9..9 + size].to_vec()).ok();
        }
    }
    None
}

/// Conversational Awareness event (opcode 0x4B): the AirPods signal that you
/// started/stopped speaking; the status byte drives the host-side volume duck.
/// 1 = start, 2 = reduce, 3 = partial, 4/6/7 = end.
pub fn parse_conversational_awareness(data: &[u8]) -> Option<u8> {
    if data.len() >= 10 && data[..4] == HEADER && data[4] == 0x4B {
        Some(data[9])
    } else {
        None
    }
}

pub fn anc_name(mode: u8) -> &'static str {
    match mode {
        1 => "Off",
        2 => "Noise Cancellation",
        3 => "Transparency",
        4 => "Adaptive",
        _ => "?",
    }
}

#[derive(Default, Clone, Copy)]
pub struct Battery {
    pub headphone: Option<u8>,
    pub left: Option<u8>,
    pub right: Option<u8>,
    pub case: Option<u8>,
}

const HEADER: [u8; 4] = [0x04, 0x00, 0x04, 0x00];

/// If `data` is a battery packet (opcode 0x04), return the parsed levels.
pub fn parse_battery(data: &[u8]) -> Option<Battery> {
    if data.len() < 7 || data[..4] != HEADER || data[4] != 0x04 {
        return None;
    }
    let payload = &data[4..]; // starts at opcode
    let count = payload[2] as usize;
    let mut b = Battery::default();
    for i in 0..count {
        let base = 3 + i * 5;
        if base + 3 >= payload.len() {
            break;
        }
        // status 0x04 = component not connected/present; level 0xFF (255) = the
        // slot exists but is absent (e.g. the headphone slot on earbuds) -> both
        // report as unknown. (0x05 = charging in case, level valid.)
        let status = payload[base + 3];
        let raw = payload[base + 2];
        let level = if status == 0x04 || raw == 0xFF {
            None
        } else {
            Some(raw)
        };
        match payload[base] {
            0x01 => b.headphone = level,
            0x02 => b.right = level,
            0x04 => b.left = level,
            0x08 => b.case = level,
            _ => {}
        }
    }
    Some(b)
}

/// In-ear state of one earbud, as reported by the AAP ear-detection packet.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum EarStatus {
    InEar,
    OutOfEar,
    InCase,
    Disconnected,
}

impl EarStatus {
    fn from_byte(b: u8) -> EarStatus {
        match b {
            0x00 => EarStatus::InEar,
            0x02 => EarStatus::InCase,
            0x03 => EarStatus::Disconnected,
            _ => EarStatus::OutOfEar, // 0x01 and anything unexpected
        }
    }
    pub fn in_ear(self) -> bool {
        self == EarStatus::InEar
    }
}

/// If `data` is an ear-detection packet (opcode 0x06), return the (primary,
/// secondary) earbud statuses. Which physical bud is "primary" varies, so
/// callers should treat them symmetrically (e.g. "is any bud in ear").
pub fn parse_ear_detection(data: &[u8]) -> Option<(EarStatus, EarStatus)> {
    if data.len() >= 8 && data[..4] == HEADER && data[4] == 0x06 {
        Some((EarStatus::from_byte(data[6]), EarStatus::from_byte(data[7])))
    } else {
        None
    }
}

/// If `data` reports the listening mode (control command 0x09, id 0x0D),
/// return the mode value.
pub fn parse_anc_mode(data: &[u8]) -> Option<u8> {
    if data.len() >= 8 && data[..4] == HEADER && data[4] == 0x09 && data[6] == 0x0D {
        Some(data[7])
    } else {
        None
    }
}
