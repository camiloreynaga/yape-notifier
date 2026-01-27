package com.yapenotifier.android.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Utility class to request reconnection of the Notification Listener Service.
 *
 * Uses ONLY requestRebind() which is the safe API that does NOT affect
 * the notification access permission.
 *
 * IMPORTANT: Component toggling (disable/enable) has been REMOVED because
 * repeated toggles cause Android to revoke the notification listener permission.
 * This was confirmed in production logs where 49+ toggles led to permission loss.
 */
object ServiceRebinder {
    private const val TAG = "ServiceRebinder"

    /**
     * Attempts to rebind the NotificationListenerService using requestRebind().
     * This is safe and will not revoke the notification access permission.
     *
     * Note: requestRebind() was designed for recovery after requestUnbind(),
     * not after process death. It may not work if the process was killed by the system.
     * The foreground service + START_STICKY are the primary defenses against process death.
     *
     * @param context The application context
     * @return true if the rebind request was sent successfully
     */
    fun rebindNotificationListener(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "requestRebind requires API 24+")
            return false
        }

        return try {
            val componentName = NotificationAccessChecker.getServiceComponentName(context)
            NotificationListenerService.requestRebind(componentName)
            Log.i(TAG, "requestRebind() called successfully")
            FileLogger.log("SERVICE", "Rebind requested", "info")
            true
        } catch (e: Exception) {
            Log.e(TAG, "requestRebind failed", e)
            FileLogger.log("SERVICE", "Rebind failed: ${e.message}", "error")
            false
        }
    }

    /**
     * Checks if the service component is currently enabled.
     */
    fun isServiceComponentEnabled(context: Context): Boolean {
        val componentName = NotificationAccessChecker.getServiceComponentName(context)
        val state = context.packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
               state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
    }

    /**
     * Calculate backoff delay for retry attempts.
     * Uses exponential backoff with jitter.
     *
     * @param attempt Current attempt number (1-based)
     * @param baseDelayMs Base delay in milliseconds
     * @param maxDelayMs Maximum delay cap
     * @return Delay in milliseconds
     */
    fun calculateBackoffDelay(
        attempt: Int,
        baseDelayMs: Long = 5000,
        maxDelayMs: Long = 60000
    ): Long {
        // Exponential backoff: base * 2^(attempt-1)
        val exponentialDelay = baseDelayMs * (1L shl (attempt - 1).coerceAtMost(6))

        // Add jitter (10% random variation)
        val jitter = (exponentialDelay * 0.1 * Math.random()).toLong()

        return (exponentialDelay + jitter).coerceAtMost(maxDelayMs)
    }
}
