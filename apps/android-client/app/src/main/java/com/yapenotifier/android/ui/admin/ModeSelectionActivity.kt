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
import com.yapenotifier.android.ui.PinLoginActivity
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
                // Verificar si el usuario tiene token de autenticación (PIN login)
                val authToken = preferencesManager.authToken.first()
                Timber.d("ModeSelection: Verificando token de autenticación")
                
                if (authToken.isNullOrBlank()) {
                    // Sin token → Usuario debe hacer login con PIN
                    Timber.d("ModeSelection: Sin token, navegando a PinLoginActivity")
                    val intent = Intent(this@ModeSelectionActivity, PinLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    return@launch
                }
                
                // Con token → Verificar si dispositivo está vinculado
                val deviceId = preferencesManager.deviceId.first()
                Timber.d("ModeSelection: Token encontrado, verificando deviceId=$deviceId")
                
                if (deviceId != null && deviceId.isNotBlank()) {
                    // Token + DeviceId → Ir a MainActivity
                    Timber.d("ModeSelection: Token y DeviceId encontrados, navegando a MainActivity")
                    val intent = Intent(this@ModeSelectionActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    // Token pero sin DeviceId → Ir a LinkDeviceActivity
                    Timber.d("ModeSelection: Token encontrado pero sin DeviceId, navegando a LinkDeviceActivity")
                    val intent = Intent(this@ModeSelectionActivity, LinkDeviceActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "ModeSelection: Error al verificar estado")
                // En caso de error, ir a PinLoginActivity para que el usuario se autentique
                val intent = Intent(this@ModeSelectionActivity, PinLoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
