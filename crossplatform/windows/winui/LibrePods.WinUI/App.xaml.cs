using LibrePods.WinUI.Ipc;
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
            onQuit: ExitApp);
        _tray.Show();

        // Surface daemon overlays as a tray balloon as well as the in-app InfoBar.
        Daemon.OverlayReceived += (title, body) =>
            _window.DispatcherQueue.TryEnqueue(() => _tray?.ShowBalloon(title, body));

        // Begin talking to the daemon (background reader + writer loops).
        Daemon.Start();

        // Intentionally NOT calling _window.Activate(): start hidden to tray.
    }

    /// The tray "Quit": tear down the tray and exit. We do NOT send `shutdown`
    /// here — closing this front-end should leave the daemon (and any other UI)
    /// running. The tray's own Quit is what stops the daemon.
    public void ExitApp()
    {
        _tray?.Dispose();
        Daemon.Dispose();
        Exit();
    }
}
