package com.yapenotifier.android.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityMainBinding
import com.yapenotifier.android.ui.viewmodel.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var preferencesManager: PreferencesManager

    private val TEST_CHANNEL_ID = "TEST_CHANNEL_ID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // Check if user is logged in
        lifecycleScope.launch {
            val token = preferencesManager.authToken.first()
            if (token == null) {
                navigateToLogin()
                return@launch
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]

        setupObservers()
        setupClickListeners()
        loadUserInfo()
        checkNotificationPermission()
        createNotificationChannel()
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }

    private fun setupObservers() {
        viewModel.statusMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEnableNotifications.setOnClickListener {
            requestNotificationPermission()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Test Buttons Logic
        binding.btnRequestPermission.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        binding.btnSendTestNotification.setOnClickListener {
            sendTestNotification()
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val email = preferencesManager.userEmail.first()
            binding.tvUserInfo.text = "Usuario: ${email ?: "No disponible"}"
        }
    }

    private fun checkNotificationPermission() {
        val enabled = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )?.contains(packageName) == true

        if (enabled) {
            binding.tvStatus.text = "✅ Servicio de notificaciones activado\nLas notificaciones de pago se están monitoreando"
            binding.btnEnableNotifications.text = "Configurar Notificaciones"
        } else {
            binding.tvStatus.text = "❌ Servicio de notificaciones desactivado\nActiva el servicio para monitorear pagos"
            binding.btnEnableNotifications.text = "Activar Notificaciones"
        }
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Habilita 'Yape Notifier' en la lista de servicios", Toast.LENGTH_LONG).show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                // Call logout API if needed
                viewModel.logout()
                
                // Clear local data
                preferencesManager.clearAll()
                
                // Navigate to login
                navigateToLogin()
            } catch (e: Exception) {
                // Even if API call fails, clear local data
                preferencesManager.clearAll()
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Test Notification Methods ---
    private fun createNotificationChannel() {
        val name = "Canal de Pruebas"
        val descriptionText = "Canal para enviar notificaciones de prueba."
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(TEST_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun sendTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(this, TEST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Make sure this icon exists
            .setContentTitle("¡Recibiste un Yape de S/ 1.00!")
            .setContentText("Juan Pérez te ha enviado dinero.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        notificationManager.notify(123, builder.build())
        Toast.makeText(this, "Notificación de prueba enviada", Toast.LENGTH_SHORT).show()
    }
}
