package com.smartcbwtf.mobile.storage

import android.content.SharedPreferences
import com.smartcbwtf.mobile.network.api.MobileConfigResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigStoreTest {

    @Test
    fun `store reads and writes through injected preferences`() {
        val prefs = InMemorySharedPreferences()
        val store = AppConfigStore(prefs)

        store.setUserRole("DRIVER")
        prefs.edit()
            .putBoolean("gpsEnabled", false)
            .putInt("gpsPingIntervalMinutes", 12)
            .apply()

        assertEquals("DRIVER", store.userRole)
        assertFalse(store.gpsEnabled)
        assertEquals(12, store.gpsPingIntervalMinutes)
    }

    @Test
    fun `update and clear remove only config keys`() {
        val prefs = InMemorySharedPreferences()
        val store = AppConfigStore(prefs)

        store.updateFromResponse(
            MobileConfigResponse(
                subscriptionStatus = "ACTIVE",
                active = true,
                features = mapOf("routes" to true),
                thresholds = mapOf("gpsEnabled" to false, "gpsPingIntervalMinutes" to 9)
            )
        )

        assertTrue(store.subscriptionActive)
        assertEquals("ACTIVE", store.subscriptionStatus)
        assertTrue(store.isFeatureEnabled("routes"))
        assertFalse(store.gpsEnabled)
        assertEquals(9, store.gpsPingIntervalMinutes)
        prefs.edit()
            .putString("auth_token", "keep-token-owner-clear")
            .putBoolean("location_consent", true)
            .putFloat("last_lat", 28.61f)
            .apply()

        store.clear()

        assertEquals("UNKNOWN", store.subscriptionStatus)
        assertFalse(store.isFeatureEnabled("routes"))
        assertTrue(store.gpsEnabled)
        assertEquals("keep-token-owner-clear", prefs.getString("auth_token", null))
        assertTrue(prefs.getBoolean("location_consent", false))
        assertEquals(28.61f, prefs.getFloat("last_lat", 0f))
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST")
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        private inner class Editor : SharedPreferences.Editor {
            private val changes = linkedMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearRequested = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
                changes[key] = value
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                changes[key] = values?.toSet()
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
                changes[key] = value
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
                changes[key] = value
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
                changes[key] = value
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
                changes[key] = value
            }
            override fun remove(key: String): SharedPreferences.Editor = apply {
                removals += key
            }
            override fun clear(): SharedPreferences.Editor = apply {
                clearRequested = true
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearRequested) {
                    values.clear()
                }
                removals.forEach(values::remove)
                changes.forEach { (key, value) ->
                    if (value == null) {
                        values.remove(key)
                    } else {
                        values[key] = value
                    }
                }
            }
        }
    }
}
