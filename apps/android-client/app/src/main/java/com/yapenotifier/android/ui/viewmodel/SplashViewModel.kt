package com.yapenotifier.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.yapenotifier.android.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Navigation destination for splash screen routing
 */
sealed class SplashDestination {
    object ModeSelection : SplashDestination()  // No session - choose mode
    object AdminPanel : SplashDestination()      // Admin session exists
    object MainActivity : SplashDestination()    // Capturer session exists
    object PinLogin : SplashDestination()       // Capturer mode but no auth
    object AdminLogin : SplashDestination()      // Admin mode but no auth
    object LinkDevice : SplashDestination()      // Auth exists but device not linked
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    application: Application,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _navigationDestination = MutableLiveData<SplashDestination>()
    val navigationDestination: LiveData<SplashDestination> = _navigationDestination

    /**
     * Determines where to navigate based on stored session data.
     *
     * Professional Architecture Approach:
     * 1. Check if auth token exists (user has logged in before)
     * 2. Check mode (admin vs capturer)
     * 3. Check additional requirements (device linking for capturer)
     * 4. Navigate to appropriate screen
     */
    fun determineNavigation() {
        viewModelScope.launch {
            try {
                Timber.d("SplashViewModel: Determining navigation destination...")

                // Get stored preferences
                val authToken = preferencesManager.authToken.first()
                val isAdminMode = preferencesManager.isAdminMode.first()
                val deviceId = preferencesManager.deviceId.first()

                Timber.d("SplashViewModel: authToken=${authToken != null}, isAdminMode=$isAdminMode, deviceId=$deviceId")

                // Decision tree
                when {
                    // No auth token → First time or logged out → Show mode selection
                    authToken.isNullOrBlank() -> {
                        Timber.d("SplashViewModel: No auth token → ModeSelection")
                        _navigationDestination.value = SplashDestination.ModeSelection
                    }

                    // Has token + Admin mode → Go to Admin Panel
                    isAdminMode -> {
                        Timber.d("SplashViewModel: Admin mode with token → AdminPanel")
                        _navigationDestination.value = SplashDestination.AdminPanel
                    }

                    // Has token + Capturer mode + Device linked → Go to MainActivity
                    !deviceId.isNullOrBlank() -> {
                        Timber.d("SplashViewModel: Capturer mode with device → MainActivity")
                        _navigationDestination.value = SplashDestination.MainActivity
                    }

                    // Has token + Capturer mode + No device → Link device
                    else -> {
                        Timber.d("SplashViewModel: Capturer mode without device → LinkDevice")
                        _navigationDestination.value = SplashDestination.LinkDevice
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "SplashViewModel: Error determining navigation")
                // On error, safe fallback to mode selection
                _navigationDestination.value = SplashDestination.ModeSelection
            }
        }
    }
}
