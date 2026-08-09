using System.IO;
using System.Text.Json;
using Microsoft.UI.Xaml;

namespace LibrePods.WinUI.Services;

/// Small persisted settings for the WinUI app, stored as JSON at
/// %LOCALAPPDATA%\LibrePods\winui-settings.json. An unpackaged app has no
/// ApplicationData.LocalSettings, so we keep our own file next to the daemon's
/// data. Best-effort — a read/write failure just falls back to defaults.
public static class AppSettings
{
    private sealed class Model
    {
        // 0 = System (Default), 1 = Light, 2 = Dark — the ThemeButtons indices.
        public int ThemeIndex { get; set; }

        // Experimental: show the heart-rate card. Off by default — the AirPods gate
        // the HR stream to Apple hosts and it does not work on Windows (they ACK the
        // enable but never send readings). See docs/windows/heart-rate.md.
        public bool EnableHeartRate { get; set; }
    }

    private static readonly object _gate = new();
    private static Model? _cache;

    private static string? Path_()
    {
        var local = System.Environment.GetFolderPath(System.Environment.SpecialFolder.LocalApplicationData);
        if (string.IsNullOrEmpty(local)) return null;
        return System.IO.Path.Combine(local, "LibrePods", "winui-settings.json");
    }

    private static Model Load()
    {
        lock (_gate)
        {
            if (_cache is not null) return _cache;
            try
            {
                var path = Path_();
                if (path is not null && File.Exists(path))
                    _cache = JsonSerializer.Deserialize<Model>(File.ReadAllText(path));
            }
            catch { }
            return _cache ??= new Model();
        }
    }

    private static void Save(Model m)
    {
        lock (_gate)
        {
            _cache = m;
            try
            {
                var path = Path_();
                if (path is null) return;
                var dir = System.IO.Path.GetDirectoryName(path);
                if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);
                File.WriteAllText(path, JsonSerializer.Serialize(m));
            }
            catch { }
        }
    }

    /// The saved theme selector index (0 System / 1 Light / 2 Dark).
    public static int ThemeIndex => Load().ThemeIndex;

    /// The saved theme as an ElementTheme for the window root.
    public static ElementTheme Theme => ThemeIndex switch
    {
        1 => ElementTheme.Light,
        2 => ElementTheme.Dark,
        _ => ElementTheme.Default,
    };

    /// Persist the chosen theme index.
    public static void SetThemeIndex(int index)
    {
        var m = Load();
        m.ThemeIndex = index;
        Save(m);
    }

    /// Whether the experimental heart-rate card is shown (off by default).
    public static bool EnableHeartRate => Load().EnableHeartRate;

    /// Persist the experimental heart-rate opt-in.
    public static void SetEnableHeartRate(bool on)
    {
        var m = Load();
        m.EnableHeartRate = on;
        Save(m);
    }
}
