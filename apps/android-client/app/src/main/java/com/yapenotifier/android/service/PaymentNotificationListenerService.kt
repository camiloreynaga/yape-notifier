package com.yapenotifier.android.service

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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository

    // CRITICAL: Initialize with default packages to avoid race condition
    // This ensures we can capture notifications even if the API call hasn't completed
    private var monitoredPackages: Set<String> = setOf("com.bcp.innovacxion.yape.movil")

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
            Log.e(TAG, "❌ Error loading monitored packages, using defaults", e)
            // Keep default packages if loading fails
        }

        ServiceStatusManager.updateStatus("✅ Servicio Creado")
        Log.i(TAG, "PaymentNotificationListenerService created with ${monitoredPackages.size} monitored packages")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
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
        ServiceStatusManager.updateStatus("❌ Servicio Destruido")
        Log.w(TAG, "PaymentNotificationListenerService destroyed.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        ServiceStatusManager.updateStatus("🔌 Servicio Desconectado")
    }

    companion object {
        private const val TAG = "PaymentNotificationService"
    }
}
