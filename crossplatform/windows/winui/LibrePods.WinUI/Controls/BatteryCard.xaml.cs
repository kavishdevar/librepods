using LibrePods.WinUI.Ipc;
using Microsoft.UI.Xaml.Controls;

namespace LibrePods.WinUI.Controls;

/// Left/Right/Case battery readout. Pure presentation — renders a Snapshot, sends
/// no commands.
public sealed partial class BatteryCard : UserControl
{
    public BatteryCard()
    {
        InitializeComponent();
    }

    /// Render the three battery gauges from a daemon Snapshot.
    public void Update(Snapshot s)
    {
        SetBattery(LeftBar, LeftText, s.Battery.Left, s.Battery.LeftCharging);
        SetBattery(RightBar, RightText, s.Battery.Right, s.Battery.RightCharging);
        SetBattery(CaseBar, CaseText, s.Battery.Case, s.Battery.CaseCharging);
    }

    private static void SetBattery(ProgressBar bar, TextBlock text, byte? value, bool charging)
    {
        // Treat >100 (e.g. the 0xFF "absent" sentinel) as no reading.
        if (value is byte v and <= 100)
        {
            bar.Value = v;
            bar.IsIndeterminate = false;
            // Append a bolt when charging (status byte 0x01 / 0x05).
            text.Text = charging ? $"{v}%  ⚡" : $"{v}%";
        }
        else
        {
            bar.Value = 0;
            text.Text = "—";
        }
    }
}
