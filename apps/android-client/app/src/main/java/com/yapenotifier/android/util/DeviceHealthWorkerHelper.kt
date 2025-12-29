package com.yapenotifier.android.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.worker.DeviceHealthWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit

object DeviceHealthWorkerHelper {
    private const val TAG = "DeviceHealthWorker"

    /**
     * Schedules a periodic work request for device health reporting.
     * The worker will run at the configured interval (default: 15 minutes) when network is available.
     * 
     * Professional approach:
     * - Uses configured heartbeat interval from PreferencesManager
     * - Falls back to default if not configured
     * - Uses enqueueUniquePeriodicWork to prevent duplicate workers
     * - Ensures only one periodic worker is active at a time
     * - Validates interval is within acceptable range
     */
    fun scheduleDeviceHealthWorker(context: Context) {
        try {
            val preferencesManager = PreferencesManager(context)
            val intervalMinutes = runBlocking {
                preferencesManager.heartbeatIntervalMinutes.first()
            }
            
            // Validate interval is within acceptable range
            val validInterval = intervalMinutes.coerceIn(
                PreferencesManager.MIN_HEARTBEAT_INTERVAL_MINUTES,
                PreferencesManager.MAX_HEARTBEAT_INTERVAL_MINUTES
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DeviceHealthWorker>(
                validInterval.toLong(),
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(DeviceHealthWorker.WORK_NAME)
                .build()

            val workManager = WorkManager.getInstance(context)
            // Use enqueueUniquePeriodicWork to prevent duplicate workers
            // REPLACE policy: replace existing worker with new interval if configuration changed
            workManager.enqueueUniquePeriodicWork(
                DeviceHealthWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
            )

            Timber.tag(TAG).i("Device health worker scheduled to run every $validInterval minutes")
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
