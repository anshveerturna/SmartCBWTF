# Keep runtime annotations/metadata used by Retrofit, Gson, Hilt, and Room.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault,InnerClasses,EnclosingMethod

# Keep API interfaces and DTOs used by Retrofit/Gson reflection.
-keep class com.smartcbwtf.mobile.network.api.** { *; }
-keep class com.smartcbwtf.mobile.network.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Room entities and generated database classes.
-keep class com.smartcbwtf.mobile.database.entity.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }

# Keep WorkManager workers discovered by class name.
-keep class * extends androidx.work.ListenableWorker { *; }

# Keep Hilt-generated wiring.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

-dontwarn javax.annotation.**

# Release builds must not retain logs containing GPS, QR, Bluetooth, or auth/session metadata.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}
