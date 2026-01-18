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
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.PreferencesManager
import javax.inject.Inject
import com.yapenotifier.android.databinding.ActivityMainBinding
import com.yapenotifier.android.ui.viewmodel.MainViewModel
import com.yapenotifier.android.ui.viewmodel.StatisticsState
import com.yapenotifier.android.ui.viewmodel.StatisticsViewModel
import android.util.Log
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val statisticsViewModel: StatisticsViewModel by viewModels()
    
    @Inject
    lateinit var preferencesManager: PreferencesManager

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
        
        // Dependencias inyectadas automáticamente por Hilt
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModels inyectados automáticamente por Hilt (ya declarados arriba)

        setupUI()
        setupObservers()
        setupClickListeners()
        loadUserInfo()
        updateAllPermissionStatus()
        createNotificationChannel()
    }

    private fun setupUI() {
        binding.tvServiceLog.movementMethod = ScrollingMovementMethod()
        // Initialize with warning state (verifying)
        updateCaptureStatus("Verificando...", Color.parseColor("#FF9800"))

        // Check initial service status
        checkServiceStatus()
    }

    private fun checkServiceStatus() {
        lifecycleScope.launch {
            val notificationAccessEnabled = withContext(Dispatchers.IO) {
                NotificationAccessChecker.isNotificationAccessEnabled(this@MainActivity)
            }

            if (!notificationAccessEnabled) {
                // Permission not granted - service can't work
                updateCaptureStatus("❌ Sin permiso de notificaciones", Color.parseColor("#F44336"))
                ServiceStatusManager.updateStatus("❌ Permiso de notificaciones no concedido")
            } else {
                // Permission granted - check if we have recent activity from the actual service
                val history = ServiceStatusManager.statusHistory.value
                val hasServiceActivity = history.any {
                    it.contains("Servicio Creado") ||
                    it.contains("Conectado") ||
                    it.contains("Escuchando") ||
                    it.contains("apps monitoreadas")
                }

                if (!hasServiceActivity) {
                    // Service not connected yet - try to trigger rebind
                    updateCaptureStatus("🔄 Conectando servicio...", Color.parseColor("#2196F3"))
                    ServiceStatusManager.updateStatus("⚠️ Intentando conectar servicio...")

                    // Try to request rebind
                    tryRebindNotificationService()

                    // Wait for service to connect
                    delay(3000)

                    val updatedHistory = ServiceStatusManager.statusHistory.value
                    val nowConnected = updatedHistory.any {
                        it.contains("Servicio Creado") ||
                        it.contains("Conectado") ||
                        it.contains("Escuchando")
                    }

                    if (nowConnected) {
                        val lastStatus = updatedHistory.first()
                        updateCaptureStatusFromServiceStatus(lastStatus)
                    } else {
                        // Service still not connected - show warning
                        ServiceStatusManager.updateStatus("⚠️ Servicio no conectado - Reinicia permisos")
                        updateCaptureStatus("⚠️ Servicio desconectado", Color.parseColor("#FF9800"))
                    }
                } else {
                    // Service is active - update based on last status
                    val lastStatus = history.first()
                    updateCaptureStatusFromServiceStatus(lastStatus)
                }
            }
        }
    }

    private fun tryRebindNotificationService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val componentName = NotificationAccessChecker.getServiceComponentName(this)
                android.service.notification.NotificationListenerService.requestRebind(componentName)
                Log.i("MainActivity", "🔄 Rebind requested for notification service")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Error requesting rebind", e)
        }
    }


    private fun setupObservers() {
        viewModel.statusMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.logoutComplete.observe(this) { isComplete ->
            if (isComplete) {
                navigateToLinkDevice()
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
        // Re-check service status in case permissions changed
        checkServiceStatus()
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
        
        // Update status indicator drawable based on color
        val indicatorDrawable = when (color) {
            Color.parseColor("#4CAF50") -> R.drawable.bg_status_active  // Green - Active
            Color.parseColor("#FF9800") -> R.drawable.bg_status_warning // Orange - Warning
            Color.parseColor("#F44336") -> R.drawable.bg_status_error   // Red - Error
            else -> R.drawable.bg_status_warning // Default to warning (blue/processing)
        }
        binding.viewStatusIndicator.setBackgroundResource(indicatorDrawable)
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
                .setTitle("Desvincular Dispositivo")
                .setMessage("¿Estás seguro de que quieres desvincular este dispositivo? Deberás volver a vincularlo con un código QR para continuar capturando notificaciones.")
                .setPositiveButton("Desvincular") { _, _ -> viewModel.unlinkDevice() }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.btnSendTestNotification.setOnClickListener {
            requestPostNotificationPermissionAndSend()
        }

        binding.btnViewDatabase.setOnClickListener {
            startActivity(Intent(this, CapturedNotificationsActivity::class.java))
        }

        binding.btnViewDebugLogs.setOnClickListener {
            startActivity(Intent(this, DebugLogsActivity::class.java))
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch {
            val email = preferencesManager.userEmail.first()
            val authToken = preferencesManager.authToken.first()
            val commerceId = preferencesManager.commerceId.first()
            
            // Professional Architecture: Authentication is OPTIONAL
            // Device linking (commerce_id) is what matters for sending notifications
            
            if (commerceId.isNullOrBlank()) {
                // Device not linked - this is critical
                binding.tvUserInfo.text = "⚠️ Dispositivo no vinculado - Escanea código QR"
                binding.tvUserInfo.setTextColor(Color.parseColor("#F44336"))
            } else if (authToken.isNullOrBlank()) {
                // Device linked but no user session (capturer mode)
                binding.tvUserInfo.text = "✅ Modo Capturador (sin usuario)"
                binding.tvUserInfo.setTextColor(Color.parseColor("#FF9800")) // Orange
            } else {
                // Device linked AND user authenticated (full mode)
                binding.tvUserInfo.text = "✅ Usuario: ${email ?: "Autenticado"}"
                binding.tvUserInfo.setTextColor(Color.parseColor("#4CAF50")) // Green
            }
        }
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Sesión Requerida")
            .setMessage("No tienes una sesión activa. Las notificaciones se capturarán localmente pero NO se enviarán a la API hasta que inicies sesión.\n\n¿Deseas iniciar sesión ahora?")
            .setPositiveButton("Iniciar Sesión") { _, _ ->
                // Navegar a la pantalla de login
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Más Tarde", null)
            .setCancelable(true)
            .show()
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
            binding.tvStatus.text = "Permiso de Notificación: Activado"
            binding.tvStatus.setTextColor(Color.parseColor("#374151"))
            binding.ivNotificationStatus.setImageResource(R.drawable.ic_check_circle_filled)
            binding.btnEnableNotifications.visibility = android.view.View.GONE
        } else {
            binding.tvStatus.text = "Permiso de Notificación: Desactivado"
            binding.tvStatus.setTextColor(Color.parseColor("#EF4444"))
            binding.ivNotificationStatus.setImageResource(R.drawable.ic_warning_filled)
            binding.btnEnableNotifications.visibility = android.view.View.VISIBLE
            binding.btnEnableNotifications.isEnabled = true
        }
    }

    private fun updateBatteryOptimizationStatus(isIgnoring: Boolean) {
        if (isIgnoring) {
            binding.tvBatteryStatus.text = "Ahorro de Batería: Desactivado"
            binding.tvBatteryStatus.setTextColor(Color.parseColor("#374151"))
            binding.ivBatteryStatus.setImageResource(R.drawable.ic_check_circle_filled)
            binding.btnBatteryOptimization.visibility = android.view.View.GONE
        } else {
            binding.tvBatteryStatus.text = "Ahorro de Batería: Activo ⚠️"
            binding.tvBatteryStatus.setTextColor(Color.parseColor("#F59E0B"))
            binding.ivBatteryStatus.setImageResource(R.drawable.ic_warning_filled)
            binding.btnBatteryOptimization.visibility = android.view.View.VISIBLE
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

    private fun navigateToLinkDevice() {
        val intent = Intent(this, LinkDeviceActivity::class.java)
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
