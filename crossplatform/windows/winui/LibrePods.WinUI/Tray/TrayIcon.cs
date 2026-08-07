using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Runtime.InteropServices;
using System.Windows.Input;
using H.NotifyIcon;
using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Imaging;

namespace LibrePods.WinUI.Tray;

/// The system-tray presence, built on H.NotifyIcon.WinUI. Left-click (or the
/// "Open" menu item) shows the main window; "Quit" exits the app. The tray is a
/// first-class control surface: the tooltip and context menu are driven live from
/// the daemon's Snapshot (via UpdateSnapshot, which the App marshals to the UI
/// thread), and the icon renders the lower earbud's battery % as a number badge
/// (MagicPods-style) so the level is visible without opening the window.
///
/// Because the TaskbarIcon is created outside the visual tree, we call ForceCreate().
public sealed class TrayIcon : IDisposable
{
    private readonly TaskbarIcon _icon;
    private readonly DaemonClient _client;

    // Context-menu items updated from each snapshot.
    private readonly MenuFlyoutItem _headerItem;
    private readonly MenuFlyoutItem _batteryItem;
    private readonly RadioMenuFlyoutItem _ancOff;
    private readonly RadioMenuFlyoutItem _ancNc;
    private readonly RadioMenuFlyoutItem _ancTransparency;
    private readonly RadioMenuFlyoutItem _ancAdaptive;
    private readonly MenuFlyoutItem _muteItem;

    // Guards against IsChecked assignments re-issuing commands while we sync the
    // menu to an incoming snapshot.
    private bool _updatingMenu;

    // Icon lifetime: the plain LibrePods icon is persistent; each number badge is a
    // freshly generated GDI icon whose HICON we must destroy when it's replaced.
    private readonly Icon? _baseIcon;
    private Icon? _generatedIcon;
    private IntPtr _generatedHandle;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool DestroyIcon(IntPtr hIcon);

    public TrayIcon(Action onOpen, Action onQuit, DaemonClient client)
    {
        _client = client;

        var menu = new MenuFlyout();
        // The SecondWindow host is a tiny window, and a flyout is by default
        // *constrained to its XamlRoot's window bounds* — so long labels ("Noise
        // Cancellation", the device name) get clipped to that narrow window.
        // ShouldConstrainToRootBounds=false lets the flyout size to its content and
        // spill beyond the host window; the MinWidth floor keeps it tidy.
        menu.ShouldConstrainToRootBounds = false;
        var presenterStyle = new Style(typeof(MenuFlyoutPresenter));
        presenterStyle.Setters.Add(new Setter(FrameworkElement.MinWidthProperty, 240.0));
        menu.MenuFlyoutPresenterStyle = presenterStyle;

        _headerItem = new MenuFlyoutItem { Text = "LibrePods", IsEnabled = false };
        _batteryItem = new MenuFlyoutItem { Text = Localize.Get("Tray_NoBatteryData"), IsEnabled = false };

        var ancLabel = new MenuFlyoutItem { Text = Localize.Get("Tray_NoiseControl"), IsEnabled = false };
        _ancOff = new RadioMenuFlyoutItem { Text = Localize.Get("Anc_Off"), GroupName = "TrayAnc" };
        _ancNc = new RadioMenuFlyoutItem { Text = Localize.Get("Anc_NoiseCancellation"), GroupName = "TrayAnc" };
        _ancTransparency = new RadioMenuFlyoutItem { Text = Localize.Get("Anc_Transparency"), GroupName = "TrayAnc" };
        _ancAdaptive = new RadioMenuFlyoutItem { Text = Localize.Get("Anc_Adaptive"), GroupName = "TrayAnc" };
        _ancOff.Click += (_, _) => OnAncClick(1);
        _ancNc.Click += (_, _) => OnAncClick(2);
        _ancTransparency.Click += (_, _) => OnAncClick(3);
        _ancAdaptive.Click += (_, _) => OnAncClick(4);

        _muteItem = new MenuFlyoutItem { Text = Localize.Get("Action_Mute") };
        _muteItem.Click += (_, _) => _client.ToggleMute();

        var open = new MenuFlyoutItem { Text = Localize.Get("Action_Open") };
        open.Click += (_, _) => onOpen();

        var quit = new MenuFlyoutItem { Text = Localize.Get("Action_Quit") };
        quit.Click += (_, _) => onQuit();

        menu.Items.Add(_headerItem);
        menu.Items.Add(_batteryItem);
        menu.Items.Add(new MenuFlyoutSeparator());
        menu.Items.Add(ancLabel);
        menu.Items.Add(_ancOff);
        menu.Items.Add(_ancNc);
        menu.Items.Add(_ancTransparency);
        menu.Items.Add(_ancAdaptive);
        menu.Items.Add(new MenuFlyoutSeparator());
        menu.Items.Add(_muteItem);
        menu.Items.Add(new MenuFlyoutSeparator());
        menu.Items.Add(open);
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

        // Load the plain icon from disk for the "no number" fallback.
        try
        {
            var path = Path.Combine(AppContext.BaseDirectory, "Assets", "tray.ico");
            if (File.Exists(path)) _baseIcon = new Icon(path);
        }
        catch { _baseIcon = null; }
    }

