/**
 * Módulo de Hilt (Dagger) que provee las dependencias de red: OkHttp, Retrofit y la API.
 *
 * Conceptos Kotlin demostrados:
 * - object declaration: declara un Singleton en Kotlin. Solo existe una instancia de este objeto.
 *   Es thread-safe por defecto (lo garantiza el lenguaje).
 * - `apply { }`: scope function que ejecuta un bloque sobre el receptor y devuelve el receptor.
 *   Ideal para configurar builders sin repetir el nombre de la variable.
 * - `if` como expresión: en Kotlin, `if` devuelve un valor (no necesita operador ternario).
 * - `::class.java`: referencia a la clase Java (KClass → Class<T>), necesaria para APIs Java.
 *
 * Patrones de diseño:
 * - Dependency Injection (DI): Hilt inyecta dependencias automáticamente en el grafo.
 * - Singleton: @Singleton garantiza una sola instancia en el contenedor de DI.
 * - Factory Method: cada @Provides es un factory method que Hilt llama cuando necesita la dependencia.
 */
package com.ecoadminmovile.core.di

import android.content.Context
import com.ecoadminmovile.BuildConfig
import com.ecoadminmovile.core.network.CsrfInterceptor
import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.network.ServerUrlInterceptor
import com.ecoadminmovile.core.network.SessionCookieJar
import com.ecoadminmovile.core.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

// @Module: indica a Hilt que esta clase contiene métodos que proveen dependencias
// @InstallIn(SingletonComponent::class): las dependencias viven durante toda la app
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule { // object = Singleton en Kotlin (una sola instancia garantizada)

    // @Provides: le dice a Hilt cómo crear esta dependencia
    // @Singleton: solo se crea una instancia para todo el ciclo de vida de la app
    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSessionCookieJar(preferences: AppPreferences): SessionCookieJar {
        return SessionCookieJar(preferences)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: SessionCookieJar,
        preferences: AppPreferences
    ): OkHttpClient {
        // apply { }: configura el objeto dentro del bloque y lo devuelve
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // if como expresión: devuelve el valor directamente (no necesita ternario)
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY // Cambiado de BASIC a BODY para ver JSON y headers
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(cookieJar)
            .addInterceptor(ServerUrlInterceptor())
            .addInterceptor(CsrfInterceptor(preferences))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideEcoAdminApi(client: OkHttpClient): EcoAdminApi {
        return Retrofit.Builder()
            .baseUrl(ServerUrlInterceptor.PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            // .create(EcoAdminApi::class.java): genera la implementación de la interfaz
            // ::class.java convierte KClass<EcoAdminApi> a Class<EcoAdminApi> (interop con Java)
            .create(EcoAdminApi::class.java)
    }
}
