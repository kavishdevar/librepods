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
    [0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x0D, mode, 0x00, 0x00, 0x00]
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
        // status 0x04 = component not connected/present -> report as unknown.
        let status = payload[base + 3];
        let level = if status == 0x04 {
            None
        } else {
            Some(payload[base + 2])
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
