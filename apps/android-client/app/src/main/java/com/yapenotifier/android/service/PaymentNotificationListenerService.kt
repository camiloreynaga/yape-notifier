package com.yapenotifier.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.local.db.CapturedNotification
import com.yapenotifier.android.data.repository.SettingsRepository
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
        
        // Load monitored packages into memory
        serviceScope.launch {
            monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
            Log.d(TAG, "Initial monitored packages loaded: $monitoredPackages")
        }

        ServiceStatusManager.updateStatus("✅ Servicio Creado")
        Log.d(TAG, "PaymentNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")
        Log.i(TAG, "Notification listener connected.")
        // Refresh packages on connect
        serviceScope.launch {
            settingsRepository.refreshMonitoredPackages()
            monitoredPackages = settingsRepository.monitoredPackagesFlow.first()
            Log.d(TAG, "Refreshed monitored packages on connect: $monitoredPackages")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName ?: return
        ServiceStatusManager.updateStatus("📬 Notificación recibida de: $packageName")
        
        // Check against the dynamic list
        if (!monitoredPackages.contains(packageName) && packageName != "com.yapenotifier.android") {
            ServiceStatusManager.updateStatus("⚠️ Ignorando (paquete no monitoreado)")
            return
        }

        val notification = sbn.notification
        val title = notification.extras?.getString("android.title") ?: return
        val text = notification.extras?.getString("android.text") ?: ""

        ServiceStatusManager.updateStatus("✅ Procesando: $title")

        serviceScope.launch {
            val capturedNotification = CapturedNotification(
                packageName = packageName,
                title = title,
                body = text
            )
            db.capturedNotificationDao().insert(capturedNotification)
            Log.i(TAG, "Notification saved locally with status PENDING.")
            ServiceStatusManager.updateStatus("💾 Guardado localmente.")

            scheduleSendNotificationWorker()
            ServiceStatusManager.updateStatus("👷 Trabajo de envío planificado.")
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
