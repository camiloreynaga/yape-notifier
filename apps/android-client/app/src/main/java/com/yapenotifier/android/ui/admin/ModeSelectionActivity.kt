package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityModeSelectionBinding
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.LinkDeviceActivity
import com.yapenotifier.android.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Initial screen that allows users to choose between Admin mode or Capturer mode.
 */
@AndroidEntryPoint
class ModeSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModeSelectionBinding
    
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Dependencias inyectadas automáticamente por Hilt
        setupClickListeners()
        setupVersionInfo()
    }

    private fun setupClickListeners() {
        // Admin mode - navigate to admin login
        binding.cardAdmin.setOnClickListener {
            Timber.d("ModeSelection: Usuario seleccionó Modo Admin")
            try {
                val intent = Intent(this, AdminLoginActivity::class.java)
                startActivity(intent)
                Timber.d("ModeSelection: Navegando a AdminLoginActivity")
            } catch (e: Exception) {
                Timber.e(e, "ModeSelection: Error al navegar a AdminLoginActivity")
            }
        }

        // Capturer mode - check if device is already linked
        binding.cardCapturer.setOnClickListener {
            Timber.d("ModeSelection: Usuario seleccionó Modo Capturer")
            checkDeviceStatusAndNavigate()
        }
    }
    
    private fun checkDeviceStatusAndNavigate() {
        lifecycleScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                Timber.d("ModeSelection: Verificando estado del dispositivo - deviceId=$deviceId")
                
                if (deviceId != null && deviceId.isNotBlank()) {
                    // DeviceId local existe, ir directo a MainActivity
                    // LinkDeviceActivity verificará en backend si realmente está vinculado
                    Timber.d("ModeSelection: DeviceId local encontrado, navegando a MainActivity")
                    val intent = Intent(this@ModeSelectionActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    // Device no está vinculado localmente, ir a LinkDeviceActivity
                    // LinkDeviceActivity verificará en backend también
                    Timber.d("ModeSelection: Dispositivo no vinculado localmente, navegando a LinkDeviceActivity")
                    val intent = Intent(this@ModeSelectionActivity, LinkDeviceActivity::class.java)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "ModeSelection: Error al verificar estado del dispositivo")
                // En caso de error, ir a LinkDeviceActivity para que el usuario pueda vincular
                val intent = Intent(this@ModeSelectionActivity, LinkDeviceActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = getString(R.string.version_info, versionName)
        } catch (_: Exception) {
            binding.tvVersion.text = getString(R.string.version_info, "1.0.0")
        }
    }
}
