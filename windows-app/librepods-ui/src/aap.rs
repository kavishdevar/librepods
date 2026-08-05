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

/// If `data` reports the listening mode (control command 0x09, id 0x0D),
/// return the mode value.
pub fn parse_anc_mode(data: &[u8]) -> Option<u8> {
    if data.len() >= 8 && data[..4] == HEADER && data[4] == 0x09 && data[6] == 0x0D {
        Some(data[7])
    } else {
        None
    }
}
