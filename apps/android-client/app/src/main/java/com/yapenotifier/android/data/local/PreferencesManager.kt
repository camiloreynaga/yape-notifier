package com.yapenotifier.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yape_notifier_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        private val DEVICE_UUID_KEY = stringPreferencesKey("device_uuid")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN_KEY]
    }

    val deviceUuid: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_UUID_KEY]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_EMAIL_KEY]
    }

    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY]
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

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

