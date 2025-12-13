package com.yapenotifier.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yapenotifier.android.data.parser.NotificationParser
import com.yapenotifier.android.data.repository.NotificationRepository
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaymentNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = NotificationParser()
    private lateinit var repository: NotificationRepository

    // Package names of payment apps to monitor
    private val paymentAppPackages = setOf(
        "com.yapenotifier.android", // TEMPORARY for testing
        
        // Interbank
        "com.interbank.mobilebanking", // Legacy package
        "pe.com.interbank.mobilebanking", // CORRECT package found in testing
        
        // BCP
        "com.bcp.bancadigital", 
        
        // BBVA
        "com.bbva.bbvacontinental",
        
        // Scotiabank
        "com.scotiabank.mobile",
        
        // Yape
        "com.yape.android", // Legacy package
        "com.bcp.innovacxion.yapeapp", // CORRECT package found in testing
        
        // Plin
        "com.plin.android"
    )

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(applicationContext)
        ServiceStatusManager.updateStatus("✅ Servicio Creado")
        Log.d(TAG, "PaymentNotificationListenerService created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        ServiceStatusManager.updateStatus("🚀 ¡Conectado! Escuchando notificaciones.")
        Log.i(TAG, "Notification listener connected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName ?: run {
            Log.w(TAG, "onNotificationPosted: packageName is null, ignoring")
            return
        }
        
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras?.getCharSequence("android.title")?.toString() ?: "N/A"
        
        Log.d(TAG, "Notification POSTED from: $packageName, Title: $title")
        ServiceStatusManager.updateStatus("📬 Notificación recibida de: $packageName")
        
        // Check if it's a payment app
        if (!paymentAppPackages.contains(packageName)) {
            ServiceStatusManager.updateStatus("⚠️ Ignorando (paquete no monitoreado): $packageName")
            return
        }
        
        Log.i(TAG, "Processing payment notification from: $packageName")
        ServiceStatusManager.updateStatus("✅ Procesando notificación de pago: $packageName")

        serviceScope.launch {
            try {
                val currentTitle = extras?.getCharSequence("android.title")?.toString()
                if (currentTitle.isNullOrEmpty()) {
                    Log.w(TAG, "Notification from $packageName has no title, skipping")
                    ServiceStatusManager.updateStatus("⚠️ Notificación sin título, ignorando")
                    return@launch
                }
                
                val text = extras?.getCharSequence("android.text")?.toString() ?: ""
                val bigText = extras?.getCharSequence("android.bigText")?.toString()
                
                val fullText = buildString {
                    append(currentTitle)
                    if (text.isNotEmpty()) {
                        appendLine()
                        append(text)
                    }
                    if (bigText?.isNotEmpty() == true) {
                        appendLine()
                        append(bigText)
                    }
                }.trim()

                ServiceStatusManager.updateStatus("🔍 Procesando: $currentTitle")

                val parsedData = parser.parseNotification(
                    packageName = packageName,
                    title = currentTitle,
                    body = fullText
                )

                if (parsedData != null) {
                    repository.sendNotification(parsedData)
                    ServiceStatusManager.updateStatus("📤 Enviando a la API...")
                } else {
                    ServiceStatusManager.updateStatus("⚠️ No se pudo procesar la notificación.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
                ServiceStatusManager.updateStatus("🔥 Error: ${e.message}")
            }
        }
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
