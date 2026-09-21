package com.smartcbwtf.mobile.bluetooth

internal object ReconnectPolicy {
    private val delaysMs = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L)

    fun delayForAttempt(attempt: Int): Long = delaysMs[attempt.coerceIn(0, delaysMs.lastIndex)]
}
