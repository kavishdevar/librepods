//! Which desktop UI the tray's "Open App" launches. Two front-ends talk to the
//! same daemon over IPC: the cross-platform **iced** app (`librepods.exe`) and a
//! native **WinUI 3** client (`librepods-winui.exe`). The choice is a per-user
//! preference persisted next to the other LibrePods data.

use std::path::PathBuf;

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum Ui {
    Iced,
    WinUi,
}

impl Ui {
    fn as_str(self) -> &'static str {
        match self {
            Ui::Iced => "iced",
            Ui::WinUi => "winui",
        }
    }
    /// The executable that renders this UI (a sibling of the tray exe).
    fn exe(self) -> &'static str {
        match self {
            Ui::Iced => "librepods.exe",
            Ui::WinUi => "librepods-winui.exe",
        }
    }
}

fn pref_path() -> Option<PathBuf> {
    let base = std::env::var("LOCALAPPDATA").ok()?;
    Some(PathBuf::from(base).join("LibrePods").join("ui.pref"))
}

/// The saved choice. On Windows we prefer the native WinUI client by default; the
/// iced app is still launched if it's explicitly chosen, or as a fallback when the
/// WinUI client isn't installed (see `launch`).
pub fn get() -> Ui {
    match pref_path().and_then(|p| std::fs::read_to_string(p).ok()) {
        Some(s) if s.trim() == "iced" => Ui::Iced,
        _ => Ui::WinUi,
    }
}

/// Persist the user's choice.
pub fn set(ui: Ui) {
    if let Some(p) = pref_path() {
        let _ = std::fs::write(p, ui.as_str());
    }
}

/// Launch the currently-preferred UI (a sibling exe of the tray). Falls back to
/// the iced app if the WinUI client isn't installed yet.
pub fn launch() {
    let Ok(exe) = std::env::current_exe() else {
        return;
    };
    let Some(dir) = exe.parent() else {
        return;
    };
    let choice = get();
    let target = dir.join(choice.exe());
    if choice == Ui::WinUi && !target.exists() {
        // WinUI client not installed — fall back so "Open App" always works.
        let _ = std::process::Command::new(dir.join(Ui::Iced.exe())).spawn();
        return;
    }
    let _ = std::process::Command::new(target).spawn();
}
