package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminSettingsBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.LoginActivity
import com.yapenotifier.android.ui.MonitoredAppsSelectionActivity
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AdminSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSettingsBinding
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dependencias inyectadas automáticamente por Hilt

        setupToolbar()
        setupBottomNavigation()
        setupClickListeners()
        loadUserInfo()
        loadHeartbeatInterval()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = "Configuración"
        binding.toolbarSubtitle.text = "Ajustes y preferencias"
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    val intent = Intent(this, AdminPanelActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_devices -> {
                    val intent = Intent(this, AdminDevicesActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings tab
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnMonitoredApps.setOnClickListener {
            // TODO: Navigate to AdminMonitoredAppsActivity when created
            // val intent = Intent(this, AdminMonitoredAppsActivity::class.java)
            // startActivity(intent)
            // For now, use existing activity
            val intent = Intent(this, MonitoredAppsSelectionActivity::class.java)
            startActivity(intent)
        }

        binding.btnConfigureHeartbeat.setOnClickListener {
            showHeartbeatIntervalDialog()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadHeartbeatInterval() {
        lifecycleScope.launch {
            val interval = preferencesManager.heartbeatIntervalMinutes.first()
            binding.tvHeartbeatInterval.text = 
                getString(R.string.heartbeat_interval_display, interval)
        }
    }

    private fun showHeartbeatIntervalDialog() {
        lifecycleScope.launch {
            val currentInterval = preferencesManager.heartbeatIntervalMinutes.first()
            
            val input = EditText(this@AdminSettingsActivity).apply {
                setHint("Minutos (${PreferencesManager.MIN_HEARTBEAT_INTERVAL_MINUTES}-${PreferencesManager.MAX_HEARTBEAT_INTERVAL_MINUTES})")
                setText(currentInterval.toString())
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            val message = getString(
                R.string.heartbeat_interval_description,
                PreferencesManager.MIN_HEARTBEAT_INTERVAL_MINUTES,
                PreferencesManager.MAX_HEARTBEAT_INTERVAL_MINUTES
            )

            AlertDialog.Builder(this@AdminSettingsActivity)
                .setTitle(getString(R.string.configure_heartbeat_interval))
                .setMessage(message)
                .setView(input)
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    val inputValue = input.text.toString().toIntOrNull()
                    if (inputValue != null) {
                        saveHeartbeatInterval(inputValue)
                    } else {
                        Toast.makeText(
                            this@AdminSettingsActivity,
                            getString(R.string.invalid_heartbeat_interval),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun saveHeartbeatInterval(intervalMinutes: Int) {
        lifecycleScope.launch {
            val success = preferencesManager.saveHeartbeatInterval(intervalMinutes)
            if (success) {
                // Reschedule worker with new interval
                DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this@AdminSettingsActivity)
                
                // Update UI
                binding.tvHeartbeatInterval.text = 
                    getString(R.string.heartbeat_interval_display, intervalMinutes)
                
                Toast.makeText(
                    this@AdminSettingsActivity,
                    getString(R.string.heartbeat_interval_saved, intervalMinutes),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val errorMessage = getString(
                    R.string.invalid_heartbeat_interval_range,
                    PreferencesManager.MIN_HEARTBEAT_INTERVAL_MINUTES,
                    PreferencesManager.MAX_HEARTBEAT_INTERVAL_MINUTES
                )
                Toast.makeText(
                    this@AdminSettingsActivity,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            try {
                val response = apiService.getCurrentUser()
                if (response.isSuccessful) {
                    val user = response.body()
                    user?.let {
                        binding.tvUserEmail.text = it.email
                        binding.tvUserName.text = it.name
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }

            // Load app version
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                binding.tvAppVersion.text = getString(R.string.app_version, packageInfo.versionName)
            } catch (e: Exception) {
                binding.tvAppVersion.text = getString(R.string.app_version, "1.0.0")
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                apiService.logout()
            } catch (e: Exception) {
                // Continue with logout even if API call fails
            }

            // Clear local preferences
            preferencesManager.clearAuthToken()
            preferencesManager.clearUserEmail()
            preferencesManager.clearIsAdminMode()

            // Navigate to ModeSelectionActivity (splash will handle routing)
            val intent = Intent(this@AdminSettingsActivity, ModeSelectionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
