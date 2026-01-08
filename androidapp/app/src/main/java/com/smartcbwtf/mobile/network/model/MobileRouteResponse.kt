package com.smartcbwtf.mobile.network.model

/**
 * Response model for staff's assigned route.
 */
data class MobileRouteResponse(
    val routeId: String,
    val routeName: String,
    val routeColor: String?,
    val completionDays: Int?,
    val facilityName: String,
    val waypoints: List<MobileWaypointDTO>
)

/**
 * Waypoint (HCF) in the route.
 */
data class MobileWaypointDTO(
    val waypointId: String,
    val sequenceOrder: Int,
    val hcfId: String,
    val hcfCode: String,
    val hcfName: String,
    val hcfAddress: String?,
    val gpsLat: Double?,
    val gpsLon: Double?,
    val attendanceMarked: Boolean?
)
