package com.smartcbwtf.mobile.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationLogSanitizerTest {

    @Test
    fun `location received message does not include coordinates`() {
        val message = LocationLogSanitizer.locationReceivedMessage(12.7)

        assertTrue(message.contains("accuracy=12m"))
        assertFalse(message.contains("28.6139"))
        assertFalse(message.contains("77.2090"))
    }

    @Test
    fun `location received message handles invalid accuracy without leaking raw data`() {
        val message = LocationLogSanitizer.locationReceivedMessage(Double.NaN)

        assertTrue(message.contains("accuracy=unknown"))
        assertFalse(message.contains("NaN"))
    }
}
