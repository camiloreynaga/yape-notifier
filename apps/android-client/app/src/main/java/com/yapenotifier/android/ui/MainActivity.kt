package com.yapenotifier.android.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import com.yapenotifier.android.ui.viewmodel.StatisticsState
import com.yapenotifier.android.ui.viewmodel.StatisticsViewModel
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var statisticsViewModel: StatisticsViewModel
    private lateinit var preferencesManager: PreferencesManager

    private val TEST_CHANNEL_ID = "TEST_CHANNEL_ID"
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
        statisticsViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[StatisticsViewModel::class.java]

        setupUI()
        setupObservers()
        setupClickListeners()
        loadUserInfo()
        updateAllPermissionStatus()
        createNotificationChannel()
    }

    private fun setupUI() {
        binding.tvServiceLog.movementMethod = ScrollingMovementMethod()
        updateCaptureStatus("Verificando...", Color.parseColor("#757575"))
    }


    private fun setupObservers() {
        viewModel.statusMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.logoutComplete.observe(this) { isComplete ->
            if (isComplete) {
                navigateToLogin()
            }
        }

        lifecycleScope.launch {
            ServiceStatusManager.statusHistory.collectLatest { history ->
                binding.tvServiceLog.text = history.joinToString(separator = "\n")
                // Update capture status based on last status
                if (history.isNotEmpty()) {
                    val lastStatus = history.first()
                    updateCaptureStatusFromServiceStatus(lastStatus)
                }
            }
        }

        // Observe statistics
        lifecycleScope.launch {
            statisticsViewModel.statisticsState.collectLatest(::updateStatistics)
        }

        // Refresh statistics when activity resumes
        lifecycleScope.launch {
            statisticsViewModel.refreshStatistics()
        }
    }

    override fun onResume() {
        super.onResume()
        updateAllPermissionStatus()
        // Refresh statistics when returning to activity
        statisticsViewModel.refreshStatistics()
    }

    private fun updateCaptureStatusFromServiceStatus(status: String) {
        when {
            status.contains("✅") || status.contains("OK") || status.contains("exitoso") -> {
                updateCaptureStatus("✅ Capturando OK", Color.parseColor("#4CAF50"))
            }
            status.contains("⚠️") || status.contains("advertencia") || status.contains("pendiente") -> {
                updateCaptureStatus("⚠️ Advertencia", Color.parseColor("#FF9800"))
            }
            status.contains("❌") || status.contains("ERROR") || status.contains("FAIL") || status.contains("error") -> {
                updateCaptureStatus("❌ Error en Captura", Color.parseColor("#F44336"))
            }
            else -> {
                updateCaptureStatus("🔄 En Proceso", Color.parseColor("#2196F3"))
            }
        }
        binding.tvLastServiceStatus.text = status
    }

    private fun updateCaptureStatus(statusText: String, color: Int) {
        binding.tvCaptureStatus.text = statusText
        binding.tvCaptureStatus.setTextColor(color)
        // Update card stroke color dynamically
        binding.cardCaptureStatus.strokeColor = color
        binding.cardCaptureStatus.strokeWidth = if (color != Color.parseColor("#757575")) 3 else 0
    }

    private fun updateStatistics(state: StatisticsState) {
        // Update counts
        binding.tvSentTodayCount.text = state.sentTodayCount.toString()
        binding.tvPendingCount.text = state.pendingCount.toString()
        binding.tvFailedCount.text = state.failedCount.toString()

        // Update last event
        state.lastSentNotification?.let { notification ->
            val time = timeFormat.format(Date(notification.timestamp))
            val appName = getAppDisplayName(notification.packageName)
            
            // Try to parse amount from notification
            val paymentDetails = PaymentNotificationParser.parse(notification.title, notification.body)
            val amountText = paymentDetails?.let { 
                "${it.currency} ${it.amount}"
            } ?: "Sin monto"
            
            binding.tvLastEvent.text = "$time - $appName\n$amountText"
        } ?: run {
            binding.tvLastEvent.text = "No hay eventos enviados"
        }
    }

    private fun getAppDisplayName(packageName: String): String {
        return when (packageName) {
            "com.bcp.innovacxion.yape.movil" -> "Yape"
            "pe.com.interbank.mobilebanking" -> "Interbank"
            "com.scotiabank.mobile.android" -> "Scotiabank"
            else -> packageName
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
            AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Aceptar") { _, _ -> viewModel.logout() }
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
        lifecycleScope.launch {
            val notificationAccessEnabled = withContext(Dispatchers.IO) {
                NotificationAccessChecker.isNotificationAccessEnabled(this@MainActivity)
            }
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringOptimizations = withContext(Dispatchers.IO) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            }
            updateNotificationPermissionStatus(notificationAccessEnabled)
            updateBatteryOptimizationStatus(isIgnoringOptimizations)
        }
    }

    private fun updateNotificationPermissionStatus(isGranted: Boolean) {
        if (isGranted) {
            binding.tvStatus.text = "✅ Permiso de Notificación: Activado"
            binding.btnEnableNotifications.isEnabled = false
        } else {
            binding.tvStatus.text = "❌ Permiso de Notificación: Desactivado"
            binding.btnEnableNotifications.isEnabled = true
        }
    }

    private fun updateBatteryOptimizationStatus(isIgnoring: Boolean) {
        if (isIgnoring) {
            binding.tvBatteryStatus.text = "✅ Ahorro de Batería: Desactivado"
            binding.btnBatteryOptimization.isEnabled = false
        } else {
            binding.tvBatteryStatus.text = "❌ Ahorro de Batería: Activado (puede detener el servicio)"
            binding.btnBatteryOptimization.isEnabled = true
        }
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
