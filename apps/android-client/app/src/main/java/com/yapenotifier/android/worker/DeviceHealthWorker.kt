package com.yapenotifier.android.worker

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.DeviceHealthData
import com.yapenotifier.android.data.repository.DeviceHealthRepository
import com.yapenotifier.android.util.FileLogger
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.flow.first

class DeviceHealthWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val preferencesManager = PreferencesManager(appContext)
    private val repository = DeviceHealthRepository(appContext)
    private val database = AppDatabase.getDatabase(appContext)

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting device health worker...")

        // Get deviceId
        val deviceId = preferencesManager.deviceId.first()
        if (deviceId.isNullOrBlank()) {
            Log.e(TAG, "DeviceId is not set. Cannot send device health data.")
            return Result.failure()
        }

        // Collect device health information
        val healthData = collectDeviceHealthData()

        Log.d(TAG, "Sending device health data: batteryLevel=${healthData.batteryLevel}, " +
                "batteryOptimizationDisabled=${healthData.batteryOptimizationDisabled}, " +
                "notificationPermissionEnabled=${healthData.notificationPermissionEnabled}, " +
                "notificationServiceConnected=${healthData.notificationServiceConnected}, " +
                "pendingNotificationsCount=${healthData.pendingNotificationsCount}")

        // Log health data to file for debugging
        FileLogger.logHealth(
            serviceConnected = healthData.notificationServiceConnected ?: false,
            pendingCount = healthData.pendingNotificationsCount,
            batteryLevel = healthData.batteryLevel
        )

        // Send to backend
        val success = repository.sendDeviceHealth(deviceId, healthData)

        if (success) {
            Log.i(TAG, "Device health data sent successfully")

            // After successful health report, try to send pending logs
            sendPendingLogs()

            return Result.success()
        } else {
            Log.w(TAG, "Failed to send device health data. Will retry later.")
            return Result.retry()
        }
    }

    /**
     * Send pending critical logs to the server.
     * Called after successful heartbeat to ensure connectivity.
     */
    private suspend fun sendPendingLogs() {
        try {
            val pendingCount = FileLogger.getPendingLogsCount()
            if (pendingCount == 0) {
                return
            }

            val deviceUuid = preferencesManager.deviceUuid.first()
            if (deviceUuid.isNullOrBlank()) {
                Log.w(TAG, "Device UUID not set, cannot send logs")
                return
            }

            val pendingLogs = FileLogger.getPendingLogsForUpload()
            if (pendingLogs.isEmpty()) {
                return
            }

            Log.d(TAG, "Sending ${pendingLogs.size} critical logs to server")

            val logsData = pendingLogs.map { entry ->
                mapOf(
                    "category" to entry.category,
                    "level" to entry.level,
                    "message" to entry.message,
                    "details" to (entry.details ?: ""),
                    "timestamp" to entry.timestamp
                )
            }

            val requestBody = mapOf(
                "device_uuid" to deviceUuid,
                "logs" to logsData
            )

            val apiService = RetrofitClient.createApiService(applicationContext)
            val response = apiService.sendDeviceLogs(requestBody)

            if (response.isSuccessful) {
                Log.i(TAG, "Successfully sent ${pendingLogs.size} logs to server")
                FileLogger.markLogsUploaded()
            } else {
                Log.e(TAG, "Failed to send logs: ${response.code()}")
                // Don't clear logs on failure - they'll be retried next time
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending logs to server", e)
        }
    }

    private suspend fun collectDeviceHealthData(): DeviceHealthData {
        // Get battery level
        val batteryLevel = getBatteryLevel()

        // Check battery optimization status
        val batteryOptimizationDisabled = isBatteryOptimizationDisabled()

        // Check notification permission
        val notificationPermissionEnabled = NotificationAccessChecker.isNotificationAccessEnabled(applicationContext)

        // NEW: Get real service connection state
        val notificationServiceConnected = ServiceStatusManager.isServiceConnected()

        // NEW: Get count of pending notifications
        val pendingNotificationsCount = try {
            database.capturedNotificationDao().getPendingCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending count", e)
            null
        }

        // NEW: Get last notification captured timestamp
        val lastNotificationCapturedAt = ServiceStatusManager.getLastNotificationCapturedAt()

        return DeviceHealthData(
            batteryLevel = batteryLevel,
            batteryOptimizationDisabled = batteryOptimizationDisabled,
            notificationPermissionEnabled = notificationPermissionEnabled,
            notificationServiceConnected = notificationServiceConnected,
            pendingNotificationsCount = pendingNotificationsCount,
            lastNotificationCapturedAt = lastNotificationCapturedAt
        )
    }

    private fun getBatteryLevel(): Int? {
        return try {
            val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            null
        }
    }

    private fun isBatteryOptimizationDisabled(): Boolean? {
        return try {
            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(applicationContext.packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking battery optimization status", e)
            null
        }
    }

    companion object {
        const val TAG = "DeviceHealthWorker"
        const val WORK_NAME = "device_health_worker"
    }
}

