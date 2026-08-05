use crate::platform::AppPaths;
use std::path::PathBuf;

/// Linux backend: follows the XDG Base Directory spec, with `$HOME`-relative
/// fallbacks. Behaviour is identical to the pre-refactor `utils` helpers.
pub struct LinuxPlatform;

impl AppPaths for LinuxPlatform {
    fn devices_path() -> PathBuf {
        let data_dir = std::env::var("XDG_DATA_HOME")
            .unwrap_or_else(|_| format!("{}/.local/share", std::env::var("HOME").unwrap_or_default()));
        PathBuf::from(data_dir)
            .join("librepods")
            .join("devices.json")
    }

    fn preferences_path() -> PathBuf {
        let config_dir = std::env::var("XDG_CONFIG_HOME")
            .unwrap_or_else(|_| format!("{}/.config", std::env::var("HOME").unwrap_or_default()));
        PathBuf::from(config_dir)
            .join("librepods")
            .join("preferences.json")
    }

    fn app_settings_path() -> PathBuf {
        let home = std::env::var("HOME").unwrap_or_default();

        let config_dir =
            std::env::var("XDG_CONFIG_HOME").unwrap_or_else(|_| format!("{}/.config", home));

        let data_dir =
            std::env::var("XDG_DATA_HOME").unwrap_or_else(|_| format!("{}/.local/share", home));

        let new_path = PathBuf::from(&config_dir)
            .join("librepods")
            .join("app_settings.json");

        let old_path = PathBuf::from(&data_dir).join("app_settings.json");

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
}
