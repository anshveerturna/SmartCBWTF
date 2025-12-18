package com.smartcbwtf.mobile.network

import android.util.Log
import com.smartcbwtf.mobile.storage.AuthTokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: AuthTokenStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.getToken() }
        
        Log.d("AuthInterceptor", "Token present: ${!token.isNullOrBlank()}, URL: ${chain.request().url}")
        
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        val response = chain.proceed(request)

        // Only clear token on 401 Unauthorized (meaning token is invalid/expired)
        // Do NOT clear on 403 Forbidden (user is authenticated but lacks permission)
        if (response.code == 401) {
            Log.d("AuthInterceptor", "Got 401, clearing token")
            runBlocking { tokenStore.setToken(null) }
        }

        return response
    }
}

