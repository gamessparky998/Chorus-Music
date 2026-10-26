-keepclassmembers class pushkar.chorus.music.utils.sabr.EjsNTransformSolver$SolverWebView {
    @android.webkit.JavascriptInterface public *;
}
-keepclassmembers class pushkar.chorus.music.utils.cipher.CipherWebView {
    @android.webkit.JavascriptInterface public *;
}
-keepclassmembers class pushkar.chorus.music.utils.potoken.PoTokenWebView {
    @android.webkit.JavascriptInterface public *;
}

-keep class pushkar.chorus.music.utils.cipher.** { *; }
-keep class pushkar.chorus.music.utils.sabr.** { *; }
-keep class pushkar.chorus.music.utils.potoken.** { *; }
-keepclassmembers class * {
    void resume(...);
    void resumeWithException(...);
}

-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    static <1>$Companion Companion;
}

-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclasseswithmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclasseswithmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

-dontwarn javax.servlet.ServletContainerInitializer
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class org.schabi.newpipe.extractor.services.youtube.protos.** { *; }
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.javascript.engine.** { *; }
-dontwarn org.mozilla.javascript.JavaToJSONConverters
-dontwarn org.mozilla.javascript.tools.**
-keep class javax.script.** { *; }
-dontwarn javax.script.**
-keep class jdk.dynalink.** { *; }
-dontwarn jdk.dynalink.**

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    #public static int i(...);
    #public static int w(...);
    #public static int e(...);
}

-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor

-keep class com.atilika.kuromoji.** { *; }

-keep class pushkar.chorus.music.models.PersistQueue { *; }
-keep class pushkar.chorus.music.models.PersistPlayerState { *; }
-keep class pushkar.chorus.music.models.QueueData { *; }
-keep class pushkar.chorus.music.models.QueueType { *; }
-keep class pushkar.chorus.music.playback.queues.** { *; }

-keepclassmembers class * implements java.io.Serializable {
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
}

-dontwarn com.yalantis.ucrop**
-keep class com.yalantis.ucrop** { *; }
-keep interface com.yalantis.ucrop** { *; }
-keep class pushkar.chorus.music.cast.** { *; }
-keep class com.google.android.gms.cast.** { *; }
-keep class androidx.mediarouter.** { *; }

-dontwarn com.google.re2j.**

-keep class pushkar.chorus.music.recognition.VibraSignature { *; }
-keepclassmembers class pushkar.chorus.music.recognition.VibraSignature {
    native <methods>;
}

-dontwarn kotlin.Metadata
-dontwarn kotlinx.metadata.**
-dontwarn kotlin.reflect.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

-keepattributes *Annotation*
-keepattributes Signature,InnerClasses,EnclosingMethod

-keep class androidx.compose.foundation.style.** { *; }
-keepclassmembers class androidx.compose.foundation.style.** { *; }
-dontwarn androidx.compose.foundation.style.**



