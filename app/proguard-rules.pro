# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/ubuntu/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-keep class com.google.firebase.** { *; }
-keep class com.eleven.store.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
