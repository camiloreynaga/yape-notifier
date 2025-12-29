package com.yapenotifier.android.di

import android.content.Context
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.repository.CommerceRepository
import com.yapenotifier.android.data.repository.MonitoredAppsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Dependency Injection usando Hilt.
 * Professional approach: Centraliza la creación de dependencias.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Proporciona ApiService como singleton.
     * Professional approach: Una sola instancia de Retrofit para toda la app.
     */
    @Provides
    @Singleton
    fun provideApiService(@ApplicationContext context: Context): ApiService {
        return RetrofitClient.createApiService(context)
    }
    
    /**
     * Proporciona PreferencesManager como singleton.
     */
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    /**
     * Proporciona CommerceRepository.
     * Professional approach: Repository depende de ApiService inyectado.
     */
    @Provides
    @Singleton
    fun provideCommerceRepository(apiService: ApiService): CommerceRepository {
        return CommerceRepository(apiService)
    }
    
    /**
     * Proporciona MonitoredAppsRepository.
     */
    @Provides
    @Singleton
    fun provideMonitoredAppsRepository(apiService: ApiService): MonitoredAppsRepository {
        return MonitoredAppsRepository(apiService)
    }
}

