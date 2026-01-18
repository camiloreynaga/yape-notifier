package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.network.ApiClient
import com.yapenotifier.android.util.FileLogger
import kotlinx.coroutines.flow.first

/**
 * Repository for sending device logs to the server.
 * Logs are sent along with heartbeat to minimize network calls.
 */
class DeviceLogRepository(private val context: Context) {

    private val preferencesManager = PreferencesManager(context)

    companion object {
        private const val TAG = "DeviceLogRepository"
    }

    /**
     * Send pending logs to the server.
     * Returns true if successful, false otherwise.
     */
    suspend fun sendPendingLogs(): Boolean {
        try {
            val deviceUuid = preferencesManager.deviceUuid.first()
            if (deviceUuid.isNullOrBlank()) {
                Log.w(TAG, "Device UUID not set, cannot send logs")
                return false
            }

            val authToken = preferencesManager.authToken.first()
            if (authToken.isNullOrBlank()) {
                Log.w(TAG, "Auth token not set, cannot send logs")
                return false
            }

            // Get pending logs
            val pendingLogs = FileLogger.getPendingLogsForUpload()
            if (pendingLogs.isEmpty()) {
                Log.d(TAG, "No pending logs to send")
                return true
            }

            Log.d(TAG, "Sending ${pendingLogs.size} logs to server")

            // Prepare request body
            val logsData = pendingLogs.map { entry ->
                mapOf(
                    "category" to entry.category,
                    "level" to entry.level,
                    "message" to entry.message,
                    "details" to entry.details,
                    "timestamp" to entry.timestamp
                )
            }

            val requestBody = mapOf(
                "device_uuid" to deviceUuid,
                "logs" to logsData
            )

            // Send to server
            val response = ApiClient.getService(context).sendDeviceLogs(
                token = "Bearer $authToken",
                body = requestBody
            )

            if (response.isSuccessful) {
                Log.i(TAG, "Successfully sent ${pendingLogs.size} logs to server")
                FileLogger.markLogsUploaded()
                return true
            } else {
                Log.e(TAG, "Failed to send logs: ${response.code()} - ${response.message()}")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending logs to server", e)
            return false
        }
    }
}
