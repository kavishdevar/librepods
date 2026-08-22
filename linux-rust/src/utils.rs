use aes::Aes128;
use aes::cipher::Array;
use aes::cipher::{BlockCipherEncrypt, KeyInit};
use iced::Theme;
use serde::{Deserialize, Serialize};
use std::io;
use std::path::PathBuf;

pub fn get_devices_path() -> PathBuf {
    let data_dir = std::env::var("XDG_DATA_HOME")
        .unwrap_or_else(|_| format!("{}/.local/share", std::env::var("HOME").unwrap_or_default()));
    PathBuf::from(data_dir)
        .join("librepods")
        .join("devices.json")
}

pub fn get_preferences_path() -> PathBuf {
    let config_dir = std::env::var("XDG_CONFIG_HOME")
        .unwrap_or_else(|_| format!("{}/.config", std::env::var("HOME").unwrap_or_default()));
    PathBuf::from(config_dir)
        .join("librepods")
        .join("preferences.json")
}

pub fn get_app_settings_path() -> PathBuf {
    let home = std::env::var("HOME").unwrap_or_default();

    let config_dir = std::env::var("XDG_CONFIG_HOME")
        .unwrap_or_else(|_| format!("{}/.config", home));

    let data_dir = std::env::var("XDG_DATA_HOME")
        .unwrap_or_else(|_| format!("{}/.local/share", home));

    let new_path = PathBuf::from(&config_dir)
        .join("librepods")
        .join("app_settings.json");

    let old_path = PathBuf::from(&data_dir)
        .join("app_settings.json");

    // migrate if needed
    if old_path.exists() && !new_path.exists() {
        if let Some(parent) = new_path.parent() {
            let _ = std::fs::create_dir_all(parent);
        }

        if std::fs::copy(&old_path, &new_path).is_ok() {
            let _ = std::fs::remove_file(&old_path);
        }
    }

    new_path
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(default)]
pub struct AppSettings {
    pub theme: MyTheme,
    pub tray_text_mode: bool,
    pub stem_control: bool,
    pub close_window_hotkey: Option<Hotkey>,
    pub quit_application_hotkey: Option<Hotkey>,
}

