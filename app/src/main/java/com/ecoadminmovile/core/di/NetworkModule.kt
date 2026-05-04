package com.ecoadminmovile.core.di

import android.content.Context
import com.ecoadminmovile.BuildConfig
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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

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
        preferences: AppPreferences,
        cookieJar: SessionCookieJar
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(cookieJar)
            .addInterceptor(ServerUrlInterceptor(preferences))
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
            .create(EcoAdminApi::class.java)
    }
}
