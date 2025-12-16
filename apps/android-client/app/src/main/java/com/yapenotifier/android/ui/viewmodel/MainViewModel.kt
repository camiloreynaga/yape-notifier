package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesManager = PreferencesManager(application)

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    private val _logoutComplete = MutableLiveData<Boolean>()
    val logoutComplete: LiveData<Boolean> = _logoutComplete

    fun showMessage(message: String) {
        _statusMessage.value = message
        _statusMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.createApiService(getApplication())
                apiService.logout()
            } catch (e: Exception) {
                // Log error but don't block logout
                android.util.Log.e("MainViewModel", "Error calling logout API", e)
            }

            // Always clear local data regardless of API call success
            preferencesManager.clearAll()
            
            // Notify the UI that logout is complete
            _logoutComplete.value = true
        }
    }
}
