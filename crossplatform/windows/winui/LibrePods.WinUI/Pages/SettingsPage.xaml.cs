using System;
using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace LibrePods.WinUI.Pages;

/// The settings view: theme picker, default front-end switch, refresh, and About.
/// Theme changes are surfaced to the host (which owns the themed root) via the
/// ThemeChanged event; everything else is self-contained.
public sealed partial class SettingsPage : UserControl
{
    /// The daemon client, used by "Refresh state".
    public DaemonClient? Client { get; set; }

    /// Raised when the user picks a theme. The host applies it to the window root.
    public event Action<ElementTheme>? ThemeChanged;

    public SettingsPage()
    {
        InitializeComponent();
    }

    /// Set the theme radio to the persisted choice on startup, without re-firing
    /// ThemeChanged (the host applies the saved theme itself).
    public void InitThemeSelector(int index)
    {
        if (index >= 0 && index <= 2) ThemeButtons.SelectedIndex = index;
    }

    private void Theme_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        var index = ThemeButtons.SelectedIndex;
        AppSettings.SetThemeIndex(index); // persist across restarts
        ThemeChanged?.Invoke(index switch
        {
            1 => ElementTheme.Light,
            2 => ElementTheme.Dark,
            _ => ElementTheme.Default,
        });
    }

    /// Fill the (hidden-by-default) device-info card from the 0x1D metadata.
    public void UpdateDeviceInfo(Snapshot s)
    {
        ModelText.Text = FriendlyModel(s.Model);
        FirmwareText.Text = string.IsNullOrWhiteSpace(s.Firmware) ? "—" : s.Firmware;
        SerialText.Text = string.IsNullOrWhiteSpace(s.Serial) ? "—" : s.Serial;
    }

    /// Map a known model number to a friendly name, else show the raw number.
    private static string FriendlyModel(string model) => model switch
    {
        "" => "—",
        "A3064" => "AirPods Pro 3 (A3064)",
        "A2968" or "A2931" or "A2699" => $"AirPods Pro 2 ({model})",
        "A2084" or "A2083" => $"AirPods Pro ({model})",
        _ => model,
    };

    // The device info (incl. serial) is covered by a frosted blur; the eye toggles it.
    private void RevealInfo_Toggled(object sender, RoutedEventArgs e)
    {
        if (InfoBlur is not null)
            InfoBlur.Visibility = RevealInfo.IsChecked == true ? Visibility.Collapsed : Visibility.Visible;
    }

    private void Refresh_Click(object sender, RoutedEventArgs e) => Client?.RequestState();

    private void SwitchUi_Click(object sender, RoutedEventArgs e)
    {
        UiPreference.Set(UiPreference.Iced);
        SettingsInfoBar.Title = Localize.Get("Settings_DefaultUiChanged_Title");
        SettingsInfoBar.Message = Localize.Get("Settings_DefaultUiChanged_Message");
        SettingsInfoBar.Severity = InfoBarSeverity.Success;
        SettingsInfoBar.IsOpen = true;
    }
}
