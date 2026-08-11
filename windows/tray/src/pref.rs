//! Launches the desktop UI from the tray's "Open App". On Windows the only
//! front-end is the native **WinUI 3** client (`librepods-winui.exe`), a sibling
//! exe of the tray that talks to the daemon over IPC. (The iced app is Linux-only.)

/// Launch the WinUI client (a sibling exe of the tray).
pub fn launch() {
    let Ok(exe) = std::env::current_exe() else {
        return;
    };
    let Some(dir) = exe.parent() else {
        return;
    };
    let _ = std::process::Command::new(dir.join("librepods-winui.exe")).spawn();
}
