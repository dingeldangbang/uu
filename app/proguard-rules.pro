# ============================================================
# SecureGuard Enterprise — ProGuard / R8 rules
# ============================================================
# Diese Regeln zusammen mit der minifyEnabled-fähigen release-
# buildType-Variante sorgen dafür, dass Reflektion-getriebene
# Bibliotheken (Room, Hilt, Gson, Mqtt, ML Kit, Honeywell, osmdroid)
# beim R8-Minifizieren NICHT zerstört werden.
# ============================================================

# --- Reflection-Annahmen für alle Libs ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations

# --- Kotlin metadata (offiziell erforderlich) ---
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.coroutines.SafeContinuation { volatile <fields>; }

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.assisted.AssistedInject class * { *; }
-keepclassmembers @dagger.hilt.android.HiltAndroidApp class * { <init>(...); }
-keepclassmembers @dagger.hilt.android.AndroidEntryPoint class * { <init>(...); }

# --- Room ---
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# --- Gson (Datentypen + Serialisierte Datenklassen) ---
-keep class com.secureguard.enterprise.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature
-keepattributes Exceptions
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class **$TypeAdapter { <fields>; }
-dontwarn com.google.gson.**

# --- Compose (R8 weiß Bescheid, aber wir bleiben explizit) ---
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- OkHttp / Retrofit ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.flow.** { *; }

# --- ML Kit Object Detection ---
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }

# --- CameraX ---
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# --- osmdroid (OSM-Karte, benutzt Reflection für die Tile-Provider) ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keepclassmembers class * implements org.osmdroid.tileprovider.tilesource.ITileSource { *; }

# --- WorkManager ---
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.Worker { <init>(...); }
-keepclassmembers class * extends androidx.work.ListenableWorker { <init>(...); }
-keepclassmembers class * extends androidx.work.CoroutineWorker { <init>(...); }

# --- Honeywell AIDC (CT45P-Scanner) ---
-keep class com.honeywell.aidc.** { *; }
-dontwarn com.honeywell.aidc.**
-keep class com.honeywell.** { *; }
-keep class com.honeywell.decode.** { *; }

# --- Paho MQTT v3 ---
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**

# --- Accompanist Permissions ---
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# --- Coil Image Loading ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- Security/Crypto (Tink) ---
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- AndroidX Security ---
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# --- Unsere eigenen Application + Agents ---
-keep class com.secureguard.enterprise.SecureGuardApplication { *; }
-keep class com.secureguard.enterprise.services.HoneywellScanner$* { *; }
-keep class com.secureguard.enterprise.worker.SecureAgentWorker { *; }
-keep class com.secureguard.enterprise.worker.SecureAgentWorker$* { *; }
