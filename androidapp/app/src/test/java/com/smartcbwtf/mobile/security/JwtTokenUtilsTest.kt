package com.smartcbwtf.mobile.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtTokenUtilsTest {

    @Test
    fun `isExpired returns true when token exp is in the past`() {
        val token = tokenWithExp(1000L)
        assertTrue(JwtTokenUtils.isExpired(token, nowEpochSeconds = 1001L))
    }

    @Test
    fun `isExpired returns false when token exp is in the future`() {
        val token = tokenWithExp(2000L)
        assertFalse(JwtTokenUtils.isExpired(token, nowEpochSeconds = 1001L))
    }

    @Test
    fun `isExpired returns true for malformed token`() {
        assertTrue(JwtTokenUtils.isExpired("not-a-jwt", nowEpochSeconds = 1001L))
    }

    private fun tokenWithExp(exp: Long): String {
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = """{"exp":$exp,"sub":"tester"}"""
        return "${base64Url(header)}.${base64Url(payload)}.signature"
    }

    private fun base64Url(value: String): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
    }
}
