package com.smartcbwtf.mobile.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test

class ReconnectPolicyTest {

    @Test
    fun `backs off quickly and caps retries at fifteen seconds`() {
        assertEquals(1_000L, ReconnectPolicy.delayForAttempt(0))
        assertEquals(2_000L, ReconnectPolicy.delayForAttempt(1))
        assertEquals(4_000L, ReconnectPolicy.delayForAttempt(2))
        assertEquals(8_000L, ReconnectPolicy.delayForAttempt(3))
        assertEquals(15_000L, ReconnectPolicy.delayForAttempt(4))
        assertEquals(15_000L, ReconnectPolicy.delayForAttempt(20))
    }
}
