using LibrePods.WinUI.Ipc;
using Microsoft.UI.Xaml.Controls;

namespace LibrePods.WinUI.Pages;

/// The device view: header + transient InfoBars + the responsive 2-column card
/// grid. It owns no daemon logic beyond fanning a Snapshot out to each card and
/// rendering the overlay / connect-prompt InfoBars.
public sealed partial class DevicePage : UserControl
{
    private DaemonClient? _client;

    /// The daemon client, propagated to the header + every command-sending card.
    public DaemonClient? Client
    {
        get => _client;
        set
        {
            _client = value;
            Header.Client = value;
            VolumeCard.Client = value;
            AdaptiveNoiseCard.Client = value;
            NoiseControlCard.Client = value;
            FeaturesCard.Client = value;
            MicCard.Client = value;
            HeartRateCard.Client = value;
        }
    }

    public DevicePage()
    {
        InitializeComponent();
    }

    /// Fan a fresh Snapshot out to the header and every card that renders state.
    /// (AdaptiveNoiseCard is stateless — send-only — so it is skipped.)
    public void Update(Snapshot s)
    {
        Header.Update(s);
        BatteryCard.Update(s);
        VolumeCard.Update(s);
        NoiseControlCard.Update(s);
        FeaturesCard.Update(s);
        MicCard.Update(s);
        HeartRateCard.Update(s);
    }

    /// Show a transient daemon overlay in the InfoBar.
    public void ShowOverlay(string title, string body)
    {
        OverlayBar.Title = title;
        OverlayBar.Message = body;
        OverlayBar.Severity = InfoBarSeverity.Informational;
        OverlayBar.IsOpen = true;
    }

    /// Show the "nearby — connect?" prompt with a Connect action.
    public void ShowConnectPrompt(string name)
    {
        ConnectPromptBar.Title = $"Connect {name}?";
        ConnectPromptBar.Message = "Nearby — click Connect to start a session.";
        var btn = new Button { Content = "Connect" };
        btn.Click += (_, _) =>
        {
            _client?.Connect();
            ConnectPromptBar.IsOpen = false;
        };
        ConnectPromptBar.ActionButton = btn;
        ConnectPromptBar.IsOpen = true;
    }

    /// The events pipe dropped — surface it in the header.
    public void ShowWaitingForDaemon() => Header.ShowWaitingForDaemon();
}
