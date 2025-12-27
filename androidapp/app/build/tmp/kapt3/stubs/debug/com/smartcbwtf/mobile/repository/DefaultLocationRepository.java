package com.smartcbwtf.mobile.repository;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000b\n\u0002\b\n\b\u0007\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB!\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010\u000b\u001a\u00020\fH\u0016J\u0016\u0010\r\u001a\u0010\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f\u0018\u00010\u000eH\u0016J\b\u0010\u0010\u001a\u00020\u0011H\u0016J\u0018\u0010\u0012\u001a\u00020\f2\u0006\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u000fH\u0016J\u0010\u0010\u0015\u001a\u00020\f2\u0006\u0010\u0016\u001a\u00020\u0011H\u0016J(\u0010\u0017\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u000f2\u0006\u0010\u0014\u001a\u00020\u000f2\b\u0010\u0018\u001a\u0004\u0018\u00010\u000fH\u0096@\u00a2\u0006\u0002\u0010\u0019R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultLocationRepository;", "Lcom/smartcbwtf/mobile/repository/LocationRepository;", "context", "Landroid/content/Context;", "locationApi", "Lcom/smartcbwtf/mobile/network/api/LocationApi;", "authTokenStore", "Lcom/smartcbwtf/mobile/storage/AuthTokenStore;", "(Landroid/content/Context;Lcom/smartcbwtf/mobile/network/api/LocationApi;Lcom/smartcbwtf/mobile/storage/AuthTokenStore;)V", "prefs", "Landroid/content/SharedPreferences;", "clearLocationData", "", "getLastKnownLocation", "Lkotlin/Pair;", "", "hasLocationConsent", "", "saveLastKnownLocation", "latitude", "longitude", "setLocationConsent", "granted", "syncLocation", "accuracy", "(DDLjava/lang/Double;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "Companion", "app_debug"})
public final class DefaultLocationRepository implements com.smartcbwtf.mobile.repository.LocationRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.network.api.LocationApi locationApi = null;
    @org.jetbrains.annotations.NotNull()
    private final com.smartcbwtf.mobile.storage.AuthTokenStore authTokenStore = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "LocationRepository";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "location_prefs";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_LAST_LAT = "last_lat";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_LAST_LON = "last_lon";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_LOCATION_CONSENT = "location_consent";
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.repository.DefaultLocationRepository.Companion Companion = null;
    
    @javax.inject.Inject()
    public DefaultLocationRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.LocationApi locationApi, @org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.storage.AuthTokenStore authTokenStore) {
        super();
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public java.lang.Object syncLocation(double latitude, double longitude, @org.jetbrains.annotations.Nullable()
    java.lang.Double accuracy, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public kotlin.Pair<java.lang.Double, java.lang.Double> getLastKnownLocation() {
        return null;
    }
    
    @java.lang.Override()
    public void saveLastKnownLocation(double latitude, double longitude) {
    }
    
    @java.lang.Override()
    public boolean hasLocationConsent() {
        return false;
    }
    
    @java.lang.Override()
    public void setLocationConsent(boolean granted) {
    }
    
    @java.lang.Override()
    public void clearLocationData() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\t"}, d2 = {"Lcom/smartcbwtf/mobile/repository/DefaultLocationRepository$Companion;", "", "()V", "KEY_LAST_LAT", "", "KEY_LAST_LON", "KEY_LOCATION_CONSENT", "PREFS_NAME", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}