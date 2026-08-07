using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Popup;
using LibrePods.WinUI.Services;
using LibrePods.WinUI.Tray;
using Microsoft.UI.Xaml;

namespace LibrePods.WinUI;

/// The app is primarily a tray client of `librepodsd`. It starts hidden to the
/// tray: the DaemonClient connects (spawning the daemon if needed), the main
/// window is created but not shown, and the tray icon drives Open/Quit.
public partial class App : Application
{
    public DaemonClient Daemon { get; } = new();

    private MainWindow? _window;
    private TrayIcon? _tray;

    // The connection "island" popup. A single instance is reused so reconnect
    // spam re-populates it rather than stacking multiple popups; it self-closes
    // after its animation and clears this reference. `_podsConnected` tracks the
    // AirPods connected state so we fire only on the false→true transition.
    private IslandWindow? _island;
    private bool _podsConnected;

    public App()
    {
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        // The main window is created hidden; closing it hides back to the tray.
        _window = new MainWindow(Daemon);

        _tray = new TrayIcon(
            onOpen: () => _window.ShowFromTray(),
            onQuit: ExitApp,
            client: Daemon);
        _tray.Show();

        // Register native toast notifications (Windows App SDK AppNotificationManager)
        // before the daemon starts producing overlays. A toast click ("action=open")
        // reactivates the window — marshal to the UI thread, same as the tray "Open".
        Notifier.Register(() =>
            _window.DispatcherQueue.TryEnqueue(() => _window.ShowFromTray()));

        // Surface daemon overlays as a native toast (persists in the Action Center,
        // clickable) as well as the in-app InfoBar. Marshalled to the UI thread.
        Daemon.OverlayReceived += (title, body) =>
            _window.DispatcherQueue.TryEnqueue(() => Notifier.Show(title, body));

        // Drive the tray's tooltip / menu / icon badge from daemon state. Snapshots
        // arrive on a background thread; marshal to the UI thread (same as the window).
        Daemon.SnapshotReceived += s =>
            _window.DispatcherQueue.TryEnqueue(() => _tray?.UpdateSnapshot(s));

        // Show the connection island on the AirPods connect transition only
        // (Snapshot.Connected false→true). Tracking the snapshot's connected flag
        // — rather than the pipe-level ConnectionChanged — means a daemon pipe
        // reconnect (which re-pushes the same connected=true snapshot) does not
        // re-fire the popup. Marshalled to the UI thread.
        Daemon.SnapshotReceived += s =>
            _window.DispatcherQueue.TryEnqueue(() =>
            {
                var now = s.Connected;
                if (now && !_podsConnected) ShowIsland(s);
                _podsConnected = now;
            });

        // Begin talking to the daemon (background reader + writer loops).
        Daemon.Start();

        // Show the window on launch (closing it hides back to the tray). Autostart
        // can later pass a "--tray"/"--minimized" arg to start hidden instead.
        _window.Activate();
    }

    /// Create (or reuse) the single island window and play the connect popup.
    /// Must run on the UI thread. Fully guarded — a popup failure never crashes
    /// the app; at worst there is simply no island.
    private void ShowIsland(Snapshot snapshot)
    {
        try
        {
            if (_island is null)
            {
                _island = new IslandWindow();
                // Self-closes after its slide-out — drop the reference so the next
                // connection builds a fresh one.
                _island.Closed += (_, _) => _island = null;
            }
            _island.ShowConnected(snapshot);
        }
        catch
        {
            _island = null;
        }
    }

    /// The tray "Quit": tear down the tray and exit. We do NOT send `shutdown`
    /// here — closing this front-end should leave the daemon (and any other UI)
    /// running. The tray's own Quit is what stops the daemon.
    public void ExitApp()
    {
        Notifier.Unregister();
        _tray?.Dispose();
        Daemon.Dispose();
        Exit();
    }
}
