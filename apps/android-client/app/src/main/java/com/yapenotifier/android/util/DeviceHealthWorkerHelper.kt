package com.yapenotifier.android.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.worker.DeviceHealthWorker
import java.util.concurrent.TimeUnit

object DeviceHealthWorkerHelper {
    private const val TAG = "DeviceHealthWorkerHelper"
    private const val REPEAT_INTERVAL_MINUTES = 15L

    /**
     * Schedules a periodic work request for device health reporting.
     * The worker will run every 15 minutes (or the configured interval) when network is available.
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
            workManager.enqueue(periodicWorkRequest)

            Log.i(TAG, "Device health worker scheduled to run every $REPEAT_INTERVAL_MINUTES minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling device health worker", e)
        }
    }

    /**
     * Cancels the periodic device health worker.
     */
    fun cancelDeviceHealthWorker(context: Context) {
        try {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(DeviceHealthWorker.WORK_NAME)
            Log.i(TAG, "Device health worker cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling device health worker", e)
        }
    }
}

