package com.yapenotifier.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.worker.BootstrapRetryWorker
import java.util.concurrent.TimeUnit

/**
 * Receives BOOT_COMPLETED broadcast to ensure the app process starts after device reboot.
 *
 * Two-phase bootstrap:
 * 1. Immediate: Application.onCreate() runs automatically when this broadcast is delivered,
 *    calling bootstrapNotificationListener() → requestRebind().
 * 2. Delayed retry: Schedules a OneTimeWork with 60s delay as fallback, because some OEMs
 *    dispatch BOOT_COMPLETED before the system is ready to bind NotificationListenerServices.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            Log.i(TAG, "Boot completed - scheduling delayed bootstrap retry (${POST_BOOT_DELAY_SEC}s)")
            FileLogger.log("SYSTEM", "Boot completed - scheduling delayed retry", "info")

            // Schedule a delayed retry in case the immediate bootstrap didn't work
            try {
                val retryRequest = OneTimeWorkRequestBuilder<BootstrapRetryWorker>()
                    .setInitialDelay(POST_BOOT_DELAY_SEC, TimeUnit.SECONDS)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    BootstrapRetryWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    retryRequest
                )

                Log.i(TAG, "Post-boot retry worker scheduled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule post-boot retry", e)
                FileLogger.logError("BootCompletedReceiver", e)
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
        private const val POST_BOOT_DELAY_SEC = 60L
    }
}
