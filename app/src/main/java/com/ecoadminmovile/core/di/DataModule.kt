package com.ecoadminmovile.core.di

import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.preferences.AppPreferences
import com.ecoadminmovile.data.AuthRepository
import com.ecoadminmovile.data.CentersRepository
import com.ecoadminmovile.data.DashboardRepository
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
        return AuthRepository(api = api, preferences = preferences)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(api: EcoAdminApi): DashboardRepository {
        return DashboardRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideTransfersRepository(api: EcoAdminApi): TransfersRepository {
        return TransfersRepository(api = api)
    }

    @Provides
    @Singleton
    fun provideCentersRepository(api: EcoAdminApi): CentersRepository {
        return CentersRepository(api = api)
    }
}
