package com.yapenotifier.android.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.parser.NotificationParser
import com.yapenotifier.android.data.repository.NotificationRepository

class SendNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val notificationDao = AppDatabase.getDatabase(appContext).capturedNotificationDao()
    private val repository = NotificationRepository(appContext)
    private val parser = NotificationParser(appContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting worker to send pending notifications...")

        val pendingNotifications = notificationDao.getPendingNotifications()
        if (pendingNotifications.isEmpty()) {
            Log.d(TAG, "No pending notifications to send.")
            return Result.success()
        }

        Log.d(TAG, "Found ${pendingNotifications.size} pending notifications.")
        var allSucceeded = true

        for (notification in pendingNotifications) {
            // 1. Parse the raw data from the database into a structured NotificationData object
            val notificationData = parser.parseNotification(
                packageName = notification.packageName,
                title = notification.title,
                body = notification.body
            )

            if (notificationData == null) {
                Log.w(TAG, "Could not parse notification ID: ${notification.id}. Marking as FAILED.")
                notificationDao.updateStatus(notification.id, "FAILED")
                continue // Move to the next notification
            }
            
            // 2. Send the structured data to the repository
            val success = repository.sendNotification(notificationData)
            
            if (success) {
                // 3. Update status to SENT on success
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
