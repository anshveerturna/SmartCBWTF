package com.smartcbwtf.mobile.security

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object JwtTokenUtils {

    fun isExpired(token: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val payloadJson = decodePayload(token) ?: return true
        val exp = extractExp(payloadJson)
        if (exp <= 0L) {
            return true
        }
        return exp <= nowEpochSeconds
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodePayload(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) {
                return null
            }
            val payload = parts[1]
            val padding = when (payload.length % 4) {
                2 -> "=="
                3 -> "="
                else -> ""
            }
            val decoded = Base64.UrlSafe.decode(payload + padding)
            String(decoded, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractExp(payloadJson: String): Long {
        val match = """"exp"\s*:\s*(\d+)""".toRegex().find(payloadJson) ?: return -1L
        return match.groupValues[1].toLongOrNull() ?: -1L
    }
}
