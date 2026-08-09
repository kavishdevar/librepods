using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Globalization;
using System.Xml.Linq;

namespace LibrePods.WinUI.Services;

/// Runtime localization service — an Angular-style translate service. All UI text
/// binds to this indexer (via the {loc:Loc} markup extension or Get() in code);
/// changing the culture raises the indexer PropertyChanged so every binding
/// re-evaluates and the whole UI switches language LIVE, no restart. This sidesteps
/// x:Uid / PrimaryLanguageOverride, which don't work on the unpackaged build.
///
/// The strings come from the same Strings/<culture>/Resources.resw files (embedded
/// as "loc.<culture>" and parsed into a culture -> key -> value map at startup), so
/// translations aren't duplicated.
public sealed class Loc : INotifyPropertyChanged
{
    public const string Fallback = "en-US";
    private static readonly string[] Cultures = { "en-US", "pt-PT", "fr-FR", "es-ES" };

    public static Loc Instance { get; private set; } = new();

    private readonly Dictionary<string, Dictionary<string, string>> _map =
        new(StringComparer.OrdinalIgnoreCase);
    private string _culture;

    public event PropertyChangedEventHandler? PropertyChanged;

    public Loc()
    {
        foreach (var c in Cultures) _map[c] = Load(c);
        _culture = ResolveInitial();
        Instance = this; // one instance (declared in App.xaml resources); code shares it
    }

    /// The active BCP-47 culture (e.g. "pt-PT").
    public string CurrentCulture => _culture;

    /// Localized value for a resw key (e.g. "SettingsTitle.Text"), current culture,
    /// falling back to en-US then the key itself.
    public string this[string key]
    {
        get
        {
            if (_map.TryGetValue(_culture, out var d) && d.TryGetValue(key, out var v)) return v;
            if (_map.TryGetValue(Fallback, out var f) && f.TryGetValue(key, out var fv)) return fv;
            return key;
        }
    }

    public string Get(string key) => this[key];
    public string Get(string key, params object[] args) => string.Format(this[key], args);

    /// Switch the UI language live. "" / unknown → the system default. No-op if
    /// unchanged; otherwise notifies every binding to re-fetch.
    public void SetCulture(string tag)
    {
        var c = string.IsNullOrWhiteSpace(tag) ? SystemDefault() : tag;
        if (!_map.ContainsKey(c)) c = Fallback;
        if (string.Equals(c, _culture, StringComparison.OrdinalIgnoreCase)) return;
        _culture = c;
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Item[]"));
    }

    private static Dictionary<string, string> Load(string culture)
    {
        var dict = new Dictionary<string, string>(StringComparer.Ordinal);
        try
        {
            var asm = typeof(Loc).Assembly;
            using var stream = asm.GetManifestResourceStream($"loc.{culture}");
            if (stream is null) return dict;
            var doc = XDocument.Load(stream);
            foreach (var data in doc.Root?.Elements("data") ?? System.Linq.Enumerable.Empty<XElement>())
            {
                var name = (string?)data.Attribute("name");
                var value = data.Element("value")?.Value;
                if (!string.IsNullOrEmpty(name) && value is not null)
                {
                    dict[name!] = value;
                    // Also index under a dot-free alias so XAML bindings can use
                    // [SettingsTitle_Text] (a dot in the indexer path is ambiguous).
                    var alias = name!.Replace('.', '_');
                    if (alias != name) dict[alias] = value;
                }
            }
        }
        catch { }
        return dict;
    }

    private string ResolveInitial()
    {
        var saved = AppSettings.LanguageTag;
        if (!string.IsNullOrWhiteSpace(saved) && _map.ContainsKey(saved)) return saved;
        var sys = SystemDefault();
        return _map.ContainsKey(sys) ? sys : Fallback;
    }

    private static string SystemDefault()
    {
        try
        {
            return CultureInfo.CurrentUICulture.TwoLetterISOLanguageName switch
            {
                "pt" => "pt-PT",
                "fr" => "fr-FR",
                "es" => "es-ES",
                _ => "en-US",
            };
        }
        catch { return Fallback; }
    }
}
