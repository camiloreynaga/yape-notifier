package com.yapenotifier.android.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.yapenotifier.android.data.parser.NotificationParser
import com.yapenotifier.android.data.repository.NotificationRepository
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
        "com.interbank.mobilebanking", // Interbank
        "com.bcp.bancadigital", // BCP
        "com.bbva.bbvacontinental", // BBVA
        "com.scotiabank.mobile", // Scotiabank
        "com.yape.android", // Yape
        "com.plin.android" // Plin
    )

    override fun onCreate() {
        super.onCreate()
        repository = NotificationRepository(applicationContext)
        Log.d(TAG, "PaymentNotificationListenerService created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName ?: return
        
        // Only process notifications from payment apps
        if (!paymentAppPackages.contains(packageName)) {
            return
        }

        serviceScope.launch {
            try {
                val notification = sbn.notification
                val title = notification.extras?.getCharSequence("android.title")?.toString() ?: return@launch
                val text = notification.extras?.getCharSequence("android.text")?.toString() ?: ""
                val bigText = notification.extras?.getCharSequence("android.bigText")?.toString()
                
                val fullText = buildString {
                    append(title)
                    if (text.isNotEmpty()) {
                        appendLine()
                        append(text)
                    }
                    if (bigText?.isNotEmpty() == true) {
                        appendLine()
                        append(bigText)
                    }
                }.trim()

                Log.d(TAG, "Payment notification received from $packageName: $fullText")

                // Parse notification
                val parsedData = parser.parseNotification(
                    packageName = packageName,
                    title = title,
                    body = fullText
                )

                if (parsedData != null) {
                    // Send to API
                    repository.sendNotification(parsedData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        // Not needed for our use case
    }

    companion object {
        private const val TAG = "PaymentNotificationService"
    }
}
