package com.smartcbwtf.mobile.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionLaunchGuardTest {

    @Test
    fun `operational services require an authenticated unlocked session`() {
        assertTrue(SessionLaunchGuard.canInitializeOperationalServices("token", mustChangePassword = false))
        assertFalse(SessionLaunchGuard.canInitializeOperationalServices(null, mustChangePassword = false))
        assertFalse(SessionLaunchGuard.canInitializeOperationalServices("", mustChangePassword = false))
        assertFalse(SessionLaunchGuard.canInitializeOperationalServices("token", mustChangePassword = true))
    }
}
