package com.smartcbwtf.mobile.security

object SessionLaunchGuard {
    fun canInitializeOperationalServices(token: String?, mustChangePassword: Boolean): Boolean {
        return !token.isNullOrBlank() && !mustChangePassword
    }
}
