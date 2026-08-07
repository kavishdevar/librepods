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
        SetBattery(LeftBar, LeftText, s.Battery.Left);
        SetBattery(RightBar, RightText, s.Battery.Right);
        SetBattery(CaseBar, CaseText, s.Battery.Case);
    }

    private static void SetBattery(ProgressBar bar, TextBlock text, byte? value)
    {
        // Treat >100 (e.g. the 0xFF "absent" sentinel) as no reading.
        if (value is byte v and <= 100)
        {
            bar.Value = v;
            bar.IsIndeterminate = false;
            text.Text = $"{v}%";
        }
        else
        {
            bar.Value = 0;
            text.Text = "—";
        }
    }
}
