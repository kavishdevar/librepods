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
    private TrayController? _tray;

    // The connection "island" popup. A single instance is reused so reconnect
    // spam re-populates it rather than stacking multiple popups; it self-closes
    // after its animation and clears this reference. `_podsConnected` tracks the
    // AirPods connected state so we fire only on the false→true transition.
    private IslandWindow? _island;
    private bool _podsConnected;
    // Last-seen model number (from the 0x1D metadata), so message-mode island
    // popups can show the right device render even without a full Snapshot.
    private string _lastModel = "";

    public App()
    {
        InitializeComponent();

        // Inject the SINGLE Loc instance as {StaticResource Loc} so XAML bindings and
        // code (Localize.Get -> Loc.Instance) share one object — otherwise a second
        // instance splits the culture (code in one language, bindings in another).
        // Done after InitializeComponent so Resources exists, before any page loads.
        Resources["Loc"] = Services.Loc.Instance;
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        // The main window is created hidden; closing it hides back to the tray.
        _window = new MainWindow(Daemon);

        _tray = new TrayController(
            onOpen: () => _window.ShowFromTray(),
            onQuit: ExitApp,
            client: Daemon);
        _tray.Show();

        // Register native toast notifications (Windows App SDK AppNotificationManager)
        // before the daemon starts producing overlays. A toast click ("action=open")
        // reactivates the window — marshal to the UI thread, same as the tray "Open".
        Notifier.Register(() =>
            _window.DispatcherQueue.TryEnqueue(() => _window.ShowFromTray()));

        // Surface daemon overlays as the centred floating island (not a Windows
        // toast — the toast was the corner card the user didn't want). Marshalled
        // to the UI thread.
        Daemon.OverlayReceived += (title, body) =>
            _window.DispatcherQueue.TryEnqueue(() => ShowIslandMessage(title, body));

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
                if (!string.IsNullOrEmpty(s.Model))
                {
                    _lastModel = s.Model;
                    AppSettings.SetLastModel(s.Model); // survive restarts for the connect island
                }
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

    /// Show a daemon overlay as the centred floating island (message mode).
    private void ShowIslandMessage(string title, string body)
    {
        try
        {
            if (_island is null)
            {
                _island = new IslandWindow();
                _island.Closed += (_, _) => _island = null;
            }
            _island.ShowMessage(title, body, _lastModel);
        }
        catch
        {
            _island = null;
        }
    }

    /// The tray "Quit": stop the daemon too and exit. On Windows one front-end
    /// runs at a time, and this app spawned the daemon, so quitting it should not
    /// leave a headless librepodsd lingering. (Closing the *window* only hides to
    /// the tray — the daemon stays; Quit is the full teardown.)
    public void ExitApp()
    {
        try
        {
            Daemon.Shutdown();                       // tell librepodsd to exit
            System.Threading.Thread.Sleep(150);      // let the writer flush it
        }
        catch { }
        Notifier.Unregister();
        _tray?.Dispose();
        Daemon.Dispose();
        Exit();
    }
}
