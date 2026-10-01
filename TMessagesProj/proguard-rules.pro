-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
-keep class org.telegram.tgnet.RequestDelegateInternal { *; }
-keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep public class * extends java.lang.Exception

-dontwarn org.bouncycastle.jsse.*
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keepnames class org.telegram.tgnet.TLRPC$TL_* {}
# RichMessageTextProcessor walks TL_iv objects by class name and public fields via reflection
-keepnames class org.telegram.tgnet.tl.TL_iv$* {}
-keepclassmembers class org.telegram.tgnet.tl.TL_iv$* { public <fields>; }
# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.ThickLanguageIdentifier { *; }
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }
-keepclasseswithmembernames class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni {
  native <methods>;
}

-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.messenger.AnimatedFileDrawableStream { <methods>; }
-keep class org.telegram.SQLite.SQLiteException { <methods>; }
-keep class org.telegram.tgnet.ConnectionsManager { <methods>; }
-keep class org.telegram.tgnet.NativeByteBuffer { <methods>; }
-keepnames class org.telegram.tgnet.TLRPC$TL_* {}
-keepclassmembernames class org.telegram.ui.* { <fields>; }
-keepclassmembernames class org.telegram.ui.Cells.* { <fields>; }
-keepclassmembernames class org.telegram.ui.Components.* { <fields>; }
-keep class org.telegram.ui.Components.RLottieDrawable$LottieMetadata { <fields>; }
-keep,allowshrinking,allowobfuscation class org.telegram.ui.Components.GroupCreateSpan {
    public void updateColors();
 }
 -keep,allowshrinking,allowobfuscation class org.telegram.ui.Components.Premium.GLIcon.ObjLoader {
     public <init>();
  }
-keep class * extends org.telegram.ui.Components.UItem$UItemFactory { public <init>(...); }
-keepclassmembernames class xyz.nextalone.nnngram.activity.DatacenterActivity$DatacenterCell { <fields>; }
-keepclassmembernames class xyz.nextalone.nnngram.activity.DatacenterActivity$DatacenterHeaderCell { <fields>; }
-keepclassmembernames class xyz.nextalone.nnngram.activity.MessageDetailActivity$TextDetailSimpleCell { <fields>; }
-keepclassmembernames class xyz.nextalone.nnngram.activity.PasscodeSettingActivity$AccountCell { <fields>; }
-keepclassmembernames class xyz.nextalone.nnngram.activity.ChatSettingActivity$StickerSizeCell { <fields>; }

-keepclassmembernames class androidx.core.widget.NestedScrollView {
    private android.widget.OverScroller mScroller;
    private void abortAnimatedScroll();
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

-assumenosideeffects class xyz.nextalone.nnngram.utils.Log {
    public static *** d(...);
}

-dontwarn org.slf4j.impl.StaticMDCBinder
-keep class io.ktor.client.** { *; }
-keep class io.ktor.serialization.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
   static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
   static **$* *;
}
-keepclassmembers class <2>$<3> {
   kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
   public static ** INSTANCE;
}
-keepclassmembers class <1> {
   public static <1> INSTANCE;
   kotlinx.serialization.KSerializer serializer(...);
}

-keep class xyz.nextalone.nnngram.utils.Log {
    public static *** nativeLog(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn org.jetbrains.annotations.NotNull
-dontwarn org.jetbrains.annotations.Nullable

-allowaccessmodification
-overloadaggressively
-keepattributes SourceFile,LineNumberTable,LocalVariableTable
-renamesourcefileattribute SourceFile
-obfuscationdictionary          proguard-dic.txt
-classobfuscationdictionary     proguard-dic.txt
-packageobfuscationdictionary   proguard-dic.txt

-keep class io.nano.tex.** {*;}

-keep class org.telegram.tgnet.** { *; }

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Upstream relies on proguard-android-optimize.txt for this (EnumSetting uses Enum.valueOf)
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
