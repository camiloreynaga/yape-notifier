package com.yapenotifier.android.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.yapenotifier.android.service.PaymentNotificationListenerService

/**
 * Helper class to check and manage Notification Listener Service access.
 * 
 * This class provides utilities to:
 * - Check if the service is enabled
 * - Open the system settings to enable the service
 * - Get the component name for the service
 */
object NotificationAccessChecker {
    private const val TAG = "NotificationAccessChecker"

    /**
     * Checks if the Notification Listener Service is enabled for this app using the modern, recommended approach.
     * 
     * @param context The application context
     * @return true if the service is enabled, false otherwise
     */
    fun isNotificationAccessEnabled(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val myPackageName = context.packageName
        val isEnabled = enabledPackages.contains(myPackageName)

        Log.d(TAG, "Notification access check: enabled=$isEnabled, package=$myPackageName")
        
        return isEnabled
    }

    /**
     * Gets the component name for the Notification Listener Service.
     */
    fun getServiceComponentName(context: Context): ComponentName {
        return ComponentName(context, PaymentNotificationListenerService::class.java)
    }

    /**
     * Opens the system settings screen where the user can enable the Notification Listener Service.
     * 
     * @param context The context to start the activity from
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.i(TAG, "Opened notification listener settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification listener settings", e)
            // Fallback: open general settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open general settings", e2)
            }
        }
    }
}
