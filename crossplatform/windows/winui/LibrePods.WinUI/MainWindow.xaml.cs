using System.IO;
using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Controls.Primitives;
using Microsoft.UI.Xaml.Media;
using Windows.Graphics;

namespace LibrePods.WinUI;

/// The Fluent control surface. It renders the daemon's Snapshot and turns user
/// input into commands. All pipe reads arrive on a background thread, so every
/// handler that touches the UI is marshalled back via DispatcherQueue. Closing
/// the window hides it to the tray rather than exiting.
public sealed partial class MainWindow : Window
{
    private readonly DaemonClient _client;

    // Suppresses command-sends while we push a fresh Snapshot into the controls
    // (setting IsOn / SelectedIndex raises Toggled / SelectionChanged synchronously).
    private bool _applyingSnapshot;

    // Extra guard specifically for the volume Slider: assigning Slider.Value fires
    // ValueChanged synchronously, and we must never echo an incoming snapshot back
    // to the daemon as a SetVolume (feedback loop).
    private bool _suppressVolume;

    // The most recent snapshot, so handlers can read sibling state (e.g. mic auto
    // needs the current mic_recording, ANC "Off" gating needs allow_off).
    private Snapshot _snapshot = new();

    public MainWindow(DaemonClient client)
    {
        _client = client;
        InitializeComponent();

        // Native Fluent look: Mica backdrop, theme-aware via system resources.
        SystemBackdrop = new MicaBackdrop();

        // Wider default so the responsive 2-column device layout shows at launch.
        AppWindow.Resize(new SizeInt32(1000, 800));
        // Enforce a minimum size — the layout breaks if the window is dragged
        // absurdly narrow (no app is usable at ~100px). Clamp on resize.
        AppWindow.Changed += (sender, e) =>
        {
            if (!e.DidSizeChange) return;
            const int minW = 420, minH = 540;
            var sz = sender.Size;
            if (sz.Width < minW || sz.Height < minH)
                sender.Resize(new SizeInt32(Math.Max(sz.Width, minW), Math.Max(sz.Height, minH)));
        };
        TrySetWindowIcon();

        // Start on the device page.
        NavView.SelectedItem = DeviceNavItem;

        // Close hides to tray (the app keeps running as an IPC client).
        AppWindow.Closing += OnClosing;

        // Daemon → UI. These fire on a background thread; marshal to the UI queue.
        _client.SnapshotReceived += OnSnapshot;
        _client.OverlayReceived += OnOverlay;
        _client.ConnectPromptReceived += OnConnectPrompt;
        _client.ConnectionChanged += OnConnectionChanged;
    }

    private void TrySetWindowIcon()
    {
        try
        {
            var ico = Path.Combine(AppContext.BaseDirectory, "Assets", "app.ico");
            if (File.Exists(ico)) AppWindow.SetIcon(ico);
        }
        catch { }
    }

    /// Show + focus the window (from the tray icon / "Open").
    public void ShowFromTray()
    {
        AppWindow.Show();
        Activate();
    }

    private void OnClosing(AppWindow sender, AppWindowClosingEventArgs args)
    {
        args.Cancel = true;   // don't destroy the window…
        AppWindow.Hide();     // …just hide it back to the tray.
    }

    // ---- Navigation --------------------------------------------------------

