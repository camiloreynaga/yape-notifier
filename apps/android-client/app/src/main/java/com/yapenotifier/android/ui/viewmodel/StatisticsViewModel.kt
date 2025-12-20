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

