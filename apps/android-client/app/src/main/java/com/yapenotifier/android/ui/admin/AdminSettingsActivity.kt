package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminSettingsBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.LoginActivity
import com.yapenotifier.android.ui.MonitoredAppsSelectionActivity
import dagger.hilt.android.AndroidEntryPoint
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
        setupClickListeners()
        loadUserInfo()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuración"
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

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
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

            // Navigate to login
            val intent = Intent(this@AdminSettingsActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
