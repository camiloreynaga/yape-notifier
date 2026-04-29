package com.yapenotifier.android.ui.admin

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminSettingsBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.ui.DebugLogsActivity
import com.yapenotifier.android.ui.LoginActivity
import com.yapenotifier.android.ui.MonitoredAppsSelectionActivity
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceStatusManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AdminSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSettingsBinding
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var apiService: ApiService

    private val capturedNotificationDao by lazy {
        AppDatabase.getDatabase(applicationContext).capturedNotificationDao()
    }

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

    override fun onResume() {
        super.onResume()
        // Refresh operational widgets each time user returns to this screen
        loadServiceStatus()
        loadDiagnostics()
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

        binding.btnViewLogs.setOnClickListener {
            startActivity(Intent(this, DebugLogsActivity::class.java))
        }
    }

    private fun loadServiceStatus() {
        // Make sure ServiceStatusManager is initialized (it's a singleton)
        try {
            ServiceStatusManager.init(applicationContext)
        } catch (e: Exception) {
            Timber.w(e, "ServiceStatusManager init failed")
        }

        val connected = ServiceStatusManager.isServiceConnected()
        if (connected) {
            binding.tvServiceStatusBadge.text = "Activo"
            binding.tvServiceStatusBadge.setBackgroundResource(R.drawable.bg_badge_online)
        } else {
            binding.tvServiceStatusBadge.text = "Inactivo"
            binding.tvServiceStatusBadge.setBackgroundResource(R.drawable.bg_badge_offline)
        }

        val lastActivity = ServiceStatusManager.getLastServiceActivityAt()
        binding.tvLastActivity.text = formatRelativeTime(lastActivity)

        lifecycleScope.launch {
            try {
                val sentToday = capturedNotificationDao.getSentTodayCount()
                val pending = capturedNotificationDao.getPendingCount()
                binding.tvCapturedCount.text = sentToday.toString()
                binding.tvPendingCount.text = pending.toString()
            } catch (e: Exception) {
                Timber.w(e, "Failed to load capture stats")
                binding.tvCapturedCount.text = "—"
                binding.tvPendingCount.text = "—"
            }
        }
    }

    private fun loadDiagnostics() {
        // Notification permission
        val hasNotifPermission = NotificationAccessChecker.isNotificationAccessEnabled(this)
        binding.tvNotificationPermission.text = if (hasNotifPermission) "OK" else "Faltante"
        binding.tvNotificationPermission.setTextColor(
            if (hasNotifPermission) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
        )

        // Battery optimization
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryOptIgnored = pm.isIgnoringBatteryOptimizations(packageName)
        binding.tvBatteryOptimization.text = if (batteryOptIgnored) "Sin restriccion" else "Restringe"
        binding.tvBatteryOptimization.setTextColor(
            if (batteryOptIgnored) 0xFF2E7D32.toInt() else 0xFFEF6C00.toInt()
        )

        // Battery level
        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        binding.tvBatteryLevel.text = if (level >= 0) "$level%" else "—"
    }

    private fun formatRelativeTime(epochMs: Long?): String {
        if (epochMs == null || epochMs == 0L) return "Sin actividad"
        val diff = System.currentTimeMillis() - epochMs
        val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
        return when {
            mins < 1 -> "Hace un momento"
            mins < 60 -> "Hace $mins min"
            mins < 1440 -> "Hace ${mins / 60} h"
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epochMs))
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
