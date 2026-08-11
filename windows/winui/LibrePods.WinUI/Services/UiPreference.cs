using System.IO;

namespace LibrePods.WinUI.Services;

/// Reads/writes the shared front-end preference at
/// %LOCALAPPDATA%\LibrePods\ui.pref — the same file the Rust tray's pref.rs uses
/// to decide which UI its "Open App" launches ("iced" or "winui"). Writing "iced"
/// makes the cross-platform app the default front-end for next login.
public static class UiPreference
{
    public const string Iced = "iced";
    public const string WinUi = "winui";

    private static string? PrefPath()
    {
        var local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        if (string.IsNullOrEmpty(local)) return null;
        return Path.Combine(local, "LibrePods", "ui.pref");
    }

    /// The saved choice, defaulting to "iced" (always present, cross-platform).
    public static string Get()
    {
        try
        {
            var path = PrefPath();
            if (path is not null && File.Exists(path))
            {
                var s = File.ReadAllText(path).Trim();
                if (s == WinUi) return WinUi;
            }
        }
        catch { }
        return Iced;
    }

    /// Persist the user's choice, creating the LibrePods folder if needed.
    public static void Set(string value)
    {
        try
        {
            var path = PrefPath();
            if (path is null) return;
            var dir = Path.GetDirectoryName(path);
            if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
            File.WriteAllText(path, value);
        }
        catch { }
    }
}
