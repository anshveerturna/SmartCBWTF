package com.smartcbwtf.mobile.utils

import kotlin.math.*

/**
 * Utility object for geolocation calculations
 */
object GeoUtils {
    
    private const val EARTH_RADIUS_METERS = 6371000.0
    
    /**
     * Calculate the distance in meters between two GPS coordinates using the Haversine formula
     * 
     * @param lat1 Latitude of first point in degrees
     * @param lon1 Longitude of first point in degrees
     * @param lat2 Latitude of second point in degrees
     * @param lon2 Longitude of second point in degrees
     * @return Distance in meters
     */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
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
    fun isWithinGeofence(
        currentLat: Double,
        currentLon: Double,
        centerLat: Double,
        centerLon: Double,
        radiusMeters: Double
    ): Boolean {
        val distance = haversineMeters(currentLat, currentLon, centerLat, centerLon)
        return distance <= radiusMeters
    }
}
