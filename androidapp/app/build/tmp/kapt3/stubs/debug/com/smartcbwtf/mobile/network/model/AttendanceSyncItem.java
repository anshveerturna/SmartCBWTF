package com.smartcbwtf.mobile.network.model;

/**
 * Single attendance event item for sync request.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0018\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BE\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\b\u0012\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\u0002\u0010\rJ\t\u0010\u001a\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\bH\u00c6\u0003J\t\u0010\u001e\u001a\u00020\bH\u00c6\u0003J\u0010\u0010\u001f\u001a\u0004\u0018\u00010\u000bH\u00c6\u0003\u00a2\u0006\u0002\u0010\u0014J\u000b\u0010 \u001a\u0004\u0018\u00010\u0003H\u00c6\u0003JX\u0010!\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\n\b\u0002\u0010\n\u001a\u0004\u0018\u00010\u000b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0003H\u00c6\u0001\u00a2\u0006\u0002\u0010\"J\u0013\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010&\u001a\u00020\'H\u00d6\u0001J\t\u0010(\u001a\u00020\u0003H\u00d6\u0001R\u0013\u0010\f\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0015\u0010\n\u001a\u0004\u0018\u00010\u000b\u00a2\u0006\n\n\u0002\u0010\u0015\u001a\u0004\b\u0013\u0010\u0014R\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0017R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u000f\u00a8\u0006)"}, d2 = {"Lcom/smartcbwtf/mobile/network/model/AttendanceSyncItem;", "", "clientEventId", "", "hcfId", "eventTs", "", "gpsLat", "", "gpsLon", "gpsAccuracyM", "", "appDeviceId", "(Ljava/lang/String;Ljava/lang/String;JDDLjava/lang/Float;Ljava/lang/String;)V", "getAppDeviceId", "()Ljava/lang/String;", "getClientEventId", "getEventTs", "()J", "getGpsAccuracyM", "()Ljava/lang/Float;", "Ljava/lang/Float;", "getGpsLat", "()D", "getGpsLon", "getHcfId", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "(Ljava/lang/String;Ljava/lang/String;JDDLjava/lang/Float;Ljava/lang/String;)Lcom/smartcbwtf/mobile/network/model/AttendanceSyncItem;", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class AttendanceSyncItem {
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String clientEventId = null;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String hcfId = null;
    private final long eventTs = 0L;
    private final double gpsLat = 0.0;
    private final double gpsLon = 0.0;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.Float gpsAccuracyM = null;
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String appDeviceId = null;
    
    public AttendanceSyncItem(@org.jetbrains.annotations.NotNull()
    java.lang.String clientEventId, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, long eventTs, double gpsLat, double gpsLon, @org.jetbrains.annotations.Nullable()
    java.lang.Float gpsAccuracyM, @org.jetbrains.annotations.Nullable()
    java.lang.String appDeviceId) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getClientEventId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getHcfId() {
        return null;
    }
    
    public final long getEventTs() {
        return 0L;
    }
    
    public final double getGpsLat() {
        return 0.0;
    }
    
    public final double getGpsLon() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float getGpsAccuracyM() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getAppDeviceId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component2() {
        return null;
    }
    
    public final long component3() {
        return 0L;
    }
    
    public final double component4() {
        return 0.0;
    }
    
    public final double component5() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Float component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.smartcbwtf.mobile.network.model.AttendanceSyncItem copy(@org.jetbrains.annotations.NotNull()
    java.lang.String clientEventId, @org.jetbrains.annotations.NotNull()
    java.lang.String hcfId, long eventTs, double gpsLat, double gpsLon, @org.jetbrains.annotations.Nullable()
    java.lang.Float gpsAccuracyM, @org.jetbrains.annotations.Nullable()
    java.lang.String appDeviceId) {
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