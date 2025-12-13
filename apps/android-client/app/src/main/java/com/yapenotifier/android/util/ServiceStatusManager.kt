package com.yapenotifier.android.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A singleton object to hold and broadcast the live status of background services.
 * This allows the UI (like MainActivity) to observe what the service is doing in real-time.
 */
object ServiceStatusManager {

    private val _statusHistory = MutableStateFlow<List<String>>(emptyList())
    val statusHistory = _statusHistory.asStateFlow()

    fun updateStatus(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newStatus = "$timestamp - $message"
        
        // Add the new status to the top of the list
        val updatedHistory = _statusHistory.value.toMutableList().apply {
            add(0, newStatus)
            // Keep a maximum of 20 log entries
            if (size > 20) {
                removeLast()
            }
        }
        _statusHistory.value = updatedHistory
    }
}