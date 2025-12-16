package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.worker.SendNotificationWorker
import kotlinx.coroutines.launch

class CapturedNotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val capturedNotificationDao = AppDatabase.getDatabase(application).capturedNotificationDao()
    private val workManager = WorkManager.getInstance(application)

    val allNotifications = capturedNotificationDao.getAllNotificationsFlow().asLiveData()

    fun retryFailedNotifications() {
        viewModelScope.launch {
            capturedNotificationDao.resetFailedNotifications()
            scheduleSendNotificationWorker()
        }
    }

    private fun scheduleSendNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendWorkRequest = OneTimeWorkRequestBuilder<SendNotificationWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(sendWorkRequest)
    }
}
