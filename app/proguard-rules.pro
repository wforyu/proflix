-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class com.proflix.** {
    *** Companion;
}
-keepclasseswithmembers class com.proflix.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room Database Entities
-keep class com.proflix.database.entity.** { *; }

# Domain Models
-keep class com.proflix.provider.domain.model.** { *; }
-keep class com.proflix.provider.domain.ProviderType { *; }

# Provider implementations
-keep class com.proflix.provider.data.provider.** { *; }

# Network
-keep class com.proflix.core.network.** { *; }
-keep class com.proflix.network.interceptor.** { *; }

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Jsoup
-keep class org.jsoup.** { *; }

# Coil
-keep class coil.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep data classes used in serialization
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.JsonProperty <fields>;
}
