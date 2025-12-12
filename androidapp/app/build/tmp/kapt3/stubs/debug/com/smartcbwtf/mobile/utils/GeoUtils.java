package com.smartcbwtf.mobile.utils;

/**
 * Utility object for geolocation calculations
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J&\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u00042\u0006\u0010\b\u001a\u00020\u00042\u0006\u0010\t\u001a\u00020\u0004J.\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u00042\u0006\u0010\r\u001a\u00020\u00042\u0006\u0010\u000e\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0011"}, d2 = {"Lcom/smartcbwtf/mobile/utils/GeoUtils;", "", "()V", "EARTH_RADIUS_METERS", "", "haversineMeters", "lat1", "lon1", "lat2", "lon2", "isWithinGeofence", "", "currentLat", "currentLon", "centerLat", "centerLon", "radiusMeters", "app_debug"})
public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    @org.jetbrains.annotations.NotNull()
    public static final com.smartcbwtf.mobile.utils.GeoUtils INSTANCE = null;
    
    private GeoUtils() {
        super();
    }
    
    /**
     * Calculate the distance in meters between two GPS coordinates using the Haversine formula
     *
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    public final double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        return 0.0;
    }
    
    /**
     * Check if a point is within a geofence
     *
     * @param currentLat Current latitude
     * @param currentLon Current longitude
     * @param centerLat Geofence center latitude
     * @param centerLon Geofence center longitude
     * @param radiusMeters Geofence radius in meters
     * @return true if the point is within the geofence
     */
    public final boolean isWithinGeofence(double currentLat, double currentLon, double centerLat, double centerLon, double radiusMeters) {
        return false;
    }
}