# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class me.kavishdevar.librepods.utils.KotlinModule { *; }

# Room discovers generated database implementations and their no-argument constructors by
# convention/reflection. R8 full mode otherwise sees WorkDatabase_Impl() as unused and removes
# it, which makes WorkManager's startup provider crash only in release builds.
-keep class * extends androidx.room.RoomDatabase {
    public <init>();
}

# Keep release builds quiet and avoid paying to construct high-volume protocol debug strings.
# Warning and error logs remain available for actionable field diagnostics.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
