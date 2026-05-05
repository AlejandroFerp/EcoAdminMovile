/**
 * Módulo de Hilt que provee la base de datos Room y sus DAOs.
 *
 * Conceptos Kotlin demostrados:
 * - object declaration: Singleton para el módulo de DI.
 * - `::class.java`: referencia KClass convertida a Class<T> de Java, necesaria para
 *   APIs como Room que esperan el tipo Java.
 * - Funciones abstractas vs concretas: Room genera la implementación de `abstract fun trasladoDao()`.
 *
 * Patrones de diseño:
 * - Abstract Factory: Room.databaseBuilder es una factoría que crea la BD concreta.
 * - Singleton: la BD se crea una sola vez (@Singleton) para toda la aplicación.
 * - DAO (Data Access Object): patrón que separa la lógica de persistencia de la lógica de negocio.
 *
 * Nota: los DAOs no tienen @Singleton porque son objetos ligeros que Room puede recrear.
 */
package com.ecoadminmovile.core.di

import android.content.Context
import androidx.room.Room
import com.ecoadminmovile.core.database.EcoAdminDatabase
import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PerfilUsuarioDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EcoAdminDatabase {
        // Room.databaseBuilder: Builder pattern para configurar y crear la base de datos
        // EcoAdminDatabase::class.java → referencia a la clase Java (interop KClass → Class)
        return Room.databaseBuilder(
            context,
            EcoAdminDatabase::class.java,
            "ecoadmin_database"
        ).build()
    }

    // Los DAOs se obtienen de la instancia de la BD — Room genera la implementación
    @Provides
    fun provideTrasladoDao(database: EcoAdminDatabase): TrasladoDao {
        return database.trasladoDao()
    }

    @Provides
    fun provideCentroDao(database: EcoAdminDatabase): CentroDao {
        return database.centroDao()
    }

    @Provides
    fun providePerfilUsuarioDao(database: EcoAdminDatabase): PerfilUsuarioDao {
        return database.perfilUsuarioDao()
    }
}
