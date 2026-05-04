package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.preferences.AppPreferences
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.toHttpUrl

class ServerUrlInterceptor(
    private val preferences: AppPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val currentBaseUrl = preferences.getBaseUrl().toHttpUrl()
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val rewrittenUrl = originalUrl.newBuilder()
            .scheme(currentBaseUrl.scheme)
            .host(currentBaseUrl.host)
            .port(currentBaseUrl.port)
            .build()

        return chain.proceed(
            originalRequest.newBuilder()
                .url(rewrittenUrl)
                .build()
        )
    }

    companion object {
        const val PLACEHOLDER_BASE_URL = "http://localhost/"
    }
}
