package com.ecoadminmovile.core.preferences

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class AppPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        return sharedPreferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(rawValue: String) {
        val normalized = normalizeBaseUrl(rawValue)
        val previousValue = getBaseUrl()
        sharedPreferences.edit()
            .putString(KEY_BASE_URL, normalized)
            .apply()

        if (normalized != previousValue) {
            clearSession()
        }
    }

    fun getSessionCookie(): String? {
        return sharedPreferences.getString(KEY_SESSION_COOKIE, null)
    }

    fun saveSessionCookie(value: String) {
        sharedPreferences.edit()
            .putString(KEY_SESSION_COOKIE, value)
            .apply()
    }

    fun hasSessionCookie(): Boolean {
        return !getSessionCookie().isNullOrBlank()
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_SESSION_COOKIE)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "ecoadmin_preferences"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_SESSION_COOKIE = "session_cookie"

        const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"
        const val SESSION_COOKIE_NAME = "JSESSIONID"

        fun normalizeBaseUrl(rawValue: String): String {
            val trimmed = rawValue.trim().ifEmpty { DEFAULT_BASE_URL }
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }

            val parsed = withScheme.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Introduce una URL valida para el servidor.")

            return parsed.newBuilder()
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
                .toString()
        }
    }
}
