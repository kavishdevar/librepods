using System.Windows.Input;
using H.NotifyIcon;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Imaging;

namespace LibrePods.WinUI.Tray;

/// The system-tray presence, built on H.NotifyIcon.WinUI. Left-click (or the
/// "Open" menu item) shows the main window; "Quit" exits the app. Because the
/// TaskbarIcon is created outside the visual tree, we call ForceCreate().
public sealed class TrayIcon : IDisposable
{
    private readonly TaskbarIcon _icon;

    public TrayIcon(Action onOpen, Action onQuit)
    {
        var menu = new MenuFlyout();

        var open = new MenuFlyoutItem { Text = "Open" };
        open.Click += (_, _) => onOpen();

        var quit = new MenuFlyoutItem { Text = "Quit" };
        quit.Click += (_, _) => onQuit();

        menu.Items.Add(open);
        menu.Items.Add(new MenuFlyoutSeparator());
        menu.Items.Add(quit);

        _icon = new TaskbarIcon
        {
            ToolTipText = "LibrePods",
            ContextFlyout = menu,
            // SecondWindow is the recommended context-menu mode for unpackaged
            // WinUI 3 apps (the classic PopupMenu path needs a visible window).
            ContextMenuMode = ContextMenuMode.SecondWindow,
            IconSource = new BitmapImage(new Uri("ms-appx:///Assets/tray.ico")),
            LeftClickCommand = new RelayCommand(onOpen),
            NoLeftClickDelay = true,
        };
    }

    public void Show() => _icon.ForceCreate();

    /// Show a tray balloon for a daemon overlay event.
    public void ShowBalloon(string title, string body)
    {
        try
        {
            _icon.ShowNotification(title, body);
        }
        catch
        {
            // Notifications are best-effort; never let one crash the app.
        }
    }

    public void Dispose() => _icon.Dispose();

    /// Minimal ICommand so the tray's left-click can invoke an Action.
    private sealed class RelayCommand(Action execute) : ICommand
    {
        public event EventHandler? CanExecuteChanged { add { } remove { } }
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => execute();
    }
}