    public void Show() => _icon.ForceCreate();

    private void OnAncClick(byte mode)
    {
        // Ignore IsChecked assignments made while syncing to a snapshot.
        if (_updatingMenu) return;
        _client.SetAnc(mode);
    }

    /// Refresh the tooltip, context menu and icon badge from the latest snapshot.
    /// MUST be called on the UI thread (the App marshals via DispatcherQueue).
    public void UpdateSnapshot(Snapshot s)
    {
        var name = string.IsNullOrWhiteSpace(s.DevName) ? "LibrePods" : s.DevName;

        var left = Present(s.Battery.Left);
        var right = Present(s.Battery.Right);
        var @case = Present(s.Battery.Case);

        // ---- Tooltip ----
        string tip;
        if (!s.Connected)
        {
            tip = $"{name} — {Localize.Get("Status_Disconnected")}";
        }
        else
        {
            var parts = new List<string>();
            if (left is byte l) parts.Add($"L {l}%");
            if (right is byte r) parts.Add($"R {r}%");
            if (@case is byte c) parts.Add($"{Localize.Get("Tray_CaseShort")} {c}%");
            tip = parts.Count > 0 ? $"{name} — {string.Join("  ", parts)}" : $"{name} — {Localize.Get("Status_Connected")}";
        }
        // Win32 NOTIFYICONDATA tooltip caps at 127 chars.
        _icon.ToolTipText = tip.Length > 127 ? tip[..127] : tip;

        // ---- Context menu ----
        _updatingMenu = true;
        try
        {
            _headerItem.Text = name;

            var battParts = new List<string>();
            if (left is byte bl) battParts.Add($"L {bl}%");
            if (right is byte br) battParts.Add($"R {br}%");
            if (@case is byte bc) battParts.Add($"{Localize.Get("Tray_CaseShort")} {bc}%");
            _batteryItem.Text = battParts.Count > 0 ? string.Join(" · ", battParts) : Localize.Get("Tray_NoBatteryData");

            // "Off" is only selectable when the daemon reports allow_off.
            _ancOff.IsEnabled = s.AllowOff;
            _ancOff.IsChecked = s.Anc == 1;
            _ancNc.IsChecked = s.Anc == 2;
            _ancTransparency.IsChecked = s.Anc == 3;
            _ancAdaptive.IsChecked = s.Anc == 4;

            _muteItem.Text = Localize.Get(s.Muted ? "Action_Unmute" : "Action_Mute");
        }
        finally
        {
            _updatingMenu = false;
        }

        // ---- Icon badge: lower present bud (min of L/R), else plain icon ----
        UpdateIconBadge(LowerBud(left, right));
    }

    /// A battery reading is "present" only when it's a real 0..100 value; the
    /// daemon uses 0xFF (255) as an absent sentinel.
    private static byte? Present(byte? value) =>
        value is byte v and <= 100 ? v : null;

