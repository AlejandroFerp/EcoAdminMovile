/**
 * Gestión de SharedPreferences para almacenar la cookie de sesión del usuario.
 *
 * Conceptos Kotlin demostrados:
 * - companion object: equivalente a los miembros "static" en Java. Permite definir
 *   constantes y funciones asociadas a la clase, no a la instancia.
 * - const val: constantes conocidas en tiempo de compilación (solo tipos primitivos y String).
 * - Tipo nullable con `?`: `String?` indica que el valor puede ser null.
 * - Función de extensión `isNullOrBlank()`: verifica si un String? es null O está vacío/blancos.
 * - `apply()` en SharedPreferences.Editor: escritura asíncrona (no bloquea el hilo principal).
 *
 * Patrón de diseño: Wrapper/Fachada sobre SharedPreferences para encapsular el acceso a datos locales.
 */
package com.ecoadminmovile.core.preferences

import android.content.Context

open class AppPreferences(context: Context) {
    // getSharedPreferences devuelve la instancia compartida de almacenamiento clave-valor
    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Retorno nullable (String?) — el caller debe manejar el caso null
    open fun getSessionCookie(): String? {
        return sharedPreferences.getString(KEY_SESSION_COOKIE, null)
    }

    open fun saveSessionCookie(value: String) {
        sharedPreferences.edit()
            .putString(KEY_SESSION_COOKIE, value)
            .apply() // apply() es asíncrono; commit() sería síncrono
    }

    open fun hasSessionCookie(): Boolean {
        // isNullOrBlank() es una función de extensión de Kotlin sobre String?
        // Combina la verificación de null Y de contenido vacío/espacios en una sola llamada
        return !getSessionCookie().isNullOrBlank()
    }

    open fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_SESSION_COOKIE)
            .apply()
    }

    // companion object: bloque "estático" de la clase. Solo existe una instancia por clase.
    companion object {
        // const val: constante de compilación, se inlinea donde se use (más eficiente que val)
        private const val PREFERENCES_NAME = "ecoadmin_preferences"
        private const val KEY_SESSION_COOKIE = "session_cookie"
        const val SESSION_COOKIE_NAME = "JSESSIONID"
    }
}
