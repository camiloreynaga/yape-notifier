package com.yapenotifier.android

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.worker.SyncSettingsWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import timber.log.Timber
import java.util.UUID

@HiltAndroidApp
class YapeNotifierApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.tag("YapeNotifierApplication").d("Application created. API URL: ${BuildConfig.API_BASE_URL}")
        
        // CRITICAL: Generate and persist device UUID once on first app launch
        ensureDeviceUuid()
        
        setupRecurringWork()
    }
    
    /**
     * Genera y guarda el UUID del dispositivo UNA VEZ cuando la app se inicia por primera vez.
     * Este UUID es único por instalación de la app y nunca cambia.
     * Se usa para identificar el dispositivo en el backend.
     */
    private fun ensureDeviceUuid() {
        applicationScope.launch {
            try {
                val preferencesManager = PreferencesManager(this@YapeNotifierApplication)
                val existingUuid = preferencesManager.deviceUuid.first()
                
                if (existingUuid == null || existingUuid.isBlank()) {
                    val uuid = UUID.randomUUID().toString()
                    preferencesManager.saveDeviceUuid(uuid)
                    Timber.tag("YapeNotifierApplication").i("Device UUID generado y guardado: $uuid")
                } else {
                    Timber.tag("YapeNotifierApplication").d("Device UUID existente: $existingUuid")
                }
            } catch (e: Exception) {
                Timber.tag("YapeNotifierApplication").e(e, "Error al generar/verificar device UUID")
            }
        }
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule a periodic worker to sync settings from the API daily
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncSettingsWorker>(
            1, TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SyncSettingsWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )

        Timber.tag("YapeNotifierApplication").d("Periodic sync worker for settings has been scheduled.")
    }
}
