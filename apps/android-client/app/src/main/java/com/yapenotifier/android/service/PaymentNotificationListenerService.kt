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
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.worker.SendNotificationWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

class PaymentNotificationListenerService : NotificationListenerService() {

    // CRITICAL FIX: Exception handler to prevent coroutine crashes from killing the service
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "❌ Coroutine exception caught (service NOT killed)", throwable)
        FileLogger.logError("CoroutineException", throwable)
        ServiceStatusManager.updateStatus("⚠️ Error interno recuperado")
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository

    // FIXED: Usar @Volatile para visibilidad entre threads y usar DEFAULT_PACKAGES del repository
    @Volatile
    private var monitoredPackages: Set<String> = SettingsRepository.DEFAULT_PACKAGES

    // FIXED: Job para el collector reactivo de paquetes
    private var packagesCollectorJob: Job? = null

    // Reconnection retry counter for exponential backoff
    private val reconnectAttempts = AtomicInteger(0)
    private val MAX_RECONNECT_ATTEMPTS = 5

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)

        // CRITICAL FIX: Load packages ASYNCHRONOUSLY with timeout to avoid ANR
        // runBlocking was causing the service to hang on slow DataStore reads
        // Use default packages immediately, then update when loaded
        serviceScope.launch {
            try {
                // Timeout of 3 seconds - if DataStore is slow, use defaults
                val packages = withTimeoutOrNull(3000L) {
                    settingsRepository.monitoredPackagesFlow.first()
                }
                if (packages != null) {
                    monitoredPackages = packages
                    Log.i(TAG, "✅ Initial monitored packages loaded: $monitoredPackages")
                    ServiceStatusManager.updateStatus("📦 ${monitoredPackages.size} apps cargadas")
                } else {
                    Log.w(TAG, "⚠️ Timeout loading packages, using ${monitoredPackages.size} defaults")
                    ServiceStatusManager.updateStatus("⚠️ Timeout cargando apps, usando ${monitoredPackages.size} por defecto")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading monitored packages, using defaults: ${SettingsRepository.DEFAULT_PACKAGES}", e)
            }
        }

        // FIXED: Iniciar collector reactivo para actualizaciones en tiempo real
        startPackagesCollector()

        ServiceStatusManager.updateStatus("✅ Servicio Creado - ${monitoredPackages.size} apps")
        FileLogger.logServiceEvent("SERVICE_CREATED", "${monitoredPackages.size} packages")
        Log.i(TAG, "PaymentNotificationListenerService created with ${monitoredPackages.size} monitored packages")
    }

    /**
     * FIXED: Inicia un collector que escucha cambios en los paquetes monitoreados.
     * Now with automatic restart on failure using catch operator.
     */
    private fun startPackagesCollector() {
        packagesCollectorJob?.cancel()
        packagesCollectorJob = serviceScope.launch {
            settingsRepository.monitoredPackagesFlow
                .catch { e ->
                    Log.e(TAG, "❌ Error in packages flow, restarting collector in 5s", e)
                    delay(5000)
                    startPackagesCollector() // Restart the collector on failure
                }
                .collectLatest { packages ->
                    val oldCount = monitoredPackages.size
                    monitoredPackages = packages
                    if (packages.size != oldCount) {
                        Log.i(TAG, "🔄 Monitored packages updated: ${packages.size} apps (was $oldCount)")
                        ServiceStatusManager.updateStatus("🔄 ${packages.size} apps monitoreadas")
                    }
                }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Mark service as CONNECTED - this is the REAL state
        ServiceStatusManager.setServiceConnected(true)
        // Reset reconnection attempts on successful connection
        val previousAttempts = reconnectAttempts.getAndSet(0)
        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")
        FileLogger.logServiceEvent("LISTENER_CONNECTED", if (previousAttempts > 0) "after $previousAttempts attempts" else "first connect")
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

        // CRITICAL: Wrap entire notification processing in try-catch to prevent service crashes
        try {
            processNotification(sbn)
        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL: Exception in onNotificationPosted (service protected)", e)
            FileLogger.logError("onNotificationPosted", e)
            ServiceStatusManager.updateStatus("⚠️ Error procesando notificación")
        }
    }

    /**
     * Process a notification. Separated from onNotificationPosted for better error handling.
     */
    private fun processNotification(sbn: StatusBarNotification) {
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
        FileLogger.logServiceEvent("SERVICE_DESTROYED", "onDestroy called - SYSTEM KILLED SERVICE")
        Log.w(TAG, "PaymentNotificationListenerService destroyed. Collector job cancelled.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Mark service as DISCONNECTED
        ServiceStatusManager.setServiceConnected(false)

        val attempt = reconnectAttempts.incrementAndGet()
        FileLogger.logDisconnect("LISTENER_DISCONNECTED - System unbind", attempt)

        if (attempt <= MAX_RECONNECT_ATTEMPTS) {
            // Exponential backoff: 2s, 4s, 8s, 16s, 32s
            val delayMs = (2000L * (1L shl (attempt - 1))).coerceAtMost(32000L)
            ServiceStatusManager.updateStatus("🔌 Desconectado - Reconectando en ${delayMs/1000}s (intento $attempt/$MAX_RECONNECT_ATTEMPTS)")
            Log.w(TAG, "⚠️ Notification listener disconnected. Reconnect attempt $attempt/$MAX_RECONNECT_ATTEMPTS in ${delayMs}ms")

            serviceScope.launch {
                try {
                    FileLogger.logReconnect(false, attempt, delayMs)
                    delay(delayMs)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val componentName = ComponentName(applicationContext, PaymentNotificationListenerService::class.java)
                        requestRebind(componentName)
                        Log.i(TAG, "🔄 Rebind requested automatically (attempt $attempt)")
                        ServiceStatusManager.updateStatus("🔄 Reconexión solicitada (intento $attempt)...")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error requesting rebind (attempt $attempt)", e)
                    FileLogger.logError("requestRebind", e)
                    ServiceStatusManager.updateStatus("❌ Error reconectando (intento $attempt)")
                }
            }
        } else {
            Log.e(TAG, "❌ Max reconnection attempts reached ($MAX_RECONNECT_ATTEMPTS). Service requires manual restart.")
            FileLogger.logDisconnect("MAX_ATTEMPTS_REACHED - Manual restart required", attempt)
            ServiceStatusManager.updateStatus("❌ Desconectado - Reinicia permisos manualmente")
        }
    }

    companion object {
        private const val TAG = "PaymentNotificationService"
    }
}
