package com.ecoadminmovile.core.network

import com.ecoadminmovile.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class ServerUrlInterceptor : Interceptor {
    private val baseUrl = BuildConfig.BASE_URL.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        val rewrittenUrl = originalUrl.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
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