    private void Nav_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        var settings = args.IsSettingsSelected;
        DevicePage.Visibility = settings ? Visibility.Collapsed : Visibility.Visible;
        SettingsPage.Visibility = settings ? Visibility.Visible : Visibility.Collapsed;
    }

    // ---- Daemon events (marshalled to the UI thread) -----------------------

    private void OnSnapshot(Snapshot s) =>
        DispatcherQueue.TryEnqueue(() => ApplySnapshot(s));

    private void OnOverlay(string title, string body) =>
        DispatcherQueue.TryEnqueue(() =>
        {
            OverlayBar.Title = title;
            OverlayBar.Message = body;
            OverlayBar.Severity = InfoBarSeverity.Informational;
            OverlayBar.IsOpen = true;
        });

    private void OnConnectPrompt(string name) =>
        DispatcherQueue.TryEnqueue(() =>
        {
            ConnectPromptBar.Title = $"Connect {name}?";
            ConnectPromptBar.Message = "Nearby — click Connect to start a session.";
            var btn = new Button { Content = "Connect" };
            btn.Click += (_, _) =>
            {
                _client.Connect();
                ConnectPromptBar.IsOpen = false;
            };
            ConnectPromptBar.ActionButton = btn;
            ConnectPromptBar.IsOpen = true;
        });

    private void OnConnectionChanged(bool connected) =>
        DispatcherQueue.TryEnqueue(() =>
        {
            if (!connected)
            {
                StatusText.Text = "Waiting for daemon…";
                NavDeviceStatus.Text = "Waiting for daemon…";
            }
        });

    // ---- Render a snapshot into the controls -------------------------------

    private void ApplySnapshot(Snapshot s)
    {
        _snapshot = s;
        _applyingSnapshot = true;
        try
        {
            var name = string.IsNullOrWhiteSpace(s.DevName) ? "LibrePods" : s.DevName;
            var status = s.Connected ? "Connected" : "Disconnected";

            DeviceName.Text = name;
            StatusText.Text = status;
            StatusDot.Fill = new SolidColorBrush(
                s.Connected ? Microsoft.UI.Colors.LimeGreen : Microsoft.UI.Colors.Gray);
            ConnectButton.Visibility = s.Connected ? Visibility.Collapsed : Visibility.Visible;

            // The device NavigationViewItem mirrors the header.
            NavDeviceName.Text = name;
            NavDeviceStatus.Text = status;

            SetBattery(LeftBar, LeftText, s.Battery.Left);
            SetBattery(RightBar, RightText, s.Battery.Right);
            SetBattery(CaseBar, CaseText, s.Battery.Case);

            // Noise control: anc 1..4 → index 0..3; 0 (unknown) → no selection.
            AncOff.IsEnabled = s.AllowOff;
            AncButtons.SelectedIndex = s.Anc is >= 1 and <= 4 ? s.Anc - 1 : -1;

            // Assigning Slider.Value fires ValueChanged synchronously — guard it.
            _suppressVolume = true;
            VolumeSlider.Value = s.Volume;
            _suppressVolume = false;
            VolumeText.Text = s.Muted ? "muted" : $"{s.Volume}%";
            MuteToggle.IsChecked = s.Muted;

            ConvAwarenessSwitch.IsOn = s.ConversationalAwareness;
            AdaptiveVolumeSwitch.IsOn = s.AdaptiveVolume;
            AllowOffSwitch.IsOn = s.AllowOff;

            MicStatusText.Text = s.MicRecording ? "Microphone: recording" : "Microphone: idle";
            MicAutoSwitch.IsOn = s.AutoMode;
            MicManualToggle.IsChecked = s.MicRecording && !s.AutoMode;
        }
        finally
        {
            _applyingSnapshot = false;
        }
    }

    private static void SetBattery(ProgressBar bar, TextBlock text, byte? value)
    {
        // Treat >100 (e.g. the 0xFF "absent" sentinel) as no reading.
        if (value is byte v and <= 100)
        {
            bar.Value = v;
            bar.IsIndeterminate = false;
            text.Text = $"{v}%";
        }
        else
        {
            bar.Value = 0;
            text.Text = "—";
        }
    }

    // ---- User input → commands ---------------------------------------------

    private void Connect_Click(object sender, RoutedEventArgs e) => _client.Connect();

    private void Refresh_Click(object sender, RoutedEventArgs e) => _client.RequestState();

    private void Anc_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_applyingSnapshot) return;
        var index = AncButtons.SelectedIndex;
        if (index < 0) return;
        _client.SetAnc((byte)(index + 1)); // 1..4
    }

    private void Volume_ValueChanged(object sender, RangeBaseValueChangedEventArgs e)
    {
        if (_applyingSnapshot || _suppressVolume) return;
        _client.SetVolume((byte)e.NewValue);
    }

    private void Mute_Click(object sender, RoutedEventArgs e) => _client.ToggleMute();

    private void ConvAwareness_Toggled(object sender, RoutedEventArgs e)
    {
        if (_applyingSnapshot) return;
        _client.SetFeature(Feature.ConversationalAwareness, ConvAwarenessSwitch.IsOn);
    }

    private void AdaptiveVolume_Toggled(object sender, RoutedEventArgs e)
    {
        if (_applyingSnapshot) return;
        _client.SetFeature(Feature.AdaptiveVolume, AdaptiveVolumeSwitch.IsOn);
    }

    private void AllowOff_Toggled(object sender, RoutedEventArgs e)
    {
        if (_applyingSnapshot) return;
        _client.SetFeature(Feature.AllowOff, AllowOffSwitch.IsOn);
    }

    private void NoiseLow_Click(object sender, RoutedEventArgs e) =>
        _client.SetControl(ControlId.AdaptiveNoiseStrength, 25);
    private void NoiseMid_Click(object sender, RoutedEventArgs e) =>
        _client.SetControl(ControlId.AdaptiveNoiseStrength, 50);
    private void NoiseHigh_Click(object sender, RoutedEventArgs e) =>
        _client.SetControl(ControlId.AdaptiveNoiseStrength, 75);

    private void MicAuto_Toggled(object sender, RoutedEventArgs e)
    {
        if (_applyingSnapshot) return;
        // Keep the current manual/recording state while flipping auto.
        _client.SetMicMode(auto: MicAutoSwitch.IsOn, manual: _snapshot.MicRecording);
    }

    private void MicManual_Click(object sender, RoutedEventArgs e) =>
        // Manual toggle: turn the hi-res stream on/off, auto off.
        _client.SetMicMode(auto: false, manual: !_snapshot.MicRecording);

    // ---- Settings ----------------------------------------------------------

    private void Theme_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        // RootGrid may not exist yet while XAML is still initializing.
        if (RootGrid is null) return;
        RootGrid.RequestedTheme = ThemeButtons.SelectedIndex switch
        {
            1 => ElementTheme.Light,
            2 => ElementTheme.Dark,
            _ => ElementTheme.Default,
        };
    }

    private void SwitchUi_Click(object sender, RoutedEventArgs e)
    {
        UiPreference.Set(UiPreference.Iced);
        SettingsInfoBar.Title = "Default UI changed";
        SettingsInfoBar.Message = "The iced app will be the default front-end next time you open LibrePods.";
        SettingsInfoBar.Severity = InfoBarSeverity.Success;
        SettingsInfoBar.IsOpen = true;
    }
}
