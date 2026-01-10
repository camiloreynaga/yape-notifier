package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivityPinLoginBinding
import com.yapenotifier.android.ui.viewmodel.PinLoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class PinLoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPinLoginBinding
    private val viewModel: PinLoginViewModel by viewModels()
    
    @javax.inject.Inject
    lateinit var preferencesManager: com.yapenotifier.android.data.local.PreferencesManager
    
    private var pin = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // Configurar teclado numérico
        binding.btn0.setOnClickListener { addDigit("0") }
        binding.btn1.setOnClickListener { addDigit("1") }
        binding.btn2.setOnClickListener { addDigit("2") }
        binding.btn3.setOnClickListener { addDigit("3") }
        binding.btn4.setOnClickListener { addDigit("4") }
        binding.btn5.setOnClickListener { addDigit("5") }
        binding.btn6.setOnClickListener { addDigit("6") }
        binding.btn7.setOnClickListener { addDigit("7") }
        binding.btn8.setOnClickListener { addDigit("8") }
        binding.btn9.setOnClickListener { addDigit("9") }
        binding.btnBackspace.setOnClickListener { removeDigit() }
        
        // Botón cancelar - limpiar sesión y cerrar
        binding.btnCancel.setOnClickListener {
            showCancelDialog()
        }
    }
    
    private fun showCancelDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Cancelar")
            .setMessage("¿Deseas salir? Se cerrará la sesión actual.")
            .setPositiveButton("Salir") { _, _ ->
                logout()
            }
            .setNegativeButton("Continuar", null)
            .show()
    }
    
    private fun logout() {
        lifecycleScope.launch {
            try {
                // Limpiar datos de sesión
                preferencesManager.clearAuthToken()
                preferencesManager.clearUserEmail()
                preferencesManager.clearDeviceId()
                preferencesManager.clearCommerceId()
                
                // Limpiar token cache
                com.yapenotifier.android.data.api.RetrofitClient.clearTokenCache()
                
                // Cerrar la app o volver al inicio
                finishAffinity()
            } catch (e: Exception) {
                Timber.e(e, "Error al hacer logout")
                finishAffinity()
            }
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is PinLoginViewModel.LoginState.Idle -> {
                        binding.progressBar.visibility = View.GONE
                        setKeypadEnabled(true)
                    }
                    is PinLoginViewModel.LoginState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        setKeypadEnabled(false)
                    }
                    is PinLoginViewModel.LoginState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@PinLoginActivity, 
                            "¡Bienvenido ${state.userName}!", 
                            Toast.LENGTH_SHORT).show()
                        navigateToLinkDevice()
                    }
                    is PinLoginViewModel.LoginState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        setKeypadEnabled(true)
                        Toast.makeText(this@PinLoginActivity, 
                            state.message, 
                            Toast.LENGTH_LONG).show()
                        clearPin()
                    }
                }
            }
        }
    }
    
    private fun addDigit(digit: String) {
        if (pin.length < 6) {
            pin += digit
            updatePinDisplay()
            
            // Auto-submit cuando llega a 4 dígitos
            if (pin.length == 4) {
                submitPin()
            }
        }
    }
    
    private fun removeDigit() {
        if (pin.isNotEmpty()) {
            pin = pin.dropLast(1)
            updatePinDisplay()
        }
    }
    
    private fun updatePinDisplay() {
        binding.pinDot1.isActivated = pin.length >= 1
        binding.pinDot2.isActivated = pin.length >= 2
        binding.pinDot3.isActivated = pin.length >= 3
        binding.pinDot4.isActivated = pin.length >= 4
    }
    
    private fun submitPin() {
        Timber.d("Submitting PIN")
        viewModel.loginWithPin(pin)
    }
    
    private fun clearPin() {
        pin = ""
        updatePinDisplay()
    }
    
    private fun setKeypadEnabled(enabled: Boolean) {
        binding.btn0.isEnabled = enabled
        binding.btn1.isEnabled = enabled
        binding.btn2.isEnabled = enabled
        binding.btn3.isEnabled = enabled
        binding.btn4.isEnabled = enabled
        binding.btn5.isEnabled = enabled
        binding.btn6.isEnabled = enabled
        binding.btn7.isEnabled = enabled
        binding.btn8.isEnabled = enabled
        binding.btn9.isEnabled = enabled
        binding.btnBackspace.isEnabled = enabled
    }
    
    private fun navigateToLinkDevice() {
        val intent = Intent(this, LinkDeviceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

