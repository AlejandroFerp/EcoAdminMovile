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
        return Room.databaseBuilder(
            context,
            EcoAdminDatabase::class.java,
            "ecoadmin_database"
        ).build()
    }

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
