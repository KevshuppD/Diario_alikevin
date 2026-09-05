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

# Firebase Firestore Serialization Keep Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class calendario.kevshupp.diariokevinali.Message { *; }
-keep class calendario.kevshupp.diariokevinali.Message { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.Pet { *; }
-keep class calendario.kevshupp.diariokevinali.Pet { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.Recipe { *; }
-keep class calendario.kevshupp.diariokevinali.Recipe { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.CalendarEvent { *; }
-keep class calendario.kevshupp.diariokevinali.CalendarEvent { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.User { *; }
-keep class calendario.kevshupp.diariokevinali.User { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.SyncMetadata { *; }
-keep class calendario.kevshupp.diariokevinali.SyncMetadata { *; }

-keepclassmembers class calendario.kevshupp.diariokevinali.MedicationItem { *; }
-keep class calendario.kevshupp.diariokevinali.MedicationItem { *; }

# Google APIs & OAuth Client
-keep class com.google.api.client.** { *; }
-keep interface com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.auth.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.auth.**
-dontwarn org.apache.commons.logging.**

# Cloudinary
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# UCrop
-keep class com.yalantis.ucrop.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# AndroidX WorkManager & Room
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keep class androidx.work.impl.** { *; }
# Osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Thor Radar Models
-keepclassmembers class calendario.kevshupp.diariokevinali.compose.RadarLocationData { *; }
-keep class calendario.kevshupp.diariokevinali.compose.RadarLocationData { *; }
-keepclassmembers class calendario.kevshupp.diariokevinali.compose.RadarPlaceZone { *; }
-keep class calendario.kevshupp.diariokevinali.compose.RadarPlaceZone { *; }