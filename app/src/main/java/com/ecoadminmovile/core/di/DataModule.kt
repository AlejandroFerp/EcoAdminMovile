/**
 * Módulo de Hilt que provee los repositorios de la capa de datos.
 * Cada repositorio encapsula la lógica de acceso a datos (API, BD local, etc.).
 *
 * Conceptos Kotlin demostrados:
 * - Named arguments (argumentos nombrados): `AuthRepository(api = api, preferences = preferences)`
 *   hace explícito qué valor va a qué parámetro. Mejora la legibilidad y evita errores de orden.
 * - object declaration: Singleton que contiene los factory methods de DI.
 *
 * Patrones de diseño:
 * - Dependency Injection: Hilt resuelve el grafo de dependencias automáticamente.
 * - Repository Pattern: cada repository abstrae la fuente de datos del resto de la app.
 * - Singleton: @Singleton asegura una sola instancia por repositorio.
 */
package com.ecoadminmovile.core.di

import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PendingOperationDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.preferences.AppPreferences
import com.ecoadminmovile.data.AuthRepository
import com.ecoadminmovile.data.CatalogRepository
import com.ecoadminmovile.data.CentersRepository
import com.ecoadminmovile.data.DashboardRepository
import com.ecoadminmovile.data.DocumentosRepository
import com.ecoadminmovile.data.ProfileRepository
import com.ecoadminmovile.data.ResiduosRepository
import com.ecoadminmovile.data.RutasRepository
import com.ecoadminmovile.data.TransfersRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAuthRepository(api: EcoAdminApi, preferences: AppPreferences): AuthRepository {
        // Named arguments: api = api, preferences = preferences
        // Hacen el código autoexplicativo sin depender del orden de parámetros
        return AuthRepository(api = api, preferences = preferences)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(api: EcoAdminApi): DashboardRepository {
        return DashboardRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideTransfersRepository(
        api: EcoAdminApi,
        trasladoDao: TrasladoDao,
        pendingOperationDao: PendingOperationDao
    ): TransfersRepository {
        return TransfersRepository(api = api, trasladoDao = trasladoDao, pendingOperationDao = pendingOperationDao)
    }

    @Provides
    @Singleton
    fun provideCentersRepository(api: EcoAdminApi, centroDao: CentroDao): CentersRepository {
        return CentersRepository(api = api, centroDao = centroDao)
    }

    @Provides
    @Singleton
    fun provideCatalogRepository(api: EcoAdminApi): CatalogRepository {
        return CatalogRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideProfileRepository(api: EcoAdminApi): ProfileRepository {
        return ProfileRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideResiduosRepository(api: EcoAdminApi): ResiduosRepository {
        return ResiduosRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideDocumentosRepository(api: EcoAdminApi): DocumentosRepository {
        return DocumentosRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideRutasRepository(api: EcoAdminApi): RutasRepository {
        return RutasRepository(api = api)
    }
}
