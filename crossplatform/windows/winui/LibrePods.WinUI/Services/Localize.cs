using Microsoft.Windows.ApplicationModel.Resources;

namespace LibrePods.WinUI.Services;

/// Thin wrapper over the Windows App SDK resource loader for strings chosen at
/// *runtime* (connection status, mute/unmute, mic state, tray menu, toasts).
///
/// Static UI labels are localized declaratively by <c>x:Uid</c> in XAML — the
/// framework resolves <c>&lt;Uid&gt;.Text/.Content/.Header</c> against the same
/// resource file. This helper covers the strings that XAML can't, because code
/// picks them at runtime. Every key lives in <c>Strings/en-US/Resources.resw</c>;
/// add a language by copying that folder (e.g. <c>Strings/pt-PT</c>) and
/// translating the values — the OS language then selects the match.
internal static class Localize
{
    // ResourceLoader is thread-affine-safe for reads; a single shared instance
    // over the app's default resource map ("Resources", built from the .resw).
    private static readonly ResourceLoader Loader = new();

    /// Look up a resource string by key. Returns the key itself if the lookup
    /// fails or is empty, so a missing translation is visible rather than blank.
    public static string Get(string key)
    {
        try
        {
            var s = Loader.GetString(key);
            return string.IsNullOrEmpty(s) ? key : s;
        }
        catch
        {
            return key;
        }
    }

    /// Look up a composite string and fill its <c>{0}</c>… placeholders.
    public static string Get(string key, params object[] args) =>
        string.Format(Get(key), args);
}
