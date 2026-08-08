using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media.Imaging;

namespace LibrePods.WinUI.Popup;

/// The visual of the connection "island": AirPods case image on the left, device
/// name + L/R/Case battery on the right. Pure presentation — it has no timers and
/// no window logic (that lives in IslandWindow); it only renders a Snapshot.
public sealed partial class IslandView : UserControl
{
    public IslandView()
    {
        InitializeComponent();

        // Kick off the product-render animation once laid out: a one-shot scale-in
        // entrance, then a continuous gentle float. Wrapped so an animation failure
        // can never break the popup (worst case: a static image).
        Loaded += (_, _) =>
        {
            try { Entrance.Begin(); } catch { }
            try { FloatLoop.Begin(); } catch { }
        };
    }

    /// Populate the card from a daemon Snapshot (name + battery + model image).
    public void Apply(Snapshot s)
    {
        // Connection-card mode: render + battery, no message line.
        MessageBody.Visibility = Visibility.Collapsed;
        DeviceImage.Visibility = Visibility.Visible;
        BatteryRow.Visibility = Visibility.Visible;

        DeviceName.Text = string.IsNullOrWhiteSpace(s.DevName) ? Localize.Get("Island_DefaultName") : s.DevName;

        try
        {
            DeviceImage.Source = new BitmapImage(new Uri(ImageForName(s.DevName)));
        }
        catch
        {
            // Leave the XAML default (airpods.png) if the URI fails for any reason.
        }

        SetBattery(LeftItem, LeftBar, LeftText, s.Battery.Left, s.Battery.LeftCharging);
        SetBattery(RightItem, RightBar, RightText, s.Battery.Right, s.Battery.RightCharging);
        SetBattery(CaseItem, CaseBar, CaseText, s.Battery.Case, s.Battery.CaseCharging);
    }

    /// Message mode: show a title + body (e.g. an ANC change), hiding the render
    /// and battery — so daemon overlays render as the centred island instead of a
    /// Windows toast.
    public void ApplyMessage(string title, string body)
    {
        DeviceName.Text = string.IsNullOrWhiteSpace(title) ? Localize.Get("Island_DefaultName") : title;
        MessageBody.Text = body ?? "";
        MessageBody.Visibility = Visibility.Visible;
        DeviceImage.Visibility = Visibility.Collapsed;
        BatteryRow.Visibility = Visibility.Collapsed;
    }

    /// Show a battery component only when it carries a real reading. Values >100
    /// (e.g. the 0xFF "absent" sentinel) collapse the whole item, matching the
    /// main window's handling but hiding empty bars on the compact island.
    private static void SetBattery(FrameworkElement item, ProgressBar bar, TextBlock text, byte? value, bool charging)
    {
        if (value is byte v and <= 100)
        {
            bar.Value = v;
            // Append a bolt when charging (status byte 0x01 / 0x05).
            text.Text = charging ? $"{v}% ⚡" : $"{v}%";
            item.Visibility = Visibility.Visible;
        }
        else
        {
            bar.Value = 0;
            text.Text = "—";
            item.Visibility = Visibility.Collapsed;
        }
    }

    /// Best-effort map from the device name to one of the AirPods case images
    /// bundled in Assets. The daemon only reports a free-text dev_name (no model
    /// id), so we match on substrings and fall back to the generic airpods.png.
    /// Uses the "_case.png" art because the island is a "connected" card.
    private static string ImageForName(string? devName)
    {
        const string root = "ms-appx:///Assets/";
        const string fallback = root + "airpods.png";

        var n = (devName ?? string.Empty).ToLowerInvariant();
        if (n.Length == 0) return fallback;

        // No Max case art bundled — fall back to the generic image.
        if (n.Contains("max")) return fallback;

        if (n.Contains("pro"))
        {
            if (n.Contains("3")) return root + "airpods_pro_3_case.png";
            if (n.Contains("1")) return root + "airpods_pro_1_case.png";
            // "AirPods Pro" / "Pro 2" → Pro 2 art (the common case).
            return root + "airpods_pro_2_case.png";
        }

        if (n.Contains("4")) return root + "airpods_4_case.png";
        if (n.Contains("3")) return root + "airpods_3_case.png";
        if (n.Contains("2")) return root + "airpods_2_case.png";
        if (n.Contains("1")) return root + "airpods_1_case.png";

        return fallback;
    }
}
