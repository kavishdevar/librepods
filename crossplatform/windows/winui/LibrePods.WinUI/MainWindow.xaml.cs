using System.IO;
using LibrePods.WinUI.Ipc;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Windows.Graphics;

namespace LibrePods.WinUI;

/// The Fluent shell: a NavigationView that content-swaps between the DevicePage
/// and SettingsPage UserControls. It owns no card logic — it wires the daemon
/// client into the pages, forwards daemon events (marshalled to the UI thread) to
/// the device page, and applies the settings page's theme choice to the window
/// root. Closing the window hides it to the tray rather than exiting.
public sealed partial class MainWindow : Window
{
    private readonly DaemonClient _client;

    public MainWindow(DaemonClient client)
    {
        _client = client;
        InitializeComponent();

        // Native Fluent look: Mica backdrop, theme-aware via system resources.
        SystemBackdrop = new MicaBackdrop();

        // Hand the client to the pages: DevicePage fans it out to its cards; the
        // SettingsPage uses it for "Refresh state".
        DevicePageView.Client = _client;
        SettingsPageView.Client = _client;

        // The settings theme picker owns the choice; the window root owns the theme.
        SettingsPageView.ThemeChanged += theme =>
        {
            if (RootGrid is not null) RootGrid.RequestedTheme = theme;
        };

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
        DevicePageView.Visibility = settings ? Visibility.Collapsed : Visibility.Visible;
        SettingsPageView.Visibility = settings ? Visibility.Visible : Visibility.Collapsed;
    }

    // ---- Daemon events (marshalled to the UI thread) -----------------------

    private void OnSnapshot(Snapshot s) =>
        DispatcherQueue.TryEnqueue(() =>
        {
            // The device NavigationViewItem mirrors the header (name only).
            NavDeviceName.Text = string.IsNullOrWhiteSpace(s.DevName) ? "LibrePods" : s.DevName;
            DevicePageView.Update(s);
        });

    private void OnOverlay(string title, string body) =>
        DispatcherQueue.TryEnqueue(() => DevicePageView.ShowOverlay(title, body));

    private void OnConnectPrompt(string name) =>
        DispatcherQueue.TryEnqueue(() => DevicePageView.ShowConnectPrompt(name));

    private void OnConnectionChanged(bool connected) =>
        DispatcherQueue.TryEnqueue(() =>
        {
            if (!connected) DevicePageView.ShowWaitingForDaemon();
        });
}
