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

    private void Theme_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        ThemeChanged?.Invoke(ThemeButtons.SelectedIndex switch
        {
            1 => ElementTheme.Light,
            2 => ElementTheme.Dark,
            _ => ElementTheme.Default,
        });
    }

    private void Refresh_Click(object sender, RoutedEventArgs e) => Client?.RequestState();

    private void SwitchUi_Click(object sender, RoutedEventArgs e)
    {
        UiPreference.Set(UiPreference.Iced);
        SettingsInfoBar.Title = "Default UI changed";
        SettingsInfoBar.Message = "The iced app will be the default front-end next time you open LibrePods.";
        SettingsInfoBar.Severity = InfoBarSeverity.Success;
        SettingsInfoBar.IsOpen = true;
    }
}
