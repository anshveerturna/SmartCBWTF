package com.smartcbwtf.mobile.network

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
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        val response = chain.proceed(request)

        if (response.code == 401 || response.code == 403) {
            runBlocking { tokenStore.setToken(null) }
        }

        return response
    }
}
