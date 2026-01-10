package com.yapenotifier.android.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivitySplashBinding
import com.yapenotifier.android.ui.admin.AdminLoginActivity
import com.yapenotifier.android.ui.admin.AdminPanelActivity
import com.yapenotifier.android.ui.admin.ModeSelectionActivity
import com.yapenotifier.android.ui.viewmodel.SplashDestination
import com.yapenotifier.android.ui.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Splash screen with session persistence.
 *
 * Professional Architecture Approach:
 * - LAUNCHER activity that determines initial navigation
 * - Checks stored auth token and user mode
 * - Navigates directly to appropriate screen
 * - Prevents unnecessary re-authentication
 * - Provides smooth app restart experience
 *
 * Benefits:
 * - Session persists across app restarts
 * - No need to re-login every time
 * - Background services can continue running
 * - Better UX for frequent app users
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupVersionInfo()
        setupObservers()

        // Small delay for branding visibility (minimum 500ms)
        lifecycleScope.launch {
            delay(500)
            viewModel.determineNavigation()
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = "v$versionName"
        } catch (_: PackageManager.NameNotFoundException) {
            binding.tvVersion.text = "v1.0.0"
        }
    }

    private fun setupObservers() {
        viewModel.navigationDestination.observe(this) { destination ->
            Timber.d("SplashActivity: Navigating to $destination")
            navigateToDestination(destination)
        }
    }

    private fun navigateToDestination(destination: SplashDestination) {
        val intent = when (destination) {
            is SplashDestination.ModeSelection -> {
                Timber.d("SplashActivity: → ModeSelectionActivity (No session)")
                Intent(this, ModeSelectionActivity::class.java)
            }
            is SplashDestination.AdminPanel -> {
                Timber.d("SplashActivity: → AdminPanelActivity (Admin session)")
                Intent(this, AdminPanelActivity::class.java)
            }
            is SplashDestination.MainActivity -> {
                Timber.d("SplashActivity: → MainActivity (Capturer session)")
                Intent(this, MainActivity::class.java)
            }
            is SplashDestination.PinLogin -> {
                Timber.d("SplashActivity: → PinLoginActivity (Capturer no auth)")
                Intent(this, PinLoginActivity::class.java)
            }
            is SplashDestination.AdminLogin -> {
                Timber.d("SplashActivity: → AdminLoginActivity (Admin no auth)")
                Intent(this, AdminLoginActivity::class.java)
            }
            is SplashDestination.LinkDevice -> {
                Timber.d("SplashActivity: → LinkDeviceActivity (Auth but no device)")
                Intent(this, LinkDeviceActivity::class.java)
            }
        }

        // Clear task stack - SplashActivity should not be in back stack
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
