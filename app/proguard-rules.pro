-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault

-keepclassmembers class pl.maxwms.data.remote.dto.** {
    <init>(...);
}
-keep class pl.maxwms.data.remote.dto.** { *; }

-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn kotlinx.serialization.**
-dontwarn com.google.mlkit.**
-dontwarn com.android.billingclient.**

-keep class com.android.billingclient.api.** { *; }
-keep class androidx.work.** { *; }
