package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.DeviceHealthData

class DeviceHealthRepository(private val context: Context) {
    private val apiService = RetrofitClient.createApiService(context)

    suspend fun sendDeviceHealth(deviceId: String, healthData: DeviceHealthData): Boolean {
        Log.d(TAG, "Sending device health data for deviceId: $deviceId")
        
        return try {
            val response = apiService.updateDeviceHealth(deviceId, healthData)
            
            if (response.isSuccessful) {
                Log.i(TAG, "Device health data sent successfully. Code: ${response.code()}")
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "Failed to send device health. Code: ${response.code()}, Message: ${response.message()}, Body: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while sending device health", e)
            false
        }
    }

    companion object {
        private const val TAG = "DeviceHealthRepository"
    }
}

