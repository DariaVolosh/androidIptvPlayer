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

-keep class com.example.iptvplayer.retrofit.data.ChannelsAndEpgAuthResponse
-keep class com.example.iptvplayer.retrofit.data.AuthInfo
-keep class com.example.iptvplayer.retrofit.services.ChannelsAndEpgService
-keep class com.example.iptvplayer.di.RetrofitModule
-keep class com.example.iptvplayer.di.ApplicationComponent
-keep class tv.danmaku.ijk.media.player.IjkMediaPlayer
-dontwarn com.squareup.okhttp.CipherSuite
-dontwarn com.squareup.okhttp.ConnectionSpec
-dontwarn com.squareup.okhttp.TlsVersion
-dontwarn tv.danmaku.ijk.media.player.IMediaPlayer$OnInfoListener
-dontwarn tv.danmaku.ijk.media.player.IMediaPlayer$OnPreparedListener
-dontwarn tv.danmaku.ijk.media.player.IMediaPlayer
-dontwarn tv.danmaku.ijk.media.player.IjkMediaPlayer
-dontwarn tv.danmaku.ijk.media.player.misc.IMediaDataSource
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 -keepclassmembers,allowobfuscation class * {
   @com.google.gson.annotations.SerializedName <fields>;
 }

-keepclassmembers class com.example.iptvplayer.retrofit.data.** { <fields>; }

-keep class tv.danmaku.ijk.** { *; }
-keepnames class tv.danmaku.ijk.**
-keep class tv.danmaku.ijk.media.player.IjkMediaPlayer {
    native <methods>;
}
-keep class tv.danmaku.ijk.media.player.** { *; }
-keepnames class tv.danmaku.ijk.media.player.**

-keep class androidx.datastore.*.** {*;}

-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }