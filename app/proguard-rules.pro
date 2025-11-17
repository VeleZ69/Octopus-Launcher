# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontwarn kotlin.reflect.jvm.internal.**
-keep class kotlin.Metadata { *; }

# Keep data classes
-keep class com.octopus.launcher.data.** { *; }
-keepclassmembers class com.octopus.launcher.data.** {
    <init>(...);
}

# Keep ViewModels
-keep class com.octopus.launcher.ui.viewmodel.** { *; }
-keepclassmembers class com.octopus.launcher.ui.viewmodel.** {
    <init>(...);
}

# Keep Composables
-keep @androidx.compose.runtime.Composable class com.octopus.launcher.ui.** { *; }
-keepclassmembers class com.octopus.launcher.ui.** {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep AndroidX Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.tv.material3.** { *; }
-keep class androidx.tv.foundation.** { *; }

# Keep Navigation Compose
-keep class androidx.navigation.** { *; }
-keep class androidx.navigation.compose.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment
-keepclassmembers class * extends androidx.navigation.NavArgs
-keepclassmembers class * implements androidx.navigation.NavArgs {
    <init>(...);
}

# Keep Material Icons Extended
-keep class androidx.compose.material.icons.** { *; }

# Keep SplashScreen
-keep class androidx.core.splashscreen.** { *; }

# Keep Lifecycle components
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** {
    <init>(...);
}

# Keep Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# Keep Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**
-keep class coil.compose.** { *; }
-keep class coil.decode.** { *; }
-keep class coil.fetch.** { *; }
-keep class coil.request.** { *; }
-keep class coil.transform.** { *; }
-keep class coil.size.** { *; }
-keep class coil.memory.** { *; }
-keep class coil.disk.** { *; }
-keep class coil.intercept.** { *; }
-keep class coil.util.** { *; }
# Keep Coil's ImageLoader and related classes
-keep class coil.ImageLoader { *; }
-keep class coil.ImageLoaderFactory { *; }
-keep class coil.ImageLoader$Builder { *; }
# Keep Coil's Compose components
-keep class coil.compose.AsyncImagePainter { *; }
-keep class coil.compose.AsyncImagePainter$State { *; }
-keep class coil.compose.SubcomposeAsyncImageScope { *; }
# Keep Coil's memory cache key (Parcelable)
-keep class coil.memory.MemoryCache$Key { *; }
-keepclassmembers class coil.memory.MemoryCache$Key {
    <init>(...);
}
# Keep Coil's disk cache
-keep class coil.disk.DiskCache { *; }
-keep class coil.disk.DiskCache$Snapshot { *; }
-keep class coil.disk.DiskCache$Editor { *; }
# Keep Coil's fetchers
-keep class coil.fetch.Fetcher { *; }
-keep class coil.fetch.Fetcher$Factory { *; }
# Keep Coil's decoders
-keep class coil.decode.Decoder { *; }
-keep class coil.decode.Decoder$Factory { *; }
# Keep Coil's request models
-keep class coil.request.ImageRequest { *; }
-keep class coil.request.ImageResult { *; }
# Keep parameterized types for Coil
-keepattributes Signature
-keepattributes *Annotation*

# Keep Application class
-keep class com.octopus.launcher.**Application { *; }
-keep class com.octopus.launcher.MainActivity { *; }

# Keep all Activities
-keep class * extends android.app.Activity
-keep class * extends androidx.fragment.app.Fragment

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}