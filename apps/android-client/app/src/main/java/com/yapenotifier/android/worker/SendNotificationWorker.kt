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
            val paymentDetails = PaymentNotificationParser.parse(notification.title, notification.body)

            if (paymentDetails == null) {
                Log.w(TAG, "Could not parse notification ID: ${notification.id}. Marking as FAILED.")
                notificationDao.updateStatus(notification.id, "FAILED")
                continue
            }
            
            val notificationData = NotificationData(
                deviceId = deviceId,
                sourceApp = notification.packageName,
                title = notification.title,
                body = notification.body,
                amount = paymentDetails.amount,
                currency = paymentDetails.currency,
                payerName = paymentDetails.sender,
                receivedAt = dateFormat.format(Date()),
                rawJson = mapOf("package_name" to notification.packageName, "title" to notification.title, "body" to notification.body),
                status = "pending"
            )

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
