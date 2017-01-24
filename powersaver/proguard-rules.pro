# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/zsigui/software/android/android-sdk-linux/tools/proguard/proguard-android.txt
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

-optimizationpasses 5
-dontusemixedcaseclassnames
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
# 不做预检验，preverify是proguard的四个步骤之一
# Android不需要preverify，去掉这一步可以加快混淆速度
-dontpreverify
#避免混淆泛型
-keepattributes Signature
# 保留所有的本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留了继承自Activity、Application这些类的子类
# 因为这些子类有可能被外部调用
-keep public class * extends android.app.Activity { public *; }
-keep public class * extends android.app.Application { public *; }
-keep public class * extends android.app.Service { public *; }
-keep public class * extends android.accessibilityservice.AccessibilityService { public *; }
-keep public class * extends android.content.BroadcastReceiver { public *; }
-keep public class * extends android.content.ContentProvider { public *; }
-keep public class * extends android.view.View { public *; }

-keep public class com.luna.powersaver.gp.PowerSaver { public *; }
-keep public interface com.luna.powersaver.gp.PowerSaver$StateChangeCallback { public *; }


-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** w(...);
    public static *** e(...);
}

# 对R文件下的所有类及其方法，都不能被混淆
-keepclassmembers class **.R$* { *; }