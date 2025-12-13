package com.yapenotifier.android.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yapenotifier.android.data.api.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val apiService = RetrofitClient.createApiService(context)

    companion object {
        private val MONITORED_PACKAGES_KEY = stringSetPreferencesKey("monitored_packages")
        private const val TAG = "SettingsRepository"
    }

    val monitoredPackagesFlow: Flow<Set<String>> = context.dataStore.data
        .map {
            it[MONITORED_PACKAGES_KEY] ?: setOf()
        }
    
    suspend fun refreshMonitoredPackages() {
        try {
            Log.d(TAG, "Fetching monitored packages from API...")
            val response = apiService.getMonitoredPackages() // This endpoint needs to be created in ApiService
            if (response.isSuccessful) {
                val packages = response.body()?.packages?.toSet() ?: emptySet()
                if (packages.isNotEmpty()) {
                    context.dataStore.edit {
                        it[MONITORED_PACKAGES_KEY] = packages
                    }
                    Log.i(TAG, "Successfully updated monitored packages: $packages")
                }
            } else {
                Log.e(TAG, "Failed to fetch packages: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing monitored packages", e)
        }
    }
}