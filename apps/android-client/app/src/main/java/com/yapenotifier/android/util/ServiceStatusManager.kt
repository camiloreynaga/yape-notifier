package com.yapenotifier.android.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A singleton object to hold and broadcast the live status of background services.
 * This allows the UI (like MainActivity) to observe what the service is doing in real-time.
 * Also tracks whether the NotificationListenerService is actually connected.
 */
object ServiceStatusManager {

    private val _statusHistory = MutableStateFlow<List<String>>(emptyList())
    val statusHistory = _statusHistory.asStateFlow()

    // Track if the NotificationListenerService is actually connected
    @Volatile
    private var _isServiceConnected: Boolean = false

    // Track the last time a notification was captured
    @Volatile
    private var _lastNotificationCapturedAt: Long? = null

    /**
     * Returns true if the NotificationListenerService is connected and listening.
     * This is the REAL state of the service, not just if the permission is enabled.
     */
    fun isServiceConnected(): Boolean = _isServiceConnected

    /**
     * Returns the timestamp of the last captured notification, or null if none.
     */
    fun getLastNotificationCapturedAt(): Long? = _lastNotificationCapturedAt

    /**
     * Called by PaymentNotificationListenerService when it connects/disconnects.
     */
    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected = connected
    }

    /**
     * Called when a notification is successfully captured.
     */
    fun notificationCaptured() {
        _lastNotificationCapturedAt = System.currentTimeMillis()
    }

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