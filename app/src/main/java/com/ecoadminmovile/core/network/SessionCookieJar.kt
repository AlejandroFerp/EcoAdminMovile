package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.preferences.AppPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar(
    private val preferences: AppPreferences
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies
            .firstOrNull { it.name == AppPreferences.SESSION_COOKIE_NAME }
            ?.let { cookie ->
                preferences.saveSessionCookie(cookie.value)
            }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val sessionValue = preferences.getSessionCookie() ?: return emptyList()
        val cookieBuilder = Cookie.Builder()
            .name(AppPreferences.SESSION_COOKIE_NAME)
            .value(sessionValue)
            .domain(url.host)
            .path("/")
            .httpOnly()

        if (url.isHttps) {
            cookieBuilder.secure()
        }

        return listOf(cookieBuilder.build())
    }
}
