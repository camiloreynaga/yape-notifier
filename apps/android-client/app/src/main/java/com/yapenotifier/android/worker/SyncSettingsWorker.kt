package com.yapenotifier.android.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.data.repository.SettingsRepository

class SyncSettingsWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting worker to sync settings...")
        val repository = SettingsRepository(applicationContext)
        
        try {
            repository.refreshMonitoredPackages()
            Log.d(TAG, "Settings sync finished successfully.")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing settings. Will retry later.", e)
            return Result.retry()
        }
    }

    companion object {
        const val TAG = "SyncSettingsWorker"
    }
}
