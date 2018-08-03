# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
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

-keep class com.vuforia.** { *; }
-keep interface com.vuforia.** { *; }
-dontwarn com.vuforia.ar.**
-keep class model.Data { *; }
-keep class model.MainContent { *; }
-keep class model.TargetMetadata { *; }

-keepclassmembers class * extends java.lang.Enum {
<fields>;
public static **[] values();
public static ** valueOf(java.lang.String);
}

#-keep public enum com.thoughworks.onboarding.model.MainContent$** { **[] $VALUES; public *; }