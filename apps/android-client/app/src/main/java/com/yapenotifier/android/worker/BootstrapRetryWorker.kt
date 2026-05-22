package com.yapenotifier.android.worker

import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceRebinder
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One-shot worker that retries requestRebind() after a delay.
 *
 * Scheduled from BootCompletedReceiver with an initial delay of 60 seconds.
 * This handles OEMs that dispatch BOOT_COMPLETED before the system is ready
 * to bind NotificationListenerServices.
 *
 * Also serves as a fallback if the immediate bootstrap in Application.onCreate()
 * didn't result in the service connecting.
 */
class BootstrapRetryWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Safety net: ensure ServiceStatusManager is hydrated in this process
        ServiceStatusManager.init(applicationContext)

        Log.d(TAG, "BootstrapRetryWorker running...")

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Log.w(TAG, "requestRebind requires API 24+")
                return@withContext Result.success()
            }

            val hasPermission = NotificationAccessChecker.isNotificationAccessEnabled(applicationContext)
            val isConnected = ServiceStatusManager.isServiceConnected()
            val isComponentEnabled = ServiceRebinder.isServiceComponentEnabled(applicationContext)

            if (!hasPermission) {
                Log.w(TAG, "Post-boot retry: permission not granted, skipping")
                FileLogger.log(
                    "BOOTSTRAP_RETRY",
                    "Skipped - permissionEnabled=false, componentEnabled=$isComponentEnabled",
                    "warning"
                )
                return@withContext Result.success()
            }

            if (isConnected) {
                Log.d(TAG, "Post-boot retry: service already connected, no action needed")
                FileLogger.log(
                    "BOOTSTRAP_RETRY",
                    "Service already connected - no retry needed",
                    "info"
                )
                return@withContext Result.success()
            }

            // Service not connected after boot + initial bootstrap → retry
            val componentName = NotificationAccessChecker.getServiceComponentName(applicationContext)
            NotificationListenerService.requestRebind(componentName)

            Log.i(TAG, "Post-boot retry: requestRebind() called")
            FileLogger.log(
                "BOOTSTRAP_RETRY",
                "requestRebind() called - permissionEnabled=true, " +
                    "componentEnabled=$isComponentEnabled, serviceConnected=false",
                "info"
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in BootstrapRetryWorker", e)
            FileLogger.logError("BootstrapRetryWorker", e)
            Result.success() // Don't retry on error — watchdog will handle it
        }
    }

    companion object {
        const val TAG = "BootstrapRetryWorker"
        const val WORK_NAME = "bootstrap_retry_worker"
    }
}
