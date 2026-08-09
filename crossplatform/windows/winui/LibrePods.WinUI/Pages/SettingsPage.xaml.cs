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

    /// Raised when the experimental heart-rate opt-in changes, so the host can
    /// refresh the DevicePage card visibility live.
    public event Action? HeartRateVisibilityChanged;

    private bool _applyingLang;

    public SettingsPage()
    {
        InitializeComponent();

        // App version in the About card (for bug reports).
        try
        {
            var v = System.Reflection.Assembly.GetExecutingAssembly().GetName().Version;
            if (v is not null) VersionText.Text = $"v{v.Major}.{v.Minor}.{v.Build}";
        }
        catch { }

        InitLanguageCombo();
    }

    /// Select the persisted UI language on load, without firing the restart hint.
    private void InitLanguageCombo()
    {
        _applyingLang = true;
        try
        {
            var tag = AppSettings.LanguageTag;
            foreach (var obj in LanguageCombo.Items)
                if (obj is ComboBoxItem item && (string)(item.Tag ?? "") == tag)
                {
                    LanguageCombo.SelectedItem = item;
                    break;
                }
            if (LanguageCombo.SelectedItem is null) LanguageCombo.SelectedIndex = 0; // System
        }
        finally { _applyingLang = false; }
    }

    private void Language_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_applyingLang) return;
        var tag = (LanguageCombo.SelectedItem as ComboBoxItem)?.Tag as string ?? "";
        AppSettings.SetLanguageTag(tag);
        // Nudge the override now (helps newly-created windows); already-rendered
        // x:Uid text only re-resolves on a full restart — hence the hint.
        try { Windows.Globalization.ApplicationLanguages.PrimaryLanguageOverride = tag; }
        catch { }
        LanguageRestartBar.IsOpen = true;
    }

    /// Set the theme radio to the persisted choice on startup, without re-firing
    /// ThemeChanged (the host applies the saved theme itself).
    public void InitThemeSelector(int index)
    {
        if (index >= 0 && index <= 2) ThemeButtons.SelectedIndex = index;
    }

    /// Set the experimental heart-rate toggle to the persisted value on startup
    /// (without re-firing the change event).
    public void InitHeartRateSetting() => HeartRateSetting.IsOn = AppSettings.EnableHeartRate;

    private void HeartRateSetting_Toggled(object sender, RoutedEventArgs e)
    {
        AppSettings.SetEnableHeartRate(HeartRateSetting.IsOn); // persist across restarts
        HeartRateVisibilityChanged?.Invoke();
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
