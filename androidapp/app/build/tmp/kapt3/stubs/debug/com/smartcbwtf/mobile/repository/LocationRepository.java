package com.smartcbwtf.mobile.repository;

/**
 * Repository for GPS location tracking.
 * Handles syncing to backend and local storage of last known location.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000b\n\u0002\b\t\bf\u0018\u00002\u00020\u0001J\b\u0010\u0002\u001a\u00020\u0003H&J\u0016\u0010\u0004\u001a\u0010\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u0006\u0018\u00010\u0005H&J\b\u0010\u0007\u001a\u00020\bH&J\u0018\u0010\t\u001a\u00020\u00032\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u0006H&J\u0010\u0010\f\u001a\u00020\u00032\u0006\u0010\r\u001a\u00020\bH&J(\u0010\u000e\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u00062\u0006\u0010\u000b\u001a\u00020\u00062\b\u0010\u000f\u001a\u0004\u0018\u00010\u0006H\u00a6@\u00a2\u0006\u0002\u0010\u0010\u00a8\u0006\u0011"}, d2 = {"Lcom/smartcbwtf/mobile/repository/LocationRepository;", "", "clearLocationData", "", "getLastKnownLocation", "Lkotlin/Pair;", "", "hasLocationConsent", "", "saveLastKnownLocation", "latitude", "longitude", "setLocationConsent", "granted", "syncLocation", "accuracy", "(DDLjava/lang/Double;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface LocationRepository {
    
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object syncLocation(double latitude, double longitude, @org.jetbrains.annotations.Nullable()
    java.lang.Double accuracy, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion);
    
    @org.jetbrains.annotations.Nullable()
    public abstract kotlin.Pair<java.lang.Double, java.lang.Double> getLastKnownLocation();
    
    public abstract void saveLastKnownLocation(double latitude, double longitude);
    
    public abstract boolean hasLocationConsent();
    
    public abstract void setLocationConsent(boolean granted);
    
    public abstract void clearLocationData();
}