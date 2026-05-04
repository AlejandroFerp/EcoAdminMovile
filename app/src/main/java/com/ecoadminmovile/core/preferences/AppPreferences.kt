package com.ecoadminmovile.core.preferences

import android.content.Context

class AppPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

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
        private const val KEY_SESSION_COOKIE = "session_cookie"
        const val SESSION_COOKIE_NAME = "JSESSIONID"
    }
}
