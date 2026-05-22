package com.yapenotifier.android.data.repository

import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.UpdateDeviceMonitoredAppsRequest

class MonitoredAppsRepository(private val apiService: ApiService) {

    suspend fun getAvailablePackages() = apiService.getMonitoredPackages()

    suspend fun getDeviceMonitoredApps(deviceId: String) = apiService.getDeviceMonitoredApps(deviceId)

    suspend fun updateDeviceMonitoredApps(deviceId: String, packages: List<String>) = apiService.updateDeviceMonitoredApps(deviceId, UpdateDeviceMonitoredAppsRequest(packages))

}
