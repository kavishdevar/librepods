//! AAP protocol bytes and decoders (mirrors the LibrePods Linux implementation).

pub const PSM_AACP: u16 = 0x1001;

/// Initial handshake (raw, no 04000400 header).
pub const HANDSHAKE: [u8; 16] = [
    0x00, 0x00, 0x04, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
/// Set feature flags (opcode 0x4D).
pub const SET_FEATURES: [u8; 14] = [
    0x04, 0x00, 0x04, 0x00, 0x4D, 0x00, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
];
/// Request notifications (opcode 0x0F).
pub const REQUEST_NOTIFS: [u8; 10] =
    [0x04, 0x00, 0x04, 0x00, 0x0F, 0x00, 0xFF, 0xFF, 0xFF, 0xFF];

/// Build a listening-mode (ANC) control command. identifier 0x0D, value = mode.
pub fn anc_command(mode: u8) -> [u8; 11] {
    [
        0x04, 0x00, 0x04, 0x00, 0x09, 0x00, 0x0D, mode, 0x00, 0x00, 0x00,
    ]
}

pub fn parse_anc_mode(s: &str) -> Option<u8> {
    match s.to_ascii_lowercase().as_str() {
        "off" => Some(0x01),
        "anc" | "on" | "noise" => Some(0x02),
        "transparency" | "trans" => Some(0x03),
        "adaptive" => Some(0x04),
        _ => None,
    }
}

pub fn hex(data: &[u8]) -> String {
    data.iter()
        .map(|b| format!("{b:02X}"))
        .collect::<Vec<_>>()
        .join(" ")
}

/// True if this looks like a battery notification (header + opcode 0x04).
pub fn is_battery(data: &[u8]) -> bool {
    data.len() >= 5 && data[..4] == [0x04, 0x00, 0x04, 0x00] && data[4] == 0x04
}

/// Print a decoded battery packet. `payload` starts at the opcode byte (0x04).
pub fn print_battery(payload: &[u8]) {
    if payload.len() < 3 {
        return;
    }
    let count = payload[2] as usize;
    for i in 0..count {
        let base = 3 + i * 5;
        if base + 3 >= payload.len() {
            break;
        }
        let component = match payload[base] {
            0x01 => "Headphone",
            0x02 => "Right",
            0x04 => "Left",
            0x08 => "Case",
            _ => "?",
        };
        let level = payload[base + 2];
        let status = match payload[base + 3] {
            0x01 => "Charging",
            0x02 => "Discharging",
            0x04 => "Disconnected",
            _ => "?",
        };
        println!("   🔋 {component}: {level}%  ({status})");
    }
}
