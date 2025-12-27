package com.smartcbwtf.mobile.storage;

/**
 * Store for app configuration values fetched from backend.
 * Values are cached locally and used throughout the app.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0011\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010/\u001a\u000200J\u000e\u00101\u001a\u00020\u00062\u0006\u00102\u001a\u00020\u001cJ\u0006\u00103\u001a\u00020\u0006J\u000e\u00104\u001a\u0002002\u0006\u00105\u001a\u00020\u001cJ\u000e\u00106\u001a\u0002002\u0006\u00107\u001a\u000208R\u0011\u0010\u0005\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\r\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u000e\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u000f\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\fR\u0011\u0010\u0011\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\u0012\u0010\bR\u0011\u0010\u0013\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u0014\u0010\fR\u0011\u0010\u0015\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\u0016\u0010\bR\u0011\u0010\u0017\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\fR\u0011\u0010\u0019\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\fR\u0011\u0010\u001b\u001a\u00020\u001c8F\u00a2\u0006\u0006\u001a\u0004\b\u001d\u0010\u001eR\u000e\u0010\u001f\u001a\u00020 X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010!\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010\bR\u0011\u0010#\u001a\u00020\u00068F\u00a2\u0006\u0006\u001a\u0004\b$\u0010\bR\u0011\u0010%\u001a\u00020\u001c8F\u00a2\u0006\u0006\u001a\u0004\b&\u0010\u001eR\u0011\u0010\'\u001a\u00020\u001c8F\u00a2\u0006\u0006\u001a\u0004\b(\u0010\u001eR\u0011\u0010)\u001a\u00020\u001c8F\u00a2\u0006\u0006\u001a\u0004\b*\u0010\u001eR\u0011\u0010+\u001a\u00020\u001c8F\u00a2\u0006\u0006\u001a\u0004\b,\u0010\u001eR\u0011\u0010-\u001a\u00020\n8F\u00a2\u0006\u0006\u001a\u0004\b.\u0010\f\u00a8\u00069"}, d2 = {"Lcom/smartcbwtf/mobile/storage/AppConfigStore;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "androidSyncDisabled", "", "getAndroidSyncDisabled", "()Z", "attendanceDistanceToleranceMeters", "", "getAttendanceDistanceToleranceMeters", "()I", "blueWasteMinPercentage", "getBlueWasteMinPercentage", "geofenceRadiusMeters", "getGeofenceRadiusMeters", "gpsEnabled", "getGpsEnabled", "gpsPingIntervalMinutes", "getGpsPingIntervalMinutes", "gpsRequireForeground", "getGpsRequireForeground", "locationUpdateIntervalMinutes", "getLocationUpdateIntervalMinutes", "maxVerificationDelayMinutes", "getMaxVerificationDelayMinutes", "platformName", "", "getPlatformName", "()Ljava/lang/String;", "prefs", "Landroid/content/SharedPreferences;", "qrVerificationDisabled", "getQrVerificationDisabled", "subscriptionActive", "getSubscriptionActive", "subscriptionStatus", "getSubscriptionStatus", "supportEmail", "getSupportEmail", "supportPhone", "getSupportPhone", "userRole", "getUserRole", "weightMismatchTolerancePercent", "getWeightMismatchTolerancePercent", "clear", "", "isFeatureEnabled", "featureKey", "needsRefresh", "setUserRole", "role", "updateFromResponse", "response", "Lcom/smartcbwtf/mobile/network/api/MobileConfigResponse;", "app_debug"})
public final class AppConfigStore {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final android.content.SharedPreferences prefs = null;
    
    @javax.inject.Inject()
    public AppConfigStore(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    public final int getGeofenceRadiusMeters() {
        return 0;
    }
    
    public final int getLocationUpdateIntervalMinutes() {
        return 0;
    }
    
    public final int getAttendanceDistanceToleranceMeters() {
        return 0;
    }
    
    public final int getMaxVerificationDelayMinutes() {
        return 0;
    }
    
    public final int getWeightMismatchTolerancePercent() {
        return 0;
    }
    
    public final int getBlueWasteMinPercentage() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getPlatformName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSupportEmail() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSupportPhone() {
        return null;
    }
    
    public final boolean getAndroidSyncDisabled() {
        return false;
    }
    
    public final boolean getQrVerificationDisabled() {
        return false;
    }
    
    public final boolean getGpsEnabled() {
        return false;
    }
    
    public final int getGpsPingIntervalMinutes() {
        return 0;
    }
    
    public final boolean getGpsRequireForeground() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getUserRole() {
        return null;
    }
    
    public final void setUserRole(@org.jetbrains.annotations.NotNull()
    java.lang.String role) {
    }
    
    public final boolean getSubscriptionActive() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSubscriptionStatus() {
        return null;
    }
    
    public final boolean isFeatureEnabled(@org.jetbrains.annotations.NotNull()
    java.lang.String featureKey) {
        return false;
    }
    
    /**
     * Update config from backend response.
     */
    public final void updateFromResponse(@org.jetbrains.annotations.NotNull()
    com.smartcbwtf.mobile.network.api.MobileConfigResponse response) {
    }
    
    /**
     * Check if config needs refresh (older than 1 hour).
     */
    public final boolean needsRefresh() {
        return false;
    }
    
    /**
     * Clear all config (on logout).
     */
    public final void clear() {
    }
}