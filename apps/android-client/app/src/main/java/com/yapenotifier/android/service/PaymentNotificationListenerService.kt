package com.yapenotifier.android.service

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.R
import com.yapenotifier.android.YapeNotifierApplication
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.local.db.CapturedNotification
import com.yapenotifier.android.data.repository.SettingsRepository
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.NetworkConnectivityReceiver
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceRebinder
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

    // Rate-limited heartbeat: only log once per minute to avoid flooding logs
    @Volatile private var lastHeartbeatLogAt: Long = 0L
    private val HEARTBEAT_LOG_INTERVAL_MS = 60_000L

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

        // CRITICAL: Start as foreground service to prevent Android from killing it
        startForegroundService()

        ServiceStatusManager.updateStatus("✅ Servicio Creado - ${monitoredPackages.size} apps")
        FileLogger.logServiceEvent("SERVICE_CREATED", "${monitoredPackages.size} packages")
        Log.i(TAG, "PaymentNotificationListenerService created with ${monitoredPackages.size} monitored packages")
    }

    /**
     * Start the service as a foreground service with a persistent notification.
     * This prevents Android from killing the service under memory pressure.
     */
    private fun startForegroundService() {
        try {
            val notification = NotificationCompat.Builder(
                this,
                YapeNotifierApplication.FOREGROUND_SERVICE_CHANNEL_ID
            )
                .setContentTitle("NotiCentral activo")
                .setContentText("Monitoreando notificaciones de pago")
                .setSmallIcon(R.drawable.ic_bell_notification)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ requires specifying foreground service type
                startForeground(
                    YapeNotifierApplication.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(
                    YapeNotifierApplication.FOREGROUND_SERVICE_NOTIFICATION_ID,
                    notification
                )
            }
            Log.i(TAG, "✅ Foreground service started successfully")
            FileLogger.log("SERVICE", "Foreground service started", "info")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start foreground service", e)
            FileLogger.logError("startForegroundService", e)
            // Service will continue without foreground status - less protected but still functional
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY tells Android to re-create the service if it was killed
        Log.d(TAG, "onStartCommand called (flags=$flags, startId=$startId)")
        return START_STICKY
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

        // Reset reconnection attempts on successful connection (both local and global)
        val previousAttempts = reconnectAttempts.getAndSet(0)
        ServiceStatusManager.resetRebindAttempts() // Reset watchdog counter too

        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")

        // Log success with network state info
        val hasNetwork = NetworkConnectivityReceiver.isNetworkAvailable(applicationContext)
        val details = buildString {
            if (previousAttempts > 0) append("after $previousAttempts attempts, ")
            else append("first connect, ")
            append("network=${if (hasNetwork) "available" else "unavailable"}")
        }
        FileLogger.logServiceEvent("LISTENER_CONNECTED", details)
        FileLogger.logReconnect(true, previousAttempts.coerceAtLeast(1), null)

        Log.i(TAG, "✅ Notification listener connected successfully ($details)")

        // Refresh monitored packages in background (only if network available)
        if (hasNetwork) {
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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        // Mark service as alive - any notification proves the listener is working
        ServiceStatusManager.serviceActivityDetected()

        // Rate-limited heartbeat log: proves listener is alive even without payment notifications
        val now = System.currentTimeMillis()
        if (now - lastHeartbeatLogAt > HEARTBEAT_LOG_INTERVAL_MS) {
            lastHeartbeatLogAt = now
            FileLogger.log("HEARTBEAT", "Listener alive - notification from ${sbn.packageName}", "info")
        }

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
        FileLogger.logServiceEvent("SERVICE_DESTROYED", "onDestroy called - persisted connected=false")
        Log.w(TAG, "PaymentNotificationListenerService destroyed. Collector job cancelled.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Mark service as DISCONNECTED
        ServiceStatusManager.setServiceConnected(false)

        val attempt = reconnectAttempts.incrementAndGet()

        // Log detailed disconnect info including network state
        val hasNetwork = NetworkConnectivityReceiver.isNetworkAvailable(applicationContext)
        val disconnectReason = buildString {
            append("LISTENER_DISCONNECTED - System unbind")
            append(", network=${if (hasNetwork) "available" else "UNAVAILABLE"}")
        }
        FileLogger.logDisconnect(disconnectReason, attempt)

        Log.w(TAG, "⚠️ Notification listener disconnected (attempt $attempt, network=$hasNetwork)")

        if (attempt <= MAX_RECONNECT_ATTEMPTS) {
            // Use ServiceRebinder's exponential backoff calculation
            val delayMs = ServiceRebinder.calculateBackoffDelay(attempt, baseDelayMs = 2000L, maxDelayMs = 32000L)

            ServiceStatusManager.updateStatus("🔌 Desconectado - Reconectando en ${delayMs/1000}s (intento $attempt/$MAX_RECONNECT_ATTEMPTS)")
            Log.w(TAG, "⚠️ Will attempt rebind in ${delayMs}ms (attempt $attempt/$MAX_RECONNECT_ATTEMPTS)")

            serviceScope.launch {
                try {
                    FileLogger.logReconnect(false, attempt, delayMs)
                    delay(delayMs)

                    // Use improved ServiceRebinder instead of direct requestRebind
                    Log.i(TAG, "🔄 Attempting service rebind using ServiceRebinder (attempt $attempt)")
                    ServiceStatusManager.updateStatus("🔄 Reconectando... (intento $attempt)")

                    val rebindSuccess = ServiceRebinder.rebindNotificationListener(applicationContext)

                    if (rebindSuccess) {
                        Log.i(TAG, "🔄 Rebind request sent successfully (attempt $attempt)")
                        ServiceStatusManager.updateStatus("🔄 Reconexión solicitada (intento $attempt)...")
                    } else {
                        Log.w(TAG, "⚠️ Rebind request failed (attempt $attempt)")
                        ServiceStatusManager.updateStatus("⚠️ Rebind fallido (intento $attempt)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error during rebind attempt $attempt", e)
                    FileLogger.logError("rebindAttempt", e)
                    ServiceStatusManager.updateStatus("❌ Error reconectando (intento $attempt)")
                }
            }
        } else {
            // Max attempts reached - let the watchdog handle it from here
            Log.e(TAG, "❌ Max reconnection attempts reached ($MAX_RECONNECT_ATTEMPTS). Watchdog will continue monitoring.")
            FileLogger.logDisconnect("MAX_ATTEMPTS_REACHED - Watchdog will continue", attempt)
            ServiceStatusManager.updateStatus("❌ Desconectado - Watchdog monitoreando...")

            // Note: We don't tell user to restart manually anymore.
            // The ServiceWatchdogWorker will continue trying every 15 minutes,
            // and NetworkConnectivityReceiver will try when network is restored.
        }
    }

    companion object {
        private const val TAG = "PaymentNotificationService"
    }
}
