# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/ahd/android-sdk-linux_86/tools/proguard/proguard-android.txt
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

-dontwarn okio.**
-dontwarn com.google.android.gms.internal.zzhu
-keep class com.adarshahd.indianrailinfo.models.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class sun.misc.Unsafe { *; }
-dontwarn com.baasbox.android.**
-keep class com.baasbox.android.** { *; }

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable,Signature
