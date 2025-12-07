package com.smartcbwtf.mobile.storage

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthTokenStore @Inject constructor(
    private val prefs: SharedPreferences
) : AuthTokenStore {

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_TOKEN, null)
    }

    override suspend fun setToken(token: String?) {
        withContext(Dispatchers.IO) {
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }
}
