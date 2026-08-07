using LibrePods.WinUI.Ipc;
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

        // Begin talking to the daemon (background reader + writer loops).
        Daemon.Start();

        // Show the window on launch (closing it hides back to the tray). Autostart
        // can later pass a "--tray"/"--minimized" arg to start hidden instead.
        _window.Activate();
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
