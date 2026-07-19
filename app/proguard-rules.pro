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