impl Default for AppSettings {
    fn default() -> Self {
        Self {
            theme: MyTheme::Dark,
            tray_text_mode: false,
            stem_control: false,
            close_window_hotkey: Some(Hotkey::with_control("w")),
            quit_application_hotkey: Some(Hotkey::with_control("q")),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct Hotkey {
    key: String,
    control: bool,
    alt: bool,
    shift: bool,
    logo: bool,
}

impl Hotkey {
    fn with_control(key: &str) -> Self {
        Self::new(key.to_string(), true, false, false, false)
    }

    pub fn new(key: String, control: bool, alt: bool, shift: bool, logo: bool) -> Self {
        Self {
            key,
            control,
            alt,
            shift,
            logo,
        }
    }

    pub fn matches(
        &self,
        key: &str,
        control: bool,
        alt: bool,
        shift: bool,
        logo: bool,
    ) -> bool {
        self.key == key
            && self.control == control
            && self.alt == alt
            && self.shift == shift
            && self.logo == logo
    }

    pub fn display(&self) -> String {
        let mut parts = Vec::new();
        if self.control {
            parts.push("Ctrl".to_string());
        }
        if self.alt {
            parts.push("Alt".to_string());
        }
        if self.shift {
            parts.push("Shift".to_string());
        }
        if self.logo {
            parts.push("Super".to_string());
        }
        parts.push(if self.key.chars().count() == 1 {
            self.key.to_uppercase()
        } else {
            self.key.clone()
        });
        parts.join("+")
    }
}

impl AppSettings {
    pub fn load() -> Self {
        std::fs::read_to_string(get_app_settings_path())
            .ok()
            .and_then(|settings| serde_json::from_str(&settings).ok())
            .unwrap_or_default()
    }

    pub fn save(&self) -> io::Result<()> {
        let path = get_app_settings_path();
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        let settings = serde_json::to_string_pretty(self).map_err(io::Error::other)?;
        std::fs::write(path, settings)
    }
}

#[cfg(test)]
mod tests {
    use super::{AppSettings, Hotkey, MyTheme};

    #[test]
    fn old_app_settings_use_default_hotkeys() {
        let settings: AppSettings = serde_json::from_str(
            r#"{"theme":"Nord","tray_text_mode":true,"stem_control":true}"#,
        )
        .unwrap();

        assert_eq!(settings.theme, MyTheme::Nord);
        assert!(settings.tray_text_mode);
        assert!(settings.stem_control);
        assert_eq!(settings.close_window_hotkey, Some(Hotkey::with_control("w")));
        assert_eq!(settings.quit_application_hotkey, Some(Hotkey::with_control("q")));
    }

    #[test]
    fn hotkey_formats_modifiers_and_key() {
        let hotkey = Hotkey::new("k".to_string(), true, true, true, false);

        assert_eq!(hotkey.display(), "Ctrl+Alt+Shift+K");
        assert!(hotkey.matches("k", true, true, true, false));
    }

    #[test]
    fn app_settings_round_trip_custom_hotkeys() {
        let expected = AppSettings {
            close_window_hotkey: None,
            quit_application_hotkey: Some(Hotkey::new(
                "F12".to_string(),
                false,
                true,
                false,
                false,
            )),
            ..AppSettings::default()
        };

        let json = serde_json::to_string(&expected).unwrap();
        let actual: AppSettings = serde_json::from_str(&json).unwrap();

        assert_eq!(actual.close_window_hotkey, expected.close_window_hotkey);
        assert_eq!(
            actual.quit_application_hotkey,
            expected.quit_application_hotkey
        );
    }
}

fn e(key: &[u8; 16], data: &[u8; 16]) -> [u8; 16] {
    let mut swapped_key = *key;
    swapped_key.reverse();
    let mut swapped_data = *data;
    swapped_data.reverse();
    let cipher = Aes128::new(&Array::from(swapped_key));
    let mut block = Array::from(swapped_data);
    cipher.encrypt_block(&mut block);
    let mut result: [u8; 16] = block.into();
    result.reverse();
    result
}

pub fn ah(k: &[u8; 16], r: &[u8; 3]) -> [u8; 3] {
    let mut r_padded = [0u8; 16];
    r_padded[..3].copy_from_slice(r);
    let encrypted = e(k, &r_padded);
    let mut hash = [0u8; 3];
    hash.copy_from_slice(&encrypted[..3]);
    hash
}

#[derive(Debug, Clone, Copy, PartialEq, Serialize, Deserialize)]
pub enum MyTheme {
    Light,
    Dark,
    Dracula,
    Nord,
    SolarizedLight,
    SolarizedDark,
    GruvboxLight,
    GruvboxDark,
    CatppuccinLatte,
    CatppuccinFrappe,
    CatppuccinMacchiato,
    CatppuccinMocha,
    TokyoNight,
    TokyoNightStorm,
    TokyoNightLight,
    KanagawaWave,
    KanagawaDragon,
    KanagawaLotus,
    Moonfly,
    Nightfly,
    Oxocarbon,
    Ferra,
}

impl std::fmt::Display for MyTheme {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            Self::Light => "Light",
            Self::Dark => "Dark",
            Self::Dracula => "Dracula",
            Self::Nord => "Nord",
            Self::SolarizedLight => "Solarized Light",
            Self::SolarizedDark => "Solarized Dark",
            Self::GruvboxLight => "Gruvbox Light",
            Self::GruvboxDark => "Gruvbox Dark",
            Self::CatppuccinLatte => "Catppuccin Latte",
            Self::CatppuccinFrappe => "Catppuccin Frappé",
            Self::CatppuccinMacchiato => "Catppuccin Macchiato",
            Self::CatppuccinMocha => "Catppuccin Mocha",
            Self::TokyoNight => "Tokyo Night",
            Self::TokyoNightStorm => "Tokyo Night Storm",
            Self::TokyoNightLight => "Tokyo Night Light",
            Self::KanagawaWave => "Kanagawa Wave",
            Self::KanagawaDragon => "Kanagawa Dragon",
            Self::KanagawaLotus => "Kanagawa Lotus",
            Self::Moonfly => "Moonfly",
            Self::Nightfly => "Nightfly",
            Self::Oxocarbon => "Oxocarbon",
            Self::Ferra => "Ferra",
        })
    }
}

impl From<MyTheme> for Theme {
    fn from(my_theme: MyTheme) -> Self {
        match my_theme {
            MyTheme::Light => Theme::Light,
            MyTheme::Dark => Theme::Dark,
            MyTheme::Dracula => Theme::Dracula,
            MyTheme::Nord => Theme::Nord,
            MyTheme::SolarizedLight => Theme::SolarizedLight,
            MyTheme::SolarizedDark => Theme::SolarizedDark,
            MyTheme::GruvboxLight => Theme::GruvboxLight,
            MyTheme::GruvboxDark => Theme::GruvboxDark,
            MyTheme::CatppuccinLatte => Theme::CatppuccinLatte,
            MyTheme::CatppuccinFrappe => Theme::CatppuccinFrappe,
            MyTheme::CatppuccinMacchiato => Theme::CatppuccinMacchiato,
            MyTheme::CatppuccinMocha => Theme::CatppuccinMocha,
            MyTheme::TokyoNight => Theme::TokyoNight,
            MyTheme::TokyoNightStorm => Theme::TokyoNightStorm,
            MyTheme::TokyoNightLight => Theme::TokyoNightLight,
            MyTheme::KanagawaWave => Theme::KanagawaWave,
            MyTheme::KanagawaDragon => Theme::KanagawaDragon,
            MyTheme::KanagawaLotus => Theme::KanagawaLotus,
            MyTheme::Moonfly => Theme::Moonfly,
            MyTheme::Nightfly => Theme::Nightfly,
            MyTheme::Oxocarbon => Theme::Oxocarbon,
            MyTheme::Ferra => Theme::Ferra,
        }
    }
}
