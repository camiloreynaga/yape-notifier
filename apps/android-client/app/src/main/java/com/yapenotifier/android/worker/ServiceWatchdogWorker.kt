package com.yapenotifier.android.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.R
import com.yapenotifier.android.YapeNotifierApplication
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceRebinder
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Watchdog Worker que monitorea el estado de conexión del NotificationListenerService.
 *
 * Se ejecuta cada 15 minutos y:
 * 1. Verifica si el permiso de Acceso a Notificaciones sigue activo
 * 2. Si se perdió el permiso: muestra notificación al usuario para reactivarlo
 * 3. Verifica si el servicio reporta serviceConnected=true
 * 4. Si está desconectado: intenta requestRebind() (seguro, no revoca permisos)
 * 5. Después de 3 intentos fallidos: muestra notificación al usuario
 * 6. Loguea diagnóstico completo: permissionEnabled, componentEnabled, timestamps
 *
 * ELIMINADO: La detección de "stale" (servicio conectado pero sin actividad reciente)
 * fue removida porque causaba que el watchdog matara un servicio funcional.
 * Si serviceConnected=true, el servicio está vivo — NO SE TOCA.
 *
 * IMPORTANTE: NO requiere conectividad de red.
 * IMPORTANTE: Component toggle fue ELIMINADO — revocaba el permiso de notificaciones.
 */
class ServiceWatchdogWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Safety net: ensure ServiceStatusManager is hydrated in this process
        ServiceStatusManager.init(applicationContext)

        // --- WorkManager diagnostics ---
        Log.d(TAG, "ServiceWatchdogWorker starting health check " +
                "(runAttempt=$runAttemptCount, isStopped=$isStopped, id=$id)")
        FileLogger.log(
            "WATCHDOG",
            "Starting - runAttempt=$runAttemptCount, isStopped=$isStopped",
            "info"
        )

        try {
            // Step 0: Check if a recent bootstrap already requested rebind
            // If Application.onCreate() bootstrap ran < 3 min ago, skip rebind
            // to avoid concurrent rebind requests that produce noise/churn.
            val bootstrapPrefs = applicationContext.getSharedPreferences(
                BOOTSTRAP_PREFS, Context.MODE_PRIVATE
            )
            val lastBootstrapAt = bootstrapPrefs.getLong(KEY_LAST_BOOTSTRAP_AT, 0L)
            val timeSinceBootstrap = System.currentTimeMillis() - lastBootstrapAt
            val recentBootstrap = timeSinceBootstrap < BOOTSTRAP_GRACE_PERIOD_MS

            // Step 1: Check if notification access permission is granted
            val hasPermission = NotificationAccessChecker.isNotificationAccessEnabled(applicationContext)

            if (!hasPermission) {
                Log.w(TAG, "Notification access permission NOT granted")
                val isComponentEnabled = ServiceRebinder.isServiceComponentEnabled(applicationContext)
                FileLogger.log(
                    "HEALTH",
                    "serviceConnected=false, " +
                        "permissionEnabled=false, " +
                        "componentEnabled=$isComponentEnabled, " +
                        "action=showingNotificationToUser",
                    "error"
                )
                // Show a notification to the user to re-enable permission
                showPermissionLostNotification()
                // Reset attempts since we can't do anything without permission
                ServiceStatusManager.resetRebindAttempts()
                return@withContext Result.success()
            }

            // Step 2: Recopilar estado completo del servicio
            val isComponentEnabled = ServiceRebinder.isServiceComponentEnabled(applicationContext)
            val isConnected = ServiceStatusManager.isServiceConnected()
            val now = System.currentTimeMillis()

            // Timestamps de actividad
            val lastActivity = ServiceStatusManager.getLastServiceActivityAt()
            val lastConnected = ServiceStatusManager.getLastListenerConnectedAt()
            val lastNotification = ServiceStatusManager.getLastNotificationCapturedAt()

            val timeSinceLastActivity = if (lastActivity != null) now - lastActivity else Long.MAX_VALUE
            val timeSinceLastConnected = if (lastConnected != null) now - lastConnected else Long.MAX_VALUE
            val timeSinceLastNotification = if (lastNotification != null) now - lastNotification else Long.MAX_VALUE

            val currentAttempts = ServiceStatusManager.getRebindAttempts()

            // --- HEALTH LOG con los 3 flags diagnósticos explícitos ---
            // 1. permissionEnabled: ¿Acceso a notificaciones sigue habilitado?
            // 2. componentEnabled: ¿El componente listener está habilitado en PackageManager?
            // 3. lastConnectedAt / lastActivityAt / lastNotificationAt: timestamps de vida
            fun formatMinAgo(ms: Long): String = if (ms == Long.MAX_VALUE) "never" else "${ms / 60000}min"

            FileLogger.log(
                "HEALTH",
                "serviceConnected=$isConnected, " +
                    "permissionEnabled=true, " +
                    "componentEnabled=$isComponentEnabled, " +
                    "lastConnectedAt=${formatMinAgo(timeSinceLastConnected)}, " +
                    "lastActivityAt=${formatMinAgo(timeSinceLastActivity)}, " +
                    "lastNotificationAt=${formatMinAgo(timeSinceLastNotification)}, " +
                    "rebindAttempts=$currentAttempts",
                if (isConnected) "info" else "warning"
            )

            Log.d(TAG, "Service status: connected=$isConnected, " +
                    "permissionEnabled=true, componentEnabled=$isComponentEnabled, " +
                    "lastConnected=${formatMinAgo(timeSinceLastConnected)}, " +
                    "lastActivity=${formatMinAgo(timeSinceLastActivity)}, " +
                    "lastNotification=${formatMinAgo(timeSinceLastNotification)}, " +
                    "rebindAttempts=$currentAttempts")

            // Step 3: Determinar si necesitamos rebind
            // isServiceConnected() ahora es recency-based (actividad < SERVICE_IDLE_TIMEOUT_MS),
            // así que no necesitamos un safety-net duplicado aquí.
            // Si el listener tiene actividad reciente → isConnected=true → no tocar.
            // Si no tiene actividad reciente → isConnected=false → intentar rebind.
            val needsRebind = when {
                !isConnected && isComponentEnabled -> {
                    Log.w(TAG, "Service disconnected (no recent activity) - needs rebind")
                    true
                }
                !isComponentEnabled -> {
                    Log.w(TAG, "Component is disabled - needs rebind")
                    true
                }
                else -> {
                    Log.d(TAG, "Service is healthy (connected=$isConnected) - no action needed")
                    ServiceStatusManager.resetRebindAttempts()
                    dismissAlertNotifications()
                    false
                }
            }

            // Step 4: Attempt safe rebind if needed
            if (needsRebind) {
                // Guard: if bootstrap ran recently, let it finish before we interfere
                if (recentBootstrap) {
                    val remainingSec = (BOOTSTRAP_GRACE_PERIOD_MS - timeSinceBootstrap) / 1000
                    Log.i(TAG, "Recent bootstrap detected (${remainingSec}s ago) - skipping watchdog rebind")
                    FileLogger.log(
                        "WATCHDOG",
                        "Skipped rebind - recent bootstrap (${remainingSec}s remaining grace period)",
                        "info"
                    )
                    return@withContext Result.success()
                }

                // Check if WorkManager cancelled us before we attempt rebind
                if (isStopped) {
                    Log.w(TAG, "Worker stopped before rebind attempt - aborting")
                    FileLogger.log(
                        "WATCHDOG",
                        "Worker stopped/cancelled before rebind - runAttempt=$runAttemptCount",
                        "warning"
                    )
                    return@withContext Result.success()
                }

                val currentAttempt = ServiceStatusManager.incrementRebindAttempt()

                FileLogger.logReconnect(
                    success = false,
                    attempt = currentAttempt,
                    delayMs = null
                )

                Log.i(TAG, "Attempting safe rebind (attempt $currentAttempt)")

                // Snapshot activity BEFORE rebind to detect genuine new events
                val beforeActivity = ServiceStatusManager.getLastServiceActivityAt()

                // ONLY safe rebind - requestRebind() cannot revoke permission
                val rebindSuccess = ServiceRebinder.rebindNotificationListener(applicationContext)

                if (rebindSuccess) {
                    Log.i(TAG, "Rebind request sent - waiting for service connection...")

                    // Wait for the system to process the rebind
                    delay(REBIND_CHECK_DELAY_MS)

                    val afterActivity = ServiceStatusManager.getLastServiceActivityAt()
                    val connectedAfterRebind = ServiceStatusManager.isServiceConnected()

                    // New activity = timestamp advanced (not just a stale flag)
                    val hasNewActivity = when {
                        afterActivity == null -> false
                        beforeActivity == null -> true
                        else -> afterActivity > beforeActivity
                    }

                    if (connectedAfterRebind && hasNewActivity) {
                        Log.i(TAG, "Service successfully reconnected after rebind!")
                        FileLogger.logReconnect(
                            success = true,
                            attempt = currentAttempt,
                            delayMs = REBIND_CHECK_DELAY_MS
                        )
                        ServiceStatusManager.resetRebindAttempts()
                        dismissAlertNotifications()
                    } else {
                        val isRealDisconnect = ServiceStatusManager.hasRealDisconnect()
                        Log.w(TAG, "Service still disconnected after rebind attempt $currentAttempt " +
                                "(connected=$connectedAfterRebind, newActivity=$hasNewActivity, realDisconnect=$isRealDisconnect)")
                        FileLogger.log(
                            "SERVICE",
                            "Watchdog: Rebind attempt $currentAttempt - still disconnected " +
                                "(connected=$connectedAfterRebind, newActivity=$hasNewActivity, realDisconnect=$isRealDisconnect)",
                            "warning"
                        )

                        // Only alert the user if there was a REAL disconnect event
                        // (onListenerDisconnected/onDestroy fired), not just idle timeout.
                        // Idle timeout during quiet hours (night) is normal — the listener
                        // is alive but no notifications are arriving.
                        if (currentAttempt >= NOTIFY_USER_AFTER_ATTEMPTS && isRealDisconnect) {
                            showServiceDisconnectedNotification()
                        }
                    }
                } else {
                    Log.e(TAG, "Rebind request failed (attempt $currentAttempt)")
                    FileLogger.log(
                        "ERROR",
                        "Watchdog: Rebind request failed",
                        "error",
                        "attempt=$currentAttempt"
                    )
                }
            } else if (isConnected) {
                // Service is healthy - reset any failure counters
                ServiceStatusManager.resetRebindAttempts()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ServiceWatchdogWorker", e)
            FileLogger.logError("ServiceWatchdogWorker", e)
            Result.retry()
        }
    }

    /**
     * Shows a notification alerting the user that notification access permission was lost.
     * Tapping the notification opens the system Notification Listener Settings.
     */
    private fun showPermissionLostNotification() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                applicationContext,
                YapeNotifierApplication.ALERT_CHANNEL_ID
            )
                .setContentTitle("NotiCentral desconectado")
                .setContentText("Permiso de notificaciones perdido. Toca para reactivar.")
                .setSmallIcon(R.drawable.ic_bell_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.notify(PERMISSION_LOST_NOTIFICATION_ID, notification)

            Log.i(TAG, "Permission-lost notification shown to user")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show permission-lost notification", e)
        }
    }

    /**
     * Shows a notification alerting the user that the service is disconnected
     * and could not be automatically recovered.
     *
     * The notification opens ACTION_NOTIFICATION_LISTENER_SETTINGS so the user can
     * toggle the permission off and on — this is the most reliable manual recovery
     * on OEMs that block automatic rebind.
     */
    private fun showServiceDisconnectedNotification() {
        try {
            // Open Notification Listener Settings — user can toggle off/on to force rebind
            val settingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 1, settingsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                applicationContext,
                YapeNotifierApplication.ALERT_CHANNEL_ID
            )
                .setContentTitle("NotiCentral necesita atención")
                .setContentText("Servicio desconectado. Toca para reactivar permiso de notificaciones.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("El servicio no pudo reconectarse automáticamente. " +
                        "Toca para abrir Ajustes y desactiva/reactiva el permiso de NotiCentral."))
                .setSmallIcon(R.drawable.ic_bell_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.notify(SERVICE_DISCONNECTED_NOTIFICATION_ID, notification)

            Log.i(TAG, "Service-disconnected notification shown to user (opens notification listener settings)")
            FileLogger.log(
                "WATCHDOG",
                "Showing actionable notification to user - open notification listener settings",
                "warning"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show service-disconnected notification", e)
        }
    }

    /**
     * Dismisses the permission-lost and service-disconnected notifications.
     */
    private fun dismissAlertNotifications() {
        try {
            val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.cancel(PERMISSION_LOST_NOTIFICATION_ID)
            notificationManager.cancel(SERVICE_DISCONNECTED_NOTIFICATION_ID)
        } catch (_: Exception) {
            // Ignore
        }
    }

    companion object {
        const val TAG = "ServiceWatchdogWorker"
        const val WORK_NAME = "service_watchdog_worker"

        // Wait 5 seconds after rebind to check if connected
        private const val REBIND_CHECK_DELAY_MS = 5000L

        // After this many failed rebind attempts, show a notification to the user
        private const val NOTIFY_USER_AFTER_ATTEMPTS = 3

        // Notification IDs for user alerts
        private const val PERMISSION_LOST_NOTIFICATION_ID = 2001
        private const val SERVICE_DISCONNECTED_NOTIFICATION_ID = 2002

        // Bootstrap grace period: skip watchdog rebind if bootstrap ran recently
        // Must match the SharedPreferences keys used in YapeNotifierApplication
        private const val BOOTSTRAP_PREFS = "bootstrap_prefs"
        private const val KEY_LAST_BOOTSTRAP_AT = "last_bootstrap_at"
        private const val BOOTSTRAP_GRACE_PERIOD_MS = 3 * 60 * 1000L // 3 minutes
    }
}
