use serde::{Deserialize, Deserializer, Serialize, Serializer};
use std::fmt;
use std::str::FromStr;

/// Bluetooth device identity on Windows: a 48-bit MAC address.
///
/// Windows' `BluetoothAddress` is a `u64` whose low 48 bits are the MAC. This
/// newtype stores the 6 bytes so its `Display`/`FromStr` match the Linux
/// (`bluer::Address`) `XX:XX:XX:XX:XX:XX` form — keeping the `devices.json`
/// MAC keys compatible across platforms.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub struct DeviceId(pub [u8; 6]);

impl fmt::Display for DeviceId {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let b = self.0;
        write!(
            f,
            "{:02X}:{:02X}:{:02X}:{:02X}:{:02X}:{:02X}",
            b[0], b[1], b[2], b[3], b[4], b[5]
        )
    }
}

/// Error returned when a string is not a valid `XX:XX:XX:XX:XX:XX` MAC.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ParseDeviceIdError;

impl fmt::Display for ParseDeviceIdError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str("invalid MAC address")
    }
}

impl std::error::Error for ParseDeviceIdError {}

impl FromStr for DeviceId {
    type Err = ParseDeviceIdError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let mut bytes = [0u8; 6];
        let mut parts = s.split(':');
        for byte in bytes.iter_mut() {
            let part = parts.next().ok_or(ParseDeviceIdError)?;
            *byte = u8::from_str_radix(part, 16).map_err(|_| ParseDeviceIdError)?;
        }
        if parts.next().is_some() {
            return Err(ParseDeviceIdError);
        }
        Ok(DeviceId(bytes))
    }
}

impl Serialize for DeviceId {
    fn serialize<S: Serializer>(&self, serializer: S) -> Result<S::Ok, S::Error> {
        serializer.serialize_str(&self.to_string())
    }
}

impl<'de> Deserialize<'de> for DeviceId {
    fn deserialize<D: Deserializer<'de>>(deserializer: D) -> Result<Self, D::Error> {
        let s = String::deserialize(deserializer)?;
        s.parse().map_err(serde::de::Error::custom)
    }
}

impl From<u64> for DeviceId {
    /// Build from a Windows `BluetoothAddress` (low 48 bits, big-endian).
    fn from(addr: u64) -> Self {
        let b = addr.to_be_bytes();
        DeviceId([b[2], b[3], b[4], b[5], b[6], b[7]])
    }
}

impl From<DeviceId> for u64 {
    fn from(id: DeviceId) -> u64 {
        let b = id.0;
        u64::from_be_bytes([0, 0, b[0], b[1], b[2], b[3], b[4], b[5]])
    }
}
