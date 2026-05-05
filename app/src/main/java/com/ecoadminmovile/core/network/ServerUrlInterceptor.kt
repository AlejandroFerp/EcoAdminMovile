/**
 * Interceptor de OkHttp que reescribe la URL base de cada petición en tiempo de ejecución.
 * Permite usar un placeholder en Retrofit y resolver la URL real desde BuildConfig.
 *
 * Conceptos Kotlin demostrados:
 * - companion object: equivalente a miembros estáticos en Java. Accesible como
 *   `ServerUrlInterceptor.PLACEHOLDER_BASE_URL` sin instanciar la clase.
 * - const val: constante de compilación (se reemplaza literalmente donde se usa).
 * - Función de extensión `.toHttpUrl()`: convierte un String en un objeto HttpUrl.
 *   Las funciones de extensión añaden métodos a clases existentes sin herencia.
 * - Builder pattern con llamadas encadenadas: construye objetos complejos paso a paso.
 *
 * Patrón de diseño: Chain of Responsibility (Interceptor) — cada interceptor procesa
 * la petición y la pasa al siguiente en la cadena con `chain.proceed()`.
 */
package com.ecoadminmovile.core.network

import com.ecoadminmovile.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class ServerUrlInterceptor : Interceptor {
    // .toHttpUrl() es una función de extensión definida en OkHttp sobre String
    private val baseUrl = BuildConfig.BASE_URL.toHttpUrl()

    // intercept() es el método del patrón Chain of Responsibility
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalUrl = originalRequest.url
        // Builder pattern: crea una nueva URL reemplazando scheme, host y port
        val rewrittenUrl = originalUrl.newBuilder()
            .scheme(baseUrl.scheme)
            .host(baseUrl.host)
            .port(baseUrl.port)
            .build()

        // chain.proceed() pasa la petición modificada al siguiente interceptor de la cadena
        return chain.proceed(
            originalRequest.newBuilder()
                .url(rewrittenUrl)
                .build()
        )
    }

    // companion object con constante accesible sin instancia: ServerUrlInterceptor.PLACEHOLDER_BASE_URL
    companion object {
        // const val: solo puede ser String o tipo primitivo, se resuelve en compilación
        const val PLACEHOLDER_BASE_URL = "http://localhost/"
    }
}
