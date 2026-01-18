package com.yapenotifier.android

import android.app.Application
import android.provider.Settings
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.util.FileLogger
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

        // Initialize FileLogger for persistent debugging
        FileLogger.init(this)
        FileLogger.log("SYSTEM", "Application started - Build: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")

        Timber.tag("YapeNotifierApplication").d("Application created. API URL: ${BuildConfig.API_BASE_URL}")
        
        // CRITICAL: Generate and persist device UUID once on first app launch
        ensureDeviceUuid()
        
        setupRecurringWork()
    }
    
    /**
     * Genera y guarda el identificador único del dispositivo.
     *
     * Usa ANDROID_ID que es:
     * - Persistente entre reinstalaciones de la app
     * - Único por dispositivo físico + usuario Android
     * - No requiere permisos especiales
     *
     * Formato: UUID v5 basado en ANDROID_ID para mantener compatibilidad con backend.
     *
     * Nota: ANDROID_ID cambia si se hace factory reset del dispositivo.
     */
    private fun ensureDeviceUuid() {
        applicationScope.launch {
            try {
                val preferencesManager = PreferencesManager(this@YapeNotifierApplication)
                val existingUuid = preferencesManager.deviceUuid.first()

                // Obtener ANDROID_ID del dispositivo
                val androidId = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ANDROID_ID
                )

                if (androidId.isNullOrBlank()) {
                    // Fallback a UUID aleatorio si ANDROID_ID no está disponible (muy raro)
                    if (existingUuid == null || existingUuid.isBlank()) {
                        val uuid = UUID.randomUUID().toString()
                        preferencesManager.saveDeviceUuid(uuid)
                        Timber.tag("YapeNotifierApplication").w("ANDROID_ID no disponible, usando UUID aleatorio: $uuid")
                    }
                    return@launch
                }

                // Generar UUID determinístico basado en ANDROID_ID
                // Esto garantiza que el mismo dispositivo siempre genere el mismo UUID
                val deviceUuid = UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()

                if (existingUuid != deviceUuid) {
                    // Guardar o actualizar el UUID basado en ANDROID_ID
                    preferencesManager.saveDeviceUuid(deviceUuid)
                    if (existingUuid == null || existingUuid.isBlank()) {
                        Timber.tag("YapeNotifierApplication").i("Device UUID generado desde ANDROID_ID: $deviceUuid")
                    } else {
                        // Migración: actualizar de UUID aleatorio a UUID basado en ANDROID_ID
                        Timber.tag("YapeNotifierApplication").i("Device UUID migrado de $existingUuid a $deviceUuid (basado en ANDROID_ID)")
                    }
                } else {
                    Timber.tag("YapeNotifierApplication").d("Device UUID existente (ANDROID_ID): $existingUuid")
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
