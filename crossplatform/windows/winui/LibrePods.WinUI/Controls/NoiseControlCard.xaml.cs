using LibrePods.WinUI.Ipc;
using Microsoft.UI.Xaml.Controls;

namespace LibrePods.WinUI.Controls;

/// Noise-control mode (Off / Noise Cancellation / Transparency / Adaptive). The
/// "Off" option is gated on the daemon's allow_off flag. anc 1..4 maps to index
/// 0..3; 0 (unknown) means no selection.
public sealed partial class NoiseControlCard : UserControl
{
    public DaemonClient? Client { get; set; }

    private bool _applying;

    public NoiseControlCard()
    {
        InitializeComponent();
    }

    /// Render the selected mode + "Off" availability from a daemon Snapshot.
    public void Update(Snapshot s)
    {
        _applying = true;
        try
        {
            AncOff.IsEnabled = s.AllowOff;
            AncButtons.SelectedIndex = s.Anc is >= 1 and <= 4 ? s.Anc - 1 : -1;
        }
        finally
        {
            _applying = false;
        }
    }

    private void Anc_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (_applying) return;
        var index = AncButtons.SelectedIndex;
        if (index < 0) return;
        Client?.SetAnc((byte)(index + 1)); // 1..4
    }
}
