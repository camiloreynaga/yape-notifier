package com.yapenotifier.android.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log

/**
 * Utility class to force reconnection of the Notification Listener Service.
 * 
 * This is particularly useful for OEMs (like OPPO, Xiaomi, Huawei) that may
 * kill or disconnect the service due to battery optimization or background restrictions.
 * 
 * The rebinding process:
 * 1. Disables the service component
 * 2. Re-enables the service component
 * 3. (API 24+) Requests a rebind using requestRebind()
 */
object ServiceRebinder {
    private const val TAG = "ServiceRebinder"

    /**
     * Forces a reconnection of the Notification Listener Service by toggling
     * the component state and requesting a rebind.
     * 
     * @param context The application context
     * @return true if the operation was successful, false otherwise
     */
    fun rebindNotificationListener(context: Context): Boolean {
        return try {
            val componentName = NotificationAccessChecker.getServiceComponentName(context)
            val packageManager = context.packageManager

            Log.i(TAG, "Starting service rebind process for: ${componentName.flattenToString()}")

            // Step 1: Disable the component
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Component disabled")

            // Wait a brief moment (not ideal but necessary for some OEMs)
            Thread.sleep(500)

            // Step 2: Re-enable the component
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Component re-enabled")

            // Step 3: Request rebind (API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationListenerService.requestRebind(componentName)
                    Log.i(TAG, "requestRebind() called successfully")
                } catch (e: Exception) {
                    Log.w(TAG, "requestRebind() failed (may require user to re-enable in settings)", e)
                    // This is not critical - the component toggle should be enough
                }
            }

            Log.i(TAG, "Service rebind process completed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebind notification listener service", e)
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
}

