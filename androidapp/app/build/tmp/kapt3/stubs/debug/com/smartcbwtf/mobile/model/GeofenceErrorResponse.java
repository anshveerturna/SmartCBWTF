package com.smartcbwtf.mobile.model;

/**
 * Error response for geofence rejection
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0015\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B1\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\b\u0010\u0007\u001a\u0004\u0018\u00010\b\u0012\b\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0016\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\u0005H\u00c6\u0003J\u0010\u0010\u0019\u001a\u0004\u0018\u00010\bH\u00c6\u0003\u00a2\u0006\u0002\u0010\u000fJ\u0010\u0010\u001a\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003\u00a2\u0006\u0002\u0010\fJD\u0010\u001b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\b2\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010\u001cJ\u0013\u0010\u001d\u001a\u00020\u001e2\b\u0010\u001f\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010 \u001a\u00020\u0003H\u00d6\u0001J\t\u0010!\u001a\u00020\u0005H\u00d6\u0001R\u0015\u0010\t\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\n\n\u0002\u0010\r\u001a\u0004\b\u000b\u0010\fR\u0015\u0010\u0007\u001a\u0004\u0018\u00010\b\u00a2\u0006\n\n\u0002\u0010\u0010\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015\u00a8\u0006\""}, d2 = {"Lcom/smartcbwtf/mobile/model/GeofenceErrorResponse;", "", "status", "", "error", "", "message", "distanceMeters", "", "allowedRadiusMeters", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Integer;)V", "getAllowedRadiusMeters", "()Ljava/lang/Integer;", "Ljava/lang/Integer;", "getDistanceMeters", "()Ljava/lang/Double;", "Ljava/lang/Double;", "getError", "()Ljava/lang/String;", "getMessage", "getStatus", "()I", "component1", "component2", "component3", "component4", "component5", "copy", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Double;Ljava/lang/Integer;)Lcom/smartcbwtf/mobile/model/GeofenceErrorResponse;", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class GeofenceErrorResponse {
    private final int status = 0;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String error = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String message = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Double distanceMeters = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Integer allowedRadiusMeters = null;
    
    public GeofenceErrorResponse(int status, @org.jetbrains.annotations.NotNull()
    java.lang.String error, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceMeters, @org.jetbrains.annotations.Nullable()
    java.lang.Integer allowedRadiusMeters) {
        super();
    }
    
    public final int getStatus() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getError() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double getDistanceMeters() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer getAllowedRadiusMeters() {
        return null;
    }
    
    public final int component1() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Double component4() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Integer component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.model.GeofenceErrorResponse copy(int status, @org.jetbrains.annotations.NotNull()
    java.lang.String error, @org.jetbrains.annotations.NotNull()
    java.lang.String message, @org.jetbrains.annotations.Nullable()
    java.lang.Double distanceMeters, @org.jetbrains.annotations.Nullable()
    java.lang.Integer allowedRadiusMeters) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}