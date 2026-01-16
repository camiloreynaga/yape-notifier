package com.yapenotifier.android.service

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.local.db.CapturedNotification
import com.yapenotifier.android.data.repository.SettingsRepository
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.worker.SendNotificationWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository

    // FIXED: Usar @Volatile para visibilidad entre threads y usar DEFAULT_PACKAGES del repository
    @Volatile
    private var monitoredPackages: Set<String> = SettingsRepository.DEFAULT_PACKAGES
    
    // FIXED: Job para el collector reactivo de paquetes
    private var packagesCollectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        
        // CRITICAL FIX: Use runBlocking to ensure packages are loaded before service starts processing
        // This prevents race condition where notifications arrive before packages are loaded
        try {
            runBlocking(Dispatchers.IO) {
                monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
                Log.i(TAG, "✅ Initial monitored packages loaded: $monitoredPackages")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading monitored packages, using defaults: ${SettingsRepository.DEFAULT_PACKAGES}", e)
            monitoredPackages = SettingsRepository.DEFAULT_PACKAGES
        }
        
        // FIXED: Iniciar collector reactivo para actualizaciones en tiempo real
        startPackagesCollector()

        ServiceStatusManager.updateStatus("✅ Servicio Creado - ${monitoredPackages.size} apps")
        Log.i(TAG, "PaymentNotificationListenerService created with ${monitoredPackages.size} monitored packages")
    }
    
    /**
     * FIXED: Inicia un collector que escucha cambios en los paquetes monitoreados.
     * Esto permite actualizar la lista sin reiniciar el servicio.
     */
    private fun startPackagesCollector() {
        packagesCollectorJob?.cancel()
        packagesCollectorJob = serviceScope.launch {
            try {
                settingsRepository.monitoredPackagesFlow.collectLatest { packages ->
                    val oldCount = monitoredPackages.size
                    monitoredPackages = packages
                    if (packages.size != oldCount) {
                        Log.i(TAG, "🔄 Monitored packages updated: ${packages.size} apps (was $oldCount)")
                        ServiceStatusManager.updateStatus("🔄 ${packages.size} apps monitoreadas")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in packages collector", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Mark service as CONNECTED - this is the REAL state
        ServiceStatusManager.setServiceConnected(true)
        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")
        Log.i(TAG, "✅ Notification listener connected successfully")

        // Refresh monitored packages in background
        serviceScope.launch {
            try {
                settingsRepository.refreshMonitoredPackages()
                monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
                Log.i(TAG, "✅ Refreshed monitored packages: $monitoredPackages (total: ${monitoredPackages.size})")
                ServiceStatusManager.updateStatus("📦 ${monitoredPackages.size} apps monitoreadas")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error refreshing monitored packages", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        
        // Log ALL notifications for debugging (even if not monitored)
        Log.d(TAG, "📬 Notification received from package: $packageName")
        Log.d(TAG, "   Monitored packages: $monitoredPackages")
        Log.d(TAG, "   Is monitored: ${monitoredPackages.contains(packageName)}")
        
        if (!monitoredPackages.contains(packageName)) {
            Log.d(TAG, "⏭️ Skipping notification from unmonitored package: $packageName")
            return
        }

        val notification = sbn.notification ?: run {
            Log.w(TAG, "⚠️ Notification object is null for package: $packageName")
            return
        }
        
        // CRITICAL FIX: Extract title and text using multiple methods for maximum compatibility
        // Try NotificationCompat constants first (most reliable), then fallback to direct extras
        val extras = notification.extras ?: android.os.Bundle()
        
        // Extract title - try multiple sources
        val finalTitle = extras.getCharSequence(NotificationCompat.EXTRA_TITLE)?.toString()
            ?: extras.getString(NotificationCompat.EXTRA_TITLE)
            ?: extras.getCharSequence("android.title")?.toString()
            ?: extras.getString("android.title")
            ?: ""
        
        // Extract text - try multiple sources (including BigTextStyle)
        val finalText = extras.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
            ?: extras.getString(NotificationCompat.EXTRA_TEXT)
            ?: extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT)?.toString() // BigTextStyle
            ?: extras.getString(NotificationCompat.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence("android.text")?.toString()
            ?: extras.getString("android.text")
            ?: extras.getCharSequence("android.bigText")?.toString() // Fallback for big text
            ?: extras.getString("android.bigText")
            ?: ""
        
        Log.d(TAG, "📋 Notification content - Package: $packageName, Title: '$finalTitle', Text: '$finalText'")

        // Capture dual app identifiers (CRITICAL for MIUI and other dual app systems)
        // UserHandle.getIdentifier() is available from API 24 (our minSdk)
        // This is the CORRECT way to get a unique identifier for dual app instances
        // hashCode() is NOT reliable as it can change between app restarts
        @Suppress("DEPRECATION")
        val androidUserId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sbn.userId
        } else {
            null // Should not happen as minSdk is 24, but safe fallback
        }
        val androidUid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sbn.uid
        } else {
            try {
                applicationContext.packageManager.getApplicationInfo(sbn.packageName, 0).uid
            } catch (e: PackageManager.NameNotFoundException) {
                -1 // Fallback value
            }
        }
        val postedAt = sbn.postTime // Original notification timestamp

        // Use finalTitle and finalText (extracted with NotificationCompat)
        val paymentDetails = PaymentNotificationParser.parse(finalTitle, finalText)
        
        if (paymentDetails != null) {
            val instanceInfo = if (androidUserId != null) {
                " (User $androidUserId)"
            } else {
                ""
            }
            ServiceStatusManager.updateStatus("📬 Notificación de pago recibida de: $packageName$instanceInfo")
            Log.i(TAG, "✅ Payment detected! Sender: ${paymentDetails.sender}, Amount: ${paymentDetails.amount} ${paymentDetails.currency}")
            
            serviceScope.launch {
                // IMPORTANT: Save the ORIGINAL title and body, not the parsed version
                // This ensures we can send the exact notification content to the API
                val capturedNotification = CapturedNotification(
                    packageName = packageName,
                    androidUserId = androidUserId,
                    androidUid = androidUid,
                    title = finalTitle, // Original title from notification
                    body = finalText,   // Original body text from notification
                    postedAt = postedAt
                )
                db.capturedNotificationDao().insert(capturedNotification)
                Log.i(TAG, "💾 Payment notification saved locally. Package: $packageName, UserId: $androidUserId, Uid: $androidUid, Title: '$finalTitle', Body: '$finalText'")
                // Mark that we successfully captured a notification
                ServiceStatusManager.notificationCaptured()
                ServiceStatusManager.updateStatus("💾 Guardado localmente.")

                scheduleSendNotificationWorker()
                ServiceStatusManager.updateStatus("👷 Trabajo de envío planificado.")
                Log.d(TAG, "👷 SendNotificationWorker scheduled")
            }
        } else {
            Log.d(TAG, "⏭️ Notification from $packageName did not match payment pattern or was filtered out. Title='$finalTitle', Text='$finalText'")
        }
    }

    private fun scheduleSendNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendWorkRequest = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(sendWorkRequest)
        Log.d(TAG, "OneTime work request to send notifications has been enqueued.")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Mark service as DISCONNECTED
        ServiceStatusManager.setServiceConnected(false)
        // FIXED: Cancelar el collector job para evitar memory leaks
        packagesCollectorJob?.cancel()
        packagesCollectorJob = null
        ServiceStatusManager.updateStatus("❌ Servicio Destruido")
        Log.w(TAG, "PaymentNotificationListenerService destroyed. Collector job cancelled.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Mark service as DISCONNECTED
        ServiceStatusManager.setServiceConnected(false)
        ServiceStatusManager.updateStatus("🔌 Desconectado - Reconectando...")
        Log.w(TAG, "⚠️ Notification listener disconnected. Attempting auto-reconnect...")
        
        // FIXED: Intentar reconectar automáticamente
        serviceScope.launch {
            try {
                // Esperar un poco antes de intentar reconectar
                delay(2000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val componentName = ComponentName(applicationContext, PaymentNotificationListenerService::class.java)
                    requestRebind(componentName)
                    Log.i(TAG, "🔄 Rebind requested automatically")
                    ServiceStatusManager.updateStatus("🔄 Reconexión solicitada...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error requesting rebind", e)
                ServiceStatusManager.updateStatus("❌ Error al reconectar")
            }
        }
    }

    companion object {
        private const val TAG = "PaymentNotificationService"
    }
}
