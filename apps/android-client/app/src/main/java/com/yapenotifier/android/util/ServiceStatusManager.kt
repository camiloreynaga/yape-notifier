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

    // Track the last time a notification was captured (payment match)
    @Volatile
    private var _lastNotificationCapturedAt: Long? = null

    // Track the last time the service showed any sign of life
    // Updated on: service connect, any notification received (even non-payment), heartbeat
    @Volatile
    private var _lastServiceActivityAt: Long? = null

    // Track the last time onListenerConnected() was called
    @Volatile
    private var _lastListenerConnectedAt: Long? = null

    // Track consecutive rebind attempts for exponential backoff
    @Volatile
    private var _rebindAttempts: Int = 0

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
     * Returns the timestamp of the last service activity, or null if none.
     * This is more reliable than lastNotificationCapturedAt because it updates
     * whenever the service processes ANY notification, not just payment matches.
     */
    fun getLastServiceActivityAt(): Long? = _lastServiceActivityAt

    /**
     * Returns the timestamp of the last onListenerConnected() call, or null if never.
     */
    fun getLastListenerConnectedAt(): Long? = _lastListenerConnectedAt

    /**
     * Called by PaymentNotificationListenerService when it connects/disconnects.
     */
    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected = connected
        if (connected) {
            _lastListenerConnectedAt = System.currentTimeMillis()
            _lastServiceActivityAt = System.currentTimeMillis()
        }
    }

    /**
     * Called when the service processes any notification (even non-payment).
     * This proves the service is alive and listening.
     */
    fun serviceActivityDetected() {
        _lastServiceActivityAt = System.currentTimeMillis()
    }

    /**
     * Called when a notification is successfully captured (payment match).
     */
    fun notificationCaptured() {
        _lastNotificationCapturedAt = System.currentTimeMillis()
        _lastServiceActivityAt = System.currentTimeMillis()
    }

    /**
     * Returns the current rebind attempt count.
     */
    fun getRebindAttempts(): Int = _rebindAttempts

    /**
     * Increments and returns the rebind attempt count.
     * Called when a rebind is about to be attempted.
     */
    fun incrementRebindAttempt(): Int {
        return ++_rebindAttempts
    }

    /**
     * Resets the rebind attempt counter to 0.
     * Called when service successfully connects.
     */
    fun resetRebindAttempts() {
        _rebindAttempts = 0
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