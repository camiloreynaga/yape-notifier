package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.yapenotifier.android.data.repository.AuthStatus
import com.yapenotifier.android.databinding.ActivityCapturedNotificationsBinding
import com.yapenotifier.android.ui.adapter.CapturedNotificationsAdapter
import com.yapenotifier.android.ui.viewmodel.CapturedNotificationsViewModel
import com.yapenotifier.android.ui.viewmodel.RetryResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CapturedNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapturedNotificationsBinding
    private val viewModel: CapturedNotificationsViewModel by viewModels()
    private val adapter = CapturedNotificationsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel inyectado automáticamente por Hilt

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Refrescar estado de autenticación al iniciar
        viewModel.refreshAuthStatus()
    }

    override fun onResume() {
        super.onResume()
        // Refrescar estado de autenticación al volver a la actividad
        viewModel.refreshAuthStatus()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnRetryFailed.setOnClickListener {
            viewModel.retryFailedNotifications()
        }
    }

    private fun observeViewModel() {
        viewModel.allNotifications.observe(this) {
            adapter.submitList(it)
        }

        // Observar estado de autenticación
        viewModel.authStatus.observe(this) { authStatus ->
            updateRetryButtonState(authStatus)
        }

        // Observar resultado del reintento
        viewModel.retryResult.observe(this) { retryResult ->
            when (retryResult) {
                is RetryResult.Started -> {
                    Toast.makeText(this, "Reintentando envío de notificaciones fallidas...", Toast.LENGTH_SHORT).show()
                }
                is RetryResult.NoAuth -> {
                    showLoginRequiredDialog()
                }
                is RetryResult.NoDevice -> {
                    Toast.makeText(this, "Dispositivo no registrado. Por favor, vincula tu dispositivo primero.", Toast.LENGTH_LONG).show()
                }
                null -> {
                    // No hacer nada
                }
            }
        }
    }

    private fun updateRetryButtonState(authStatus: AuthStatus) {
        when (authStatus) {
            AuthStatus.AUTHENTICATED -> {
                binding.btnRetryFailed.isEnabled = true
                binding.btnRetryFailed.text = "Reintentar Envío"
            }
            AuthStatus.NOT_AUTHENTICATED -> {
                binding.btnRetryFailed.isEnabled = false
                binding.btnRetryFailed.text = "Inicia Sesión para Reintentar"
            }
            AuthStatus.NO_DEVICE -> {
                binding.btnRetryFailed.isEnabled = false
                binding.btnRetryFailed.text = "Dispositivo No Registrado"
            }
            AuthStatus.TOKEN_EXPIRED -> {
                binding.btnRetryFailed.isEnabled = false
                binding.btnRetryFailed.text = "Sesión Expirada. Inicia Sesión"
            }
        }
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Sesión Requerida")
            .setMessage("No tienes una sesión activa. Para reintentar el envío de notificaciones, necesitas iniciar sesión primero.\n\n¿Deseas iniciar sesión ahora?")
            .setPositiveButton("Iniciar Sesión") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .setCancelable(true)
            .show()
    }
}
