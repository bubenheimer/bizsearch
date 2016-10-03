# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/uli/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-optimizationpasses 10
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# Disable verbose and debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
}
-assumenosideeffects class android.util.Log {
    public static int d(...);
}

#google-api-client
# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection

-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault
