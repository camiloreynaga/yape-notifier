package com.yapenotifier.android.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.data.repository.NotificationRepository
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.SourceAppMapper
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SendNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationDao = AppDatabase.getDatabase(appContext).capturedNotificationDao()
    private val repository = NotificationRepository(appContext)
    private val preferencesManager = PreferencesManager(appContext)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting worker to send pending notifications...")

        val pendingNotifications = notificationDao.getPendingNotifications()
        if (pendingNotifications.isEmpty()) {
            Log.d(TAG, "No pending notifications to send.")
            return Result.success()
        }
        
        // Fetch deviceId once for all notifications in this batch
        val deviceId = preferencesManager.deviceUuid.first() ?: ""
        if (deviceId.isEmpty()) {
            Log.e(TAG, "DeviceID is not set. Cannot send notifications.")
            return Result.failure() // Fail fast if no deviceId
        }

        Log.d(TAG, "Found ${pendingNotifications.size} pending notifications for deviceId: $deviceId")
        var allSucceeded = true

        for (notification in pendingNotifications) {
            // Parse payment details to extract amount, currency, and payer name
            val paymentDetails = PaymentNotificationParser.parse(notification.title, notification.body)

            // Map package_name to source_app (required by backend validation)
            var sourceApp = SourceAppMapper.mapPackageToSourceApp(notification.packageName)
            
            // If package mapping failed, try to infer from notification content
            // This handles cases where the package is our own app (com.yapenotifier.android)
            if (sourceApp == null) {
                Log.d(TAG, "Package mapping failed for '${notification.packageName}', attempting to infer from content")
                sourceApp = SourceAppMapper.inferSourceAppFromContent(notification.title, notification.body)
            }
            
            // If still null, we cannot determine source_app - mark as failed
            if (sourceApp == null) {
                Log.w(TAG, "Could not determine source_app for notification ID: ${notification.id}, package: ${notification.packageName}, title: '${notification.title}'. Marking as FAILED.")
                notificationDao.updateStatus(notification.id, "FAILED")
                continue
            }

            // If payment details couldn't be parsed, we still send the notification
            // but with null values for amount, currency, and payer_name
            // The backend can handle these null values
            val amount = paymentDetails?.amount
            val currency = paymentDetails?.currency ?: "PEN"
            val payerName = paymentDetails?.sender

            // Format posted_at timestamp if available
            val postedAt = notification.postedAt?.let {
                dateFormat.format(Date(it))
            }

            // Format received_at (when we captured it)
            val receivedAt = dateFormat.format(Date(notification.timestamp))

            // Build raw_json with all available metadata
            val rawJson = mutableMapOf<String, Any>(
                "package_name" to notification.packageName,
                "title" to notification.title,
                "body" to notification.body,
                "original_timestamp" to notification.timestamp
            ).apply {
                notification.androidUserId?.let { put("android_user_id", it) }
                notification.androidUid?.let { put("android_uid", it) }
                notification.postedAt?.let { put("posted_at", it) }
            }

            val notificationData = NotificationData(
                deviceId = deviceId,
                sourceApp = sourceApp, // Mapped source_app, not package_name
                packageName = notification.packageName,
                androidUserId = notification.androidUserId,
                androidUid = notification.androidUid,
                title = notification.title,
                body = notification.body, // Original body text
                amount = amount,
                currency = currency,
                payerName = payerName,
                postedAt = postedAt,
                receivedAt = receivedAt,
                rawJson = rawJson,
                status = "pending"
            )

            Log.d(TAG, "Sending notification ID: ${notification.id}, sourceApp: $sourceApp, packageName: ${notification.packageName}")

            val success = repository.sendNotification(notificationData)

            if (success) {
                notificationDao.updateStatus(notification.id, "SENT")
                Log.i(TAG, "Successfully sent notification ID: ${notification.id}")
            } else {
                Log.e(TAG, "Failed to send notification ID: ${notification.id}. Will retry later.")
                allSucceeded = false
            }
        }

        return if (allSucceeded) Result.success() else Result.retry()
    }

    companion object {
        const val TAG = "SendNotificationWorker"
    }
}
