package com.yapenotifier.android.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.worker.DeviceHealthWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

object DeviceHealthWorkerHelper {
    private const val TAG = "DeviceHealthWorkerHelper"
    private const val REPEAT_INTERVAL_MINUTES = 15L

    /**
     * Schedules a periodic work request for device health reporting.
     * The worker will run every 15 minutes (or the configured interval) when network is available.
     * 
     * Professional approach: Uses enqueueUniquePeriodicWork to prevent duplicate workers
     * and ensures only one periodic worker is active at a time.
     */
    fun scheduleDeviceHealthWorker(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DeviceHealthWorker>(
                REPEAT_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(DeviceHealthWorker.WORK_NAME)
                .build()

            val workManager = WorkManager.getInstance(context)
            // Use enqueueUniquePeriodicWork to prevent duplicate workers
            // KEEP policy: if worker already exists, keep the existing one
            workManager.enqueueUniquePeriodicWork(
                DeviceHealthWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            Timber.tag(TAG).i("Device health worker scheduled to run every $REPEAT_INTERVAL_MINUTES minutes")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error scheduling device health worker")
        }
    }
    
    /**
     * Sends an immediate health check (one-time work) in addition to the periodic worker.
     * This is useful after device linking to immediately update connection status.
     */
    fun sendImmediateHealthCheck(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWorkRequest = androidx.work.OneTimeWorkRequestBuilder<DeviceHealthWorker>()
                .setConstraints(constraints)
                .addTag(DeviceHealthWorker.WORK_NAME)
                .build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueue(oneTimeWorkRequest)

            Timber.tag(TAG).i("Immediate device health check scheduled")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error scheduling immediate health check")
        }
    }

    /**
     * Cancels the periodic device health worker.
     */
    fun cancelDeviceHealthWorker(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(DeviceHealthWorker.WORK_NAME)
            Timber.tag(TAG).i("Device health worker cancelled")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error cancelling device health worker")
        }
    }
}

