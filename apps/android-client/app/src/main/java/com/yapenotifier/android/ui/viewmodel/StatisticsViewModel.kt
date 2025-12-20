package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.local.db.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatisticsState(
    val lastSentNotification: CapturedNotification? = null,
    val sentTodayCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).capturedNotificationDao()

    private val _statisticsState = MutableStateFlow(StatisticsState())
    val statisticsState: StateFlow<StatisticsState> = _statisticsState.asStateFlow()

    init {
        observeStatistics()
        loadInitialStatistics()
    }

    private fun observeStatistics() {
        viewModelScope.launch {
            // Observe all notifications to update last sent notification and sent today count in real-time
            dao.getAllNotificationsFlow().collect { notifications ->
                val lastSent = notifications.firstOrNull { it.status == "SENT" }
                // Count sent today: notifications with status SENT and timestamp from today
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = calendar.timeInMillis
                val sentToday = notifications.count { 
                    it.status == "SENT" && it.timestamp >= todayStart 
                }
                val currentState = _statisticsState.value
                _statisticsState.value = currentState.copy(
                    lastSentNotification = lastSent,
                    sentTodayCount = sentToday
                )
            }
        }

        viewModelScope.launch {
            // Combine flows for real-time updates of counts
            combine(
                dao.getPendingCountFlow(),
                dao.getFailedCountFlow()
            ) { pendingCount, failedCount ->
                val currentState = _statisticsState.value
                _statisticsState.value = currentState.copy(
                    pendingCount = pendingCount,
                    failedCount = failedCount
                )
            }.collect { }
        }
    }

    private fun loadInitialStatistics() {
        viewModelScope.launch {
            try {
                val lastSent = dao.getLastSentNotification()
                val sentToday = dao.getSentTodayCount()
                val currentState = _statisticsState.value
                _statisticsState.value = currentState.copy(
                    lastSentNotification = lastSent,
                    sentTodayCount = sentToday
                )
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    fun refreshStatistics() {
        viewModelScope.launch {
            try {
                val lastSent = dao.getLastSentNotification()
                val sentToday = dao.getSentTodayCount()
                val currentState = _statisticsState.value
                _statisticsState.value = currentState.copy(
                    lastSentNotification = lastSent,
                    sentTodayCount = sentToday
                )
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

