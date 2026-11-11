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
-keepattributes SourceFile,LineNumberTable

# Moshi and Data Models serialization safety
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.example.data.model.** { *; }

# Room SQLite Entities and DAOs
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Retrofit & OkHttp
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
