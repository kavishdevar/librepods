use crate::platform::AppPaths;
use directories::ProjectDirs;
use std::path::PathBuf;

/// Windows backend: resolves to `%APPDATA%\librepods` via the `directories`
/// crate. No legacy migration exists on Windows (fresh platform).
pub struct WindowsPlatform;

fn project_dirs() -> ProjectDirs {
    ProjectDirs::from("", "", "librepods")
        .expect("could not determine platform config/data directories")
}

impl AppPaths for WindowsPlatform {
    fn devices_path() -> PathBuf {
        project_dirs().data_dir().join("devices.json")
    }

    fn preferences_path() -> PathBuf {
        project_dirs().config_dir().join("preferences.json")
    }

    fn app_settings_path() -> PathBuf {
        project_dirs().config_dir().join("app_settings.json")
    }
}
