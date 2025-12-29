package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val apiService: ApiService,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

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
                apiService.logout()
            } catch (e: Exception) {
                // Log error but don't block logout
                Timber.tag("MainViewModel").e(e, "Error calling logout API")
            }

            // Always clear local data regardless of API call success
            preferencesManager.clearAll()
            
            // Clear token cache in RetrofitClient
            com.yapenotifier.android.data.api.RetrofitClient.clearTokenCache()
            
            // Notify the UI that logout is complete
            _logoutComplete.value = true
        }
    }
}
