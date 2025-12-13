package com.yapenotifier.android.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityMainBinding
import com.yapenotifier.android.ui.viewmodel.MainViewModel
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var preferencesManager: PreferencesManager

    private val TEST_CHANNEL_ID = "TEST_CHANNEL_ID"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Permiso concedido. Puedes enviar una notificación de prueba.", Toast.LENGTH_SHORT).show()
            sendTestNotification()
        } else {
            Toast.makeText(this, "Permiso denegado. No se puede enviar la notificación.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[MainViewModel::class.java]

        setupUI()
        setupObservers()
        setupClickListeners()
        loadUserInfo()
        updateAllPermissionStatus()
        createNotificationChannel()
    }

    private fun setupUI() {
        binding.tvServiceLog.movementMethod = ScrollingMovementMethod()
    }

    override fun onResume() {
        super.onResume()
        updateAllPermissionStatus()
    }

    private fun setupObservers() {
        viewModel.statusMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        lifecycleScope.launch {
            ServiceStatusManager.statusHistory.collectLatest { history ->
                binding.tvServiceLog.text = history.joinToString(separator = "\n")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnEnableNotifications.setOnClickListener {
            NotificationAccessChecker.openNotificationListenerSettings(this)
        }

        binding.btnBatteryOptimization.setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }

        binding.btnLogout.setOnClickListener {
            // Restore Login functionality
            AlertDialog.Builder(this)
                .setTitle("Restaurar App")
                .setMessage("Esto reactivará la pantalla de Login y restaurará el AndroidManifest. ¿Continuar?")
                .setPositiveButton("Restaurar") { _, _ -> restoreLoginFunctionality() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnSendTestNotification.setOnClickListener {
            requestPostNotificationPermissionAndSend()
        }

        binding.btnViewDatabase.setOnClickListener {
            startActivity(Intent(this, CapturedNotificationsActivity::class.java))
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val email = preferencesManager.userEmail.first()
            binding.tvUserInfo.text = "Usuario: ${email ?: "Modo de Prueba"}"
        }
    }

    private fun updateAllPermissionStatus() {
        checkNotificationPermission()
        checkBatteryOptimizationStatus()
    }

    private fun checkNotificationPermission() {
        if (NotificationAccessChecker.isNotificationAccessEnabled(this)) {
            binding.tvStatus.text = "✅ Permiso de Notificación: Activado"
            binding.btnEnableNotifications.isEnabled = false
        } else {
            binding.tvStatus.text = "❌ Permiso de Notificación: Desactivado"
            binding.btnEnableNotifications.isEnabled = true
        }
    }

    private fun checkBatteryOptimizationStatus() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (isIgnoringOptimizations) {
            binding.tvBatteryStatus.text = "✅ Ahorro de Batería: Desactivado"
            binding.btnBatteryOptimization.isEnabled = false
        } else {
            binding.tvBatteryStatus.text = "❌ Ahorro de Batería: Activado (puede detener el servicio)"
            binding.btnBatteryOptimization.isEnabled = true
        }
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Habilita 'Yape Notifier' en la lista de servicios", Toast.LENGTH_LONG).show()
    }

    private fun requestIgnoreBatteryOptimizations() {
        AlertDialog.Builder(this)
            .setTitle("Acción Requerida: Desactivar Ahorro de Batería")
            .setMessage(
                "Para asegurar que las notificaciones se procesen en tiempo real, es crucial desactivar las optimizaciones de batería para 'Yape Notifier'.\n\n" +
                "1. Presiona 'Ir a Ajustes'.\n" +
                "2. Busca la sección 'Batería' o 'Administración de aplicaciones'.\n" +
                "3. Encuentra 'Yape Notifier' y selecciona 'Sin restricciones' o 'Permitir actividad en segundo plano'."
            )
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "No se pudo abrir la pantalla de Ajustes.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Más Tarde", null)
            .show()
    }

    private fun restoreLoginFunctionality() {
        // This is a developer-only feature to easily revert testing changes.
        // In a real app, this would be removed.
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- Test Notification Methods ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    }

    private fun requestPostNotificationPermissionAndSend() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    sendTestNotification()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            sendTestNotification()
        }
    }

    private fun sendTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val testTitle = "Plin"
        val testBody = "JOHN DOE te ha plineado S/ 5.50"

        val notification = NotificationCompat.Builder(this, TEST_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(testTitle)
            .setContentText(testBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(testBody))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Toast.makeText(this, "Notificación de prueba (PLIN) enviada", Toast.LENGTH_LONG).show()
    }
}
