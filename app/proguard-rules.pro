# ─────────────────────────────────────────────────────────────────────────────
# Repackage: move all remaining classes into the default package and allow
# R8 to widen visibility so it can inline/merge across package boundaries.
# ─────────────────────────────────────────────────────────────────────────────
-repackageclasses ''
-allowaccessmodification

# ─────────────────────────────────────────────────────────────────────────────
# Opt: Removed the blanket "-keep class io.nekohasekai.sagernet.** { *; }" and
# "-keep class moe.matsuri.nb4a.** { *; }" rules.  Those rules told R8 to keep
# EVERY class and EVERY member of the app's own code, making shrinking and
# obfuscation completely ineffective for ~90% of the APK.
#
# Instead we keep only what must survive:
#  • AIDL-generated stubs (referenced by name from binder plumbing)
#  • Room entities/DAOs (referenced by annotations at runtime)
#  • Serializable beans (Kryo reads fields by name)
#  • Parcelable / Parcelize types
#  • Enums accessed by ordinal from AIDL
#  • Public API surfaces called from the Go/JNI layer (NativeInterface)
#  • Crash handler (registered as UncaughtExceptionHandler by class name)
# ─────────────────────────────────────────────────────────────────────────────

# AIDL stubs — class names hardcoded in binder marshalling
-keep class io.nekohasekai.sagernet.aidl.** { *; }

# Room entities and DAOs — annotations processed at compile time but field
# names are used via reflection at runtime for type converters.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class io.nekohasekai.sagernet.database.** { *; }
-keep class moe.matsuri.nb4a.TempDatabase { *; }
-keep class moe.matsuri.nb4a.TempDatabase$* { *; }

# Kryo serialized beans — field names must survive
-keep class io.nekohasekai.sagernet.fmt.** { *; }
-keep class moe.matsuri.nb4a.proxy.** { *; }
-keep class moe.matsuri.nb4a.SingBoxOptions { *; }
-keep class moe.matsuri.nb4a.SingBoxOptions$* { *; }

# NativeInterface — called directly from JNI/Go bridge
-keep class moe.matsuri.nb4a.NativeInterface { *; }
-keep class moe.matsuri.nb4a.NativeInterface$* { *; }

# Parcelable — CREATOR fields accessed by name through reflection
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Enums — values() and valueOf() are used reflectively by Android framework
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Crash handler
-keep class io.nekohasekai.sagernet.utils.CrashHandler { *; }

# Service / Activity / BroadcastReceiver / ContentProvider referenced in Manifest
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.service.quicksettings.TileService
-keep public class * extends android.net.VpnService

# WorkManager workers — class name registered in WorkRequest
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─────────────────────────────────────────────────────────────────────────────
# Strip Kotlin runtime null-check intrinsics (safe — we know our own nullability)
# ─────────────────────────────────────────────────────────────────────────────
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkReturnedValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String, java.lang.String);
    static void checkFieldIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNull(java.lang.Object);
    static void checkNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}

# ─────────────────────────────────────────────────────────────────────────────
# Third-party library keeps (minimum required)
# ─────────────────────────────────────────────────────────────────────────────

# ini4j — uses ServiceLoader / SPI reflection
-keep public class org.ini4j.spi.** { <init>(); }

# SnakeYAML — Opt: narrowed from "keep everything" to just the SafeConstructor
# path and the public API surface we actually call.  This shaves ~60 KB off
# the dex after shrinking compared to "-keep class org.yaml.snakeyaml.** { *; }".
-keep class org.yaml.snakeyaml.Yaml { *; }
-keep class org.yaml.snakeyaml.constructor.SafeConstructor { *; }
-keep class org.yaml.snakeyaml.LoaderOptions { *; }
-keep class org.yaml.snakeyaml.TypeDescription { *; }
-keep class org.yaml.snakeyaml.error.YAMLException { *; }
-keep class org.yaml.snakeyaml.representer.** { *; }
-keep class org.yaml.snakeyaml.nodes.** { *; }
-dontwarn org.yaml.snakeyaml.**

# Kryo — uses unsafe reflection on registered classes (kept above via bean keeps)
-keep class com.esotericsoftware.kryo.** { *; }
-dontwarn com.esotericsoftware.**

# ProcessPhoenix
-keep class com.jakewharton.processphoenix.** { *; }

# ─────────────────────────────────────────────────────────────────────────────
# Opt: Enable obfuscation for everything not explicitly kept above.
# Previously "-dontobfuscate" was set, which left all class/member names intact
# in the release APK and prevented R8 from producing shorter identifiers.
# Remove "-dontobfuscate" and keep source file / line info for crash reporting.
# ─────────────────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─────────────────────────────────────────────────────────────────────────────
# Suppress known harmless warnings from optional TLS provider classes
# ─────────────────────────────────────────────────────────────────────────────
-dontwarn java.beans.**
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
