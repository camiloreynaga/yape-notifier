package com.yapenotifier.android

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.worker.SyncSettingsWorker
import java.util.concurrent.TimeUnit

class YapeNotifierApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("YapeNotifierApplication", "Application created. Scheduling background tasks.")
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Schedule a periodic worker to sync settings from the API daily
        val repeatingRequest = PeriodicWorkRequestBuilder<SyncSettingsWorker>(
            1, TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            SyncSettingsWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )

        Log.d("YapeNotifierApplication", "Periodic sync worker for settings has been scheduled.")
    }
}