    private static byte? LowerBud(byte? left, byte? right)
    {
        if (left is byte l && right is byte r) return Math.Min(l, r);
        return left ?? right;
    }

    private void UpdateIconBadge(byte? level)
    {
        if (level is not byte v)
        {
            // Disconnected / no bud reading → plain LibrePods icon.
            if (_baseIcon is not null) SetIcon(_baseIcon, IntPtr.Zero);
            return;
        }

        try
        {
            var (icon, handle) = RenderNumberIcon(v.ToString(), LightTaskbar());
            SetIcon(icon, handle);
        }
        catch
        {
            // Rendering must never crash the tray; fall back to the plain icon.
            if (_baseIcon is not null) SetIcon(_baseIcon, IntPtr.Zero);
        }
    }

    /// Assign a new tray icon and release the previously generated one (never the
    /// persistent base icon, which is passed with a zero handle).
    private void SetIcon(Icon icon, IntPtr generatedHandle)
    {
        try { _icon.Icon = icon; } catch { }

        // The shell has copied the icon by now; free the prior generated one.
        _generatedIcon?.Dispose();
        if (_generatedHandle != IntPtr.Zero) DestroyIcon(_generatedHandle);

        _generatedIcon = generatedHandle == IntPtr.Zero ? null : icon;
        _generatedHandle = generatedHandle;
    }

    /// Draw the number centered on a 32×32 transparent bitmap and convert it to an
    /// Icon. Returns the Icon plus its backing HICON (to DestroyIcon on replace).
    private static (Icon icon, IntPtr handle) RenderNumberIcon(string text, bool lightTaskbar)
    {
        // Render large (64px) for a crisp downscale in the tray, and MEASURE-fit the
        // font so 1, 2 or 3 digits ("5", "58", "100") always fit inside the box —
        // a fixed size clipped "58" to "5".
        const int size = 64;
        using var bmp = new Bitmap(size, size);
        using (var g = Graphics.FromImage(bmp))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
            g.Clear(Color.Transparent);

            using var brush = new SolidBrush(lightTaskbar
                ? Color.FromArgb(255, 32, 32, 32)
                : Color.White);
            using var fmt = new StringFormat
            {
                Alignment = StringAlignment.Center,
                LineAlignment = StringAlignment.Center,
            };

            // Shrink the face until the text fits within ~92% of the icon.
            float emSize = 54f;
            while (emSize > 12f)
            {
                using var probe = new Font("Segoe UI", emSize, FontStyle.Bold, GraphicsUnit.Pixel);
                var m = g.MeasureString(text, probe);
                if (m.Width <= size * 0.92f && m.Height <= size * 0.98f) break;
                emSize -= 2f;
            }
            using var font = new Font("Segoe UI", emSize, FontStyle.Bold, GraphicsUnit.Pixel);
            g.DrawString(text, font, brush, new RectangleF(0, 0, size, size), fmt);
        }

        var handle = bmp.GetHicon();
        // Icon.FromHandle does not own the handle; caller destroys it later.
        var icon = Icon.FromHandle(handle);
        return (icon, handle);
    }

    /// True when the taskbar uses the light theme (→ draw dark text). Defaults to
    /// false (dark taskbar, white text) — the Windows 11 default — if unreadable.
    private static bool LightTaskbar()
    {
        try
        {
            using var key = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(
                @"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize");
            return key?.GetValue("SystemUsesLightTheme") is int i && i != 0;
        }
        catch
        {
            return false;
        }
    }

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

    public void Dispose()
    {
        _icon.Dispose();
        _generatedIcon?.Dispose();
        if (_generatedHandle != IntPtr.Zero) DestroyIcon(_generatedHandle);
        _baseIcon?.Dispose();
    }

    /// Minimal ICommand so the tray's left-click can invoke an Action.
    private sealed class RelayCommand(Action execute) : ICommand
    {
        public event EventHandler? CanExecuteChanged { add { } remove { } }
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => execute();
    }
}
