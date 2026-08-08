using LibrePods.WinUI.Ipc;
using LibrePods.WinUI.Services;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;

namespace LibrePods.WinUI.Controls;

/// Device header card: product image + name (editable via a pencil), connection
/// dot/status, an inline Connect link while disconnected, and a Disconnect button
/// while connected. Renders a Snapshot; actions forward to the daemon.
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
        // Don't clobber the name while the user is editing it.
        if (NameEdit.Visibility != Visibility.Visible)
            DeviceName.Text = string.IsNullOrWhiteSpace(s.DevName) ? "LibrePods" : s.DevName;

        StatusText.Text = Localize.Get(s.Connected ? "Status_Connected" : "Status_Disconnected");
        StatusDot.Fill = new SolidColorBrush(
            s.Connected ? Microsoft.UI.Colors.LimeGreen : Microsoft.UI.Colors.Gray);
        ConnectButton.Visibility = s.Connected ? Visibility.Collapsed : Visibility.Visible;
        // Rename + Disconnect only make sense while connected.
        RenameBtn.Visibility = s.Connected ? Visibility.Visible : Visibility.Collapsed;
        DisconnectButton.Visibility = s.Connected ? Visibility.Visible : Visibility.Collapsed;
        if (!s.Connected) ExitEdit();
    }

    /// The events pipe dropped — reflect that we're no longer hearing from the daemon.
    public void ShowWaitingForDaemon() => StatusText.Text = Localize.Get("Status_WaitingForDaemon");

    private void Connect_Click(object sender, RoutedEventArgs e) => Client?.Connect();
    private void Disconnect_Click(object sender, RoutedEventArgs e) => Client?.Disconnect();

    // ---- Rename ------------------------------------------------------------

    private void Rename_Click(object sender, RoutedEventArgs e)
    {
        NameBox.Text = DeviceName.Text;
        NameView.Visibility = Visibility.Collapsed;
        NameEdit.Visibility = Visibility.Visible;
        NameBox.Focus(FocusState.Programmatic);
        NameBox.SelectAll();
    }

    private void Submit_Click(object sender, RoutedEventArgs e)
    {
        var name = NameBox.Text?.Trim() ?? "";
        if (name.Length > 0)
        {
            Client?.SetName(name);
            DeviceName.Text = name; // optimistic
        }
        ExitEdit();
    }

    private void Cancel_Click(object sender, RoutedEventArgs e) => ExitEdit();

    private void NameBox_KeyDown(object sender, KeyRoutedEventArgs e)
    {
        if (e.Key == Windows.System.VirtualKey.Enter) Submit_Click(sender, e);
        else if (e.Key == Windows.System.VirtualKey.Escape) ExitEdit();
    }

    private void ExitEdit()
    {
        NameEdit.Visibility = Visibility.Collapsed;
        NameView.Visibility = Visibility.Visible;
    }
}
