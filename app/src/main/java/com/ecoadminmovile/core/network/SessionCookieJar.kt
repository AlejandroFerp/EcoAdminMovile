/**
 * Implementación de CookieJar de OkHttp que persiste la cookie de sesión en SharedPreferences.
 *
 * Conceptos Kotlin demostrados:
 * - Constructor primario con parámetro: `class Foo(private val x: X)` define el constructor
 *   directamente en la cabecera de la clase. Es más conciso que Java.
 * - Implementación de interfaz: `: CookieJar` (equivalente a "implements" en Java).
 * - `?.let { }`: safe-call + scope function. Solo ejecuta el bloque si el receptor no es null.
 * - `firstOrNull { predicado }`: función de extensión de colecciones que devuelve el primer
 *   elemento que cumple la condición, o null si ninguno la cumple.
 * - Builder pattern condicional: se encadena `.secure()` solo si la URL es HTTPS.
 * - `?: return emptyList()`: operador Elvis para retorno temprano si el valor es null.
 *
 * Patrón de diseño: Adapter — adapta el almacenamiento local (AppPreferences) a la interfaz
 * que OkHttp espera (CookieJar).
 */
package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.preferences.AppPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

// Constructor primario: los parámetros declarados aquí son propiedades de la clase
class SessionCookieJar(
    private val preferences: AppPreferences
) : CookieJar { // `: CookieJar` = implementa la interfaz CookieJar
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies
            .firstOrNull { it.name == AppPreferences.SESSION_COOKIE_NAME }
            ?.let { cookie ->
                preferences.saveSessionCookie(cookie.value)
            }
        // Capturar XSRF-TOKEN para incluirlo en peticiones mutantes (POST/PUT/DELETE)
        cookies
            .firstOrNull { it.name == AppPreferences.XSRF_COOKIE_NAME }
            ?.let { cookie ->
                preferences.saveXsrfToken(cookie.value)
            }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // Operador Elvis (?: ): si getSessionCookie() es null, retorna inmediatamente emptyList()
        val sessionValue = preferences.getSessionCookie() ?: return emptyList()
        // Builder pattern: construye el objeto paso a paso con llamadas encadenadas
        val cookieBuilder = Cookie.Builder()
            .name(AppPreferences.SESSION_COOKIE_NAME)
            .value(sessionValue)
            .domain(url.host)
            .path("/")
            .httpOnly()

        // Condicional sobre el builder: agrega .secure() solo si aplica
        if (url.isHttps) {
            cookieBuilder.secure()
        }

        return listOf(cookieBuilder.build())
    }
}
