package com.smartcbwtf.mobile.utils

object LocationLogSanitizer {
    fun locationReceivedMessage(accuracyMeters: Double?): String {
        val accuracy = accuracyMeters
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?.let { "${it.toInt()}m" }
            ?: "unknown"
        return "Location received with accuracy=$accuracy"
    }
}
