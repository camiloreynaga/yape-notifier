package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.NotificationData
import com.yapenotifier.android.util.ServiceStatusManager

class NotificationRepository(private val context: Context) {
    private val apiService = RetrofitClient.createApiService(context)

    suspend fun sendNotification(notificationData: NotificationData): Boolean {
        Log.d(TAG, "Preparing to send notification data: ${Gson().toJson(notificationData)}")
        ServiceStatusManager.updateStatus("📤 Enviando a la API: ${notificationData.sourceApp}")

        return try {
            val response = apiService.createNotification(notificationData)
            
            if (response.isSuccessful) {
                Log.i(TAG, "API call successful. Code: ${response.code()}")
                ServiceStatusManager.updateStatus("✅ API OK: ${response.code()}")
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "API call failed. Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody")
                ServiceStatusManager.updateStatus("🔥 API FAIL: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending notification", e)
            ServiceStatusManager.updateStatus("🔥 NETWORK ERROR: ${e.javaClass.simpleName}")
            false
        }
    }

    companion object {
        private const val TAG = "NotificationRepository"
    }
}
