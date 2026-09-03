# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep the JavaScript bridge interface methods so WebView can call them via reflection.
-keepclassmembers class com.gonzalo.webviewinterface.data.bridge.WebAppInterface {
    public *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
