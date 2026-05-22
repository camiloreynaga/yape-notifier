package com.yapenotifier.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.NetworkConnectivityReceiver
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceRebinder
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.worker.ServiceWatchdogWorker
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
        FileLogger.log("BUILD", "versionName=${BuildConfig.VERSION_NAME}, versionCode=${BuildConfig.VERSION_CODE}")

        // CRITICAL: Initialize ServiceStatusManager early — hydrates persisted state from
        // SharedPreferences and records processStartAt. Must run before bootstrapNotificationListener()
        // and any other code that reads ServiceStatusManager.isServiceConnected().
        ServiceStatusManager.init(this)

        // CRITICAL: Initialize AuthSessionManager synchronously. Registers appContext,
        // appScope and initialized=true immediately. Hydrates the awaitingLogin mirror
        // from DataStore in background. Must run before any code that may trigger a 401
        // (e.g. bootstrap, watchdog, etc.).
        com.yapenotifier.android.data.auth.AuthSessionManager.initialize(this, applicationScope)

        Timber.tag("YapeNotifierApplication").d("Application created. API URL: ${BuildConfig.API_BASE_URL}")
        
        // Create notification channels (required for Android 8.0+)
        createNotificationChannels()

        // CRITICAL: Generate and persist device UUID once on first app launch
        ensureDeviceUuid()

        // Register network connectivity callback for automatic service recovery
        NetworkConnectivityReceiver.registerNetworkCallback(this)
        FileLogger.log("SYSTEM", "Network callback registered")

        // CRITICAL: Bootstrap the NotificationListenerService on every process start.
        // When WorkManager restarts the app process in background, no Activity runs,
        // so the service never gets bound. This requestRebind() ensures the system
        // binds the NotificationListenerService regardless of how the process started.
        bootstrapNotificationListener()

        setupRecurringWork()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Unregister network callback when app terminates
        NetworkConnectivityReceiver.unregisterNetworkCallback(this)
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

    /**
     * Requests the system to bind our NotificationListenerService.
     *
     * This is the critical bootstrap: when the app process is created by WorkManager,
     * a boot receiver, or the system, there is no Activity to trigger the service.
     * requestRebind() asks the system to bind the NotificationListenerService,
     * which will call onCreate() → startForeground() → onListenerConnected().
     *
     * Guards:
     * - permissionEnabled=false → skip, only log
     * - serviceConnected=true → skip, no need to rebind
     * - lastBootstrapAt < BOOTSTRAP_COOLDOWN_MS → skip, anti-loop for OEM process churn
     *
     * Requires: notification access permission already granted by the user.
     */
    private fun bootstrapNotificationListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Timber.tag(TAG).w("requestRebind requires API 24+, skipping bootstrap")
            return
        }

        try {
            val hasPermission = NotificationAccessChecker.isNotificationAccessEnabled(this)
            val isConnected = ServiceStatusManager.isServiceConnected()
            val isComponentEnabled = ServiceRebinder.isServiceComponentEnabled(this)

            // Guard 1: No permission → skip
            if (!hasPermission) {
                Timber.tag(TAG).w("Bootstrap: permission not granted, skipping")
                FileLogger.log(
                    "BOOTSTRAP",
                    "Skipped - permissionEnabled=false, componentEnabled=$isComponentEnabled",
                    "warning"
                )
                return
            }

            // Guard 2: Already connected → skip (reduces noise on OEMs)
            if (isConnected) {
                Timber.tag(TAG).d("Bootstrap: service already connected, skipping")
                FileLogger.log(
                    "BOOTSTRAP",
                    "Skipped - serviceConnected=true (already healthy)",
                    "info"
                )
                return
            }

            // Guard 3: Cooldown anti-loop — prevent spam if OEM kills process in loop
            val prefs = getSharedPreferences(BOOTSTRAP_PREFS, MODE_PRIVATE)
            val lastBootstrapAt = prefs.getLong(KEY_LAST_BOOTSTRAP_AT, 0L)
            val now = System.currentTimeMillis()
            val elapsed = now - lastBootstrapAt

            if (elapsed < BOOTSTRAP_COOLDOWN_MS) {
                val remainingSec = (BOOTSTRAP_COOLDOWN_MS - elapsed) / 1000
                Timber.tag(TAG).w("Bootstrap: cooldown active (${remainingSec}s remaining), skipping")
                FileLogger.log(
                    "BOOTSTRAP",
                    "Skipped - cooldown active (${remainingSec}s remaining), " +
                        "permissionEnabled=true, componentEnabled=$isComponentEnabled",
                    "info"
                )
                return
            }

            // All guards passed → request rebind
            val componentName = NotificationAccessChecker.getServiceComponentName(this)
            NotificationListenerService.requestRebind(componentName)

            // Save timestamp for cooldown
            prefs.edit().putLong(KEY_LAST_BOOTSTRAP_AT, now).apply()

            Timber.tag(TAG).i("Bootstrap: requestRebind() called on process start")
            FileLogger.log(
                "BOOTSTRAP",
                "requestRebind() called - permissionEnabled=true, " +
                    "componentEnabled=$isComponentEnabled, serviceConnected=false",
                "info"
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Bootstrap: requestRebind() failed")
            FileLogger.log("BOOTSTRAP", "requestRebind() failed: ${e.message}", "error")
        }
    }

    /**
     * Creates notification channels required by the app.
     * Must be called before any notification is posted.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for the foreground service persistent notification
            val serviceChannel = NotificationChannel(
                FOREGROUND_SERVICE_CHANNEL_ID,
                "Servicio de monitoreo",
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound, shows in status bar
            ).apply {
                description = "Mantiene activo el monitoreo de notificaciones de pago"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Channel for alert notifications (permission lost, service disconnected)
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas del servicio",
                NotificationManager.IMPORTANCE_HIGH // High importance = sound + heads-up
            ).apply {
                description = "Alertas cuando el servicio de monitoreo necesita atención"
            }
            notificationManager.createNotificationChannel(alertChannel)

            Timber.tag("YapeNotifierApplication").d("Notification channels created")
        }
    }

    private fun setupRecurringWork() {
        val workManager = WorkManager.getInstance(applicationContext)

        // === 1. Settings Sync Worker (requires network, runs daily) ===
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val settingsSyncRequest = PeriodicWorkRequestBuilder<SyncSettingsWorker>(
            1, TimeUnit.DAYS
        )
            .setConstraints(networkConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncSettingsWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            settingsSyncRequest
        )

        Timber.tag("YapeNotifierApplication").d("Periodic sync worker for settings has been scheduled.")

        // === 2. Service Watchdog Worker (NO network required, runs every 15 minutes) ===
        // This worker monitors the NotificationListenerService health and attempts
        // to rebind if the service is disconnected.
        // IMPORTANT: Does NOT require network - it monitors local service state.
        val watchdogRequest = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
            15, TimeUnit.MINUTES // Minimum interval for PeriodicWork is 15 minutes
        )
            // No network constraint - watchdog should run regardless of connectivity
            .build()

        workManager.enqueueUniquePeriodicWork(
            ServiceWatchdogWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            watchdogRequest
        )

        Timber.tag("YapeNotifierApplication").d("Service watchdog worker has been scheduled (every 15 min).")
        FileLogger.log("SYSTEM", "Watchdog worker scheduled (15 min interval)")
    }

    companion object {
        private const val TAG = "YapeNotifierApplication"
        const val FOREGROUND_SERVICE_CHANNEL_ID = "payment_listener_service_channel"
        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1001
        const val ALERT_CHANNEL_ID = "service_alert_channel"

        // Bootstrap cooldown: prevent spamming requestRebind() if OEM kills process in loop
        private const val BOOTSTRAP_PREFS = "bootstrap_prefs"
        private const val KEY_LAST_BOOTSTRAP_AT = "last_bootstrap_at"
        private const val BOOTSTRAP_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes
    }
}
