package com.yapenotifier.android.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ServiceStatusManager {

    enum class ListenerRecoveryMode {
        NORMAL,
        RECONNECTING,
        MANUAL_ACTION_REQUIRED,
    }

    private const val PREFS_NAME = "service_status_prefs"

    private const val KEY_CONNECTED = "connected"
    private const val KEY_LAST_ACTIVITY = "last_activity"
    private const val KEY_LAST_CONNECTED = "last_connected"
    private const val KEY_LAST_CAPTURED = "last_captured"
    private const val KEY_REBIND_ATTEMPTS = "rebind_attempts"
    private const val KEY_PROCESS_START_AT = "process_start_at"
    private const val KEY_RECOVERY_MODE = "listener_recovery_mode"

    // Ventana máxima sin actividad para seguir considerando vivo el listener.
    // Si no hay ningún callback (onNotificationPosted/onListenerConnected) en este
    // período, isServiceConnected() retorna false y el watchdog dispara rebind.
    private const val SERVICE_IDLE_TIMEOUT_MS = 20 * 60_000L // 20 minutos

    @Volatile private var initialized = false
    @Volatile private var appContext: Context? = null

    private fun prefs(): SharedPreferences {
        val ctx = appContext ?: throw IllegalStateException("ServiceStatusManager.init(context) not called")
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _statusHistory = MutableStateFlow<List<String>>(emptyList())
    val statusHistory = _statusHistory.asStateFlow()

    @Volatile private var _isServiceConnected: Boolean = false
    @Volatile private var _lastNotificationCapturedAt: Long? = null
    @Volatile private var _lastServiceActivityAt: Long? = null
    @Volatile private var _lastListenerConnectedAt: Long? = null
    @Volatile private var _rebindAttempts: Int = 0
    @Volatile private var _processStartAt: Long = 0L

    // True only when onListenerDisconnected() or onDestroy() explicitly fired.
    // Used by watchdog to distinguish "real disconnect" from "idle timeout".
    @Volatile private var _hasRealDisconnect: Boolean = false

    @Volatile private var _recoveryMode: ListenerRecoveryMode = ListenerRecoveryMode.NORMAL
    private val _recoveryModeFlow = MutableStateFlow(ListenerRecoveryMode.NORMAL)
    val recoveryModeFlow: StateFlow<ListenerRecoveryMode> = _recoveryModeFlow.asStateFlow()

    /**
     * IMPORTANT: call this early (Application.onCreate) and also safe to call from Worker/Service.
     * It hydrates persisted state AND records the current process start timestamp.
     *
     * Idempotent within a process: if already initialized, only ensures appContext is set.
     * This prevents Workers calling init() from resetting _processStartAt and invalidating
     * activity timestamps that prove the listener is alive.
     */
    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx

        if (initialized) return // Already initialized in this process — don't reset timestamps

        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Mark this process start — only on FIRST init per process
        _processStartAt = System.currentTimeMillis()
        p.edit().putLong(KEY_PROCESS_START_AT, _processStartAt).apply()

        // Hydrate persisted values
        _isServiceConnected = p.getBoolean(KEY_CONNECTED, false)
        _lastServiceActivityAt = p.getLongOrNull(KEY_LAST_ACTIVITY)
        _lastListenerConnectedAt = p.getLongOrNull(KEY_LAST_CONNECTED)
        _lastNotificationCapturedAt = p.getLongOrNull(KEY_LAST_CAPTURED)
        _rebindAttempts = p.getInt(KEY_REBIND_ATTEMPTS, 0)
        _recoveryMode = try {
            p.getString(KEY_RECOVERY_MODE, null)?.let { ListenerRecoveryMode.valueOf(it) } ?: ListenerRecoveryMode.NORMAL
        } catch (_: IllegalArgumentException) {
            ListenerRecoveryMode.NORMAL
        }
        _recoveryModeFlow.value = _recoveryMode

        initialized = true
    }

    /**
     * Recency-based connected state.
     *
     * The listener is considered alive if there is RECENT activity in THIS process,
     * regardless of whether onListenerConnected() ever fired. This handles Xiaomi/MIUI
     * where the system keeps the binding alive without invoking lifecycle callbacks.
     *
     * Decision order:
     * 1. If not initialized or no timestamps at all → fall back to raw persisted flag.
     * 2. Recent onNotificationPosted() activity in this process → alive.
     * 3. Recent onListenerConnected() in this process → alive.
     * 4. None of the above → considered disconnected (stale flag from previous process).
     */
    fun isServiceConnected(): Boolean {
        // Pre-init: return the raw flag (only happens very early in lifecycle)
        if (!initialized) return _isServiceConnected

        val now = System.currentTimeMillis()
        val lastActivity = _lastServiceActivityAt
        val lastConn = _lastListenerConnectedAt

        // No callbacks ever recorded → use the persisted flag as best guess
        if (lastActivity == null && lastConn == null) {
            return _isServiceConnected
        }

        // Primary: recent listener activity in THIS process → alive
        if (lastActivity != null &&
            lastActivity >= _processStartAt &&
            now - lastActivity <= SERVICE_IDLE_TIMEOUT_MS
        ) {
            return true
        }

        // Fallback: recent onListenerConnected() in THIS process → alive
        if (lastConn != null &&
            lastConn >= _processStartAt &&
            now - lastConn <= SERVICE_IDLE_TIMEOUT_MS
        ) {
            return true
        }

        // No recent evidence → disconnected
        return false
    }

    fun getLastNotificationCapturedAt(): Long? = _lastNotificationCapturedAt
    fun getLastServiceActivityAt(): Long? = _lastServiceActivityAt
    fun getLastListenerConnectedAt(): Long? = _lastListenerConnectedAt
    fun getRebindAttempts(): Int = _rebindAttempts
    fun hasRealDisconnect(): Boolean = _hasRealDisconnect

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected = connected
        if (connected) {
            _hasRealDisconnect = false
        } else {
            _hasRealDisconnect = true
        }
        if (!initialized) return

        val now = System.currentTimeMillis()
        val editor = prefs().edit().putBoolean(KEY_CONNECTED, connected)

        if (connected) {
            _lastListenerConnectedAt = now
            _lastServiceActivityAt = now
            _rebindAttempts = 0

            editor.putLong(KEY_LAST_CONNECTED, now)
            editor.putLong(KEY_LAST_ACTIVITY, now)
            editor.putInt(KEY_REBIND_ATTEMPTS, 0)
        }

        editor.apply()
    }

    fun serviceActivityDetected() {
        // If onNotificationPosted() is running, the listener IS connected — even if
        // onListenerConnected() never re-fired in this process (OEM edge case).
        val wasConnected = _isServiceConnected
        _isServiceConnected = true
        _hasRealDisconnect = false // Activity proves listener is alive — clear any disconnect flag
        _lastServiceActivityAt = System.currentTimeMillis()
        if (!initialized) return
        prefs().edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_LAST_ACTIVITY, _lastServiceActivityAt!!)
            .apply()
        if (!wasConnected) {
            FileLogger.log("STATUS", "activityDetected -> set connected=true (persisted)")
        }
    }

    fun notificationCaptured() {
        val now = System.currentTimeMillis()
        _isServiceConnected = true
        _lastNotificationCapturedAt = now
        _lastServiceActivityAt = now
        if (!initialized) return
        prefs().edit()
            .putBoolean(KEY_CONNECTED, true)
            .putLong(KEY_LAST_CAPTURED, now)
            .putLong(KEY_LAST_ACTIVITY, now)
            .apply()
    }

    fun incrementRebindAttempt(): Int {
        _rebindAttempts += 1
        if (initialized) prefs().edit().putInt(KEY_REBIND_ATTEMPTS, _rebindAttempts).apply()
        return _rebindAttempts
    }

    fun resetRebindAttempts() {
        _rebindAttempts = 0
        if (initialized) prefs().edit().putInt(KEY_REBIND_ATTEMPTS, 0).apply()
    }

    fun getListenerRecoveryMode(): ListenerRecoveryMode = _recoveryMode

    fun setListenerRecoveryMode(mode: ListenerRecoveryMode) {
        if (_recoveryMode == mode) return  // no-op si no cambia
        _recoveryMode = mode
        _recoveryModeFlow.value = mode
        if (!initialized) return
        prefs().edit().putString(KEY_RECOVERY_MODE, mode.name).apply()
        FileLogger.log("SERVICE", "ListenerRecoveryMode=$mode", "info")
    }

    fun updateStatus(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newStatus = "$timestamp - $message"

        val updatedHistory = _statusHistory.value.toMutableList().apply {
            add(0, newStatus)
            if (size > 20) removeLast()
        }
        _statusHistory.value = updatedHistory
    }

    private fun SharedPreferences.getLongOrNull(key: String): Long? {
        return if (contains(key)) getLong(key, 0L) else null
    }
}
