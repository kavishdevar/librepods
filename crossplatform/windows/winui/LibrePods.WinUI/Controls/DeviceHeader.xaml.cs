using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace LibrePods.WinUI.Controls;

/// Device header card: product image + name, connection dot/status, and an inline
/// Connect link shown while disconnected. Renders a Snapshot; the Connect link
/// forwards to the daemon.
public sealed partial class DeviceHeader : UserControl
{
    public DaemonClient? Client { get; set; }

    public DeviceHeader()
    {
        InitializeComponent();
    }

    /// Render the header from a daemon Snapshot (name + connection state).
    public void Update(Snapshot s)
    {
        DeviceName.Text = string.IsNullOrWhiteSpace(s.DevName) ? "LibrePods" : s.DevName;
        StatusText.Text = Localize.Get(s.Connected ? "Status_Connected" : "Status_Disconnected");
        StatusDot.Fill = new SolidColorBrush(
            s.Connected ? Microsoft.UI.Colors.LimeGreen : Microsoft.UI.Colors.Gray);
        ConnectButton.Visibility = s.Connected ? Visibility.Collapsed : Visibility.Visible;
    }

    /// The events pipe dropped — reflect that we're no longer hearing from the daemon.
    public void ShowWaitingForDaemon() => StatusText.Text = Localize.Get("Status_WaitingForDaemon");

    private void Connect_Click(object sender, RoutedEventArgs e) => Client?.Connect();
}
