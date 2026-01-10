package com.yapenotifier.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yape_notifier_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val DEVICE_UUID_KEY = stringPreferencesKey("device_uuid")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val COMMERCE_ID_KEY = stringPreferencesKey("commerce_id")
        private val WIZARD_COMPLETED_KEY = booleanPreferencesKey("wizard_completed")
        private val SELECTED_MONITORED_PACKAGES_KEY = stringSetPreferencesKey("selected_monitored_packages")
        private val HEARTBEAT_INTERVAL_MINUTES_KEY = intPreferencesKey("heartbeat_interval_minutes")
        
        // Default heartbeat interval: 15 minutes
        // Professional practice: Reasonable default that balances battery life and connection status accuracy
        const val DEFAULT_HEARTBEAT_INTERVAL_MINUTES = 15
        const val MIN_HEARTBEAT_INTERVAL_MINUTES = 5  // Minimum: 5 minutes (battery consideration)
        const val MAX_HEARTBEAT_INTERVAL_MINUTES = 60 // Maximum: 60 minutes (connection status accuracy)
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    val deviceUuid: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_UUID_KEY]
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY]
    }

    val commerceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[COMMERCE_ID_KEY]
    }

    val wizardCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WIZARD_COMPLETED_KEY] ?: false
    }

    val selectedMonitoredPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_MONITORED_PACKAGES_KEY] ?: emptySet()
    }

    val heartbeatIntervalMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HEARTBEAT_INTERVAL_MINUTES_KEY] ?: DEFAULT_HEARTBEAT_INTERVAL_MINUTES
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = token
        }
    }

    suspend fun saveDeviceUuid(uuid: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_UUID_KEY] = uuid
        }
    }

    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun saveUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = userName
        }
    }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL_KEY] = email
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun saveCommerceId(commerceId: String) {
        context.dataStore.edit { preferences ->
            preferences[COMMERCE_ID_KEY] = commerceId
        }
    }

    suspend fun setWizardCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIZARD_COMPLETED_KEY] = completed
        }
    }

    suspend fun saveSelectedMonitoredPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_MONITORED_PACKAGES_KEY] = packages
        }
    }

    /**
     * Saves the heartbeat interval in minutes.
     * Validates that the interval is within acceptable range (5-60 minutes).
     * 
     * @param intervalMinutes Heartbeat interval in minutes (must be between 5 and 60)
     * @return true if saved successfully, false if value is out of range
     */
    suspend fun saveHeartbeatInterval(intervalMinutes: Int): Boolean {
        if (intervalMinutes < MIN_HEARTBEAT_INTERVAL_MINUTES || intervalMinutes > MAX_HEARTBEAT_INTERVAL_MINUTES) {
            return false
        }
        context.dataStore.edit { preferences ->
            preferences[HEARTBEAT_INTERVAL_MINUTES_KEY] = intervalMinutes
        }
        return true
    }

    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN_KEY)
        }
    }

    suspend fun clearUserEmail() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_EMAIL_KEY)
        }
    }

    suspend fun clearDeviceId() {
        context.dataStore.edit { preferences ->
            preferences.remove(DEVICE_ID_KEY)
        }
    }

    suspend fun clearCommerceId() {
        context.dataStore.edit { preferences ->
            preferences.remove(COMMERCE_ID_KEY)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
