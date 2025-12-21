package com.yapenotifier.android.service

import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
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

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: AppDatabase
    private lateinit var settingsRepository: SettingsRepository

    private var monitoredPackages: Set<String> = setOf()

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        
        serviceScope.launch {
            monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
            Log.d(TAG, "Initial monitored packages: $monitoredPackages")
        }

        ServiceStatusManager.updateStatus("✅ Servicio Creado")
        Log.d(TAG, "PaymentNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")
        Log.i(TAG, "Notification listener connected.")
        serviceScope.launch {
            settingsRepository.refreshMonitoredPackages()
            monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
            Log.d(TAG, "Refreshed monitored packages: $monitoredPackages")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        val packageName = sbn.packageName
        if (!monitoredPackages.contains(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val title = notification.extras?.getString("android.title") ?: ""
        val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""

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
        val androidUid = sbn.uid // Optional UID
        val postedAt = sbn.postTime // Original notification timestamp

        val paymentDetails = PaymentNotificationParser.parse(title, text)
        
        if (paymentDetails != null) {
            val instanceInfo = if (androidUserId != null) {
                " (User $androidUserId)"
            } else {
                ""
            }
            ServiceStatusManager.updateStatus("📬 Notificación de pago recibida de: $packageName$instanceInfo")
            
            serviceScope.launch {
                // IMPORTANT: Save the ORIGINAL title and body, not the parsed version
                // This ensures we can send the exact notification content to the API
                val capturedNotification = CapturedNotification(
                    packageName = packageName,
                    androidUserId = androidUserId,
                    androidUid = androidUid,
                    title = title, // Original title from notification
                    body = text,   // Original body text from notification
                    postedAt = postedAt
                )
                db.capturedNotificationDao().insert(capturedNotification)
                Log.i(TAG, "Payment notification saved locally. Package: $packageName, UserId: $androidUserId, Uid: $androidUid, Title: '$title', Body: '$text'")
                ServiceStatusManager.updateStatus("💾 Guardado localmente.")

                scheduleSendNotificationWorker()
                ServiceStatusManager.updateStatus("👷 Trabajo de envío planificado.")
            }
        } else {
            Log.d(TAG, "Notification from $packageName did not match payment pattern or was filtered out. Title='$title', Text='$text'")
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
