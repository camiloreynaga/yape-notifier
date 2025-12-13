package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.model.NotificationData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.UUID

class NotificationRepository(private val context: Context) {
    private val apiService = RetrofitClient.createApiService(context)
    private val preferencesManager = PreferencesManager(context)

    suspend fun sendNotification(notificationData: NotificationData): Boolean {
        return try {
            // Get device ID (preferred) or UUID (fallback)
            val deviceId = preferencesManager.deviceId.first()
                ?: preferencesManager.deviceUuid.first()
                ?: run {
                    // Generate and save UUID if not exists
                    val uuid = UUID.randomUUID().toString()
                    runBlocking { preferencesManager.saveDeviceUuid(uuid) }
                    uuid
                }

            // Use device ID or UUID
            val notificationWithDevice = notificationData.copy(deviceId = deviceId.toString())

            val response = apiService.createNotification(notificationWithDevice)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Notification sent successfully")
                true
            } else {
                Log.e(TAG, "Failed to send notification: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            false
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
