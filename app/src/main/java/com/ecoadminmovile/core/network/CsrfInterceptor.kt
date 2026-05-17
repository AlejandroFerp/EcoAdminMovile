package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.preferences.AppPreferences
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor OkHttp que añade el header X-XSRF-TOKEN en peticiones mutantes
 * (POST, PUT, DELETE, PATCH). Spring Security lo requiere para validar CSRF.
 */
class CsrfInterceptor(
    private val preferences: AppPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method.uppercase()

        // Solo peticiones mutantes necesitan CSRF
        if (method in listOf("POST", "PUT", "DELETE", "PATCH")) {
            val xsrfToken = preferences.getXsrfToken()
            if (!xsrfToken.isNullOrBlank()) {
                val newRequest = request.newBuilder()
                    .header("X-XSRF-TOKEN", xsrfToken)
                    .build()
                return chain.proceed(newRequest)
            }
        }

        return chain.proceed(request)
    }
}
