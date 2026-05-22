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
import com.yapenotifier.android.util.OemDetection
import com.yapenotifier.android.util.PaymentNotificationParser
import com.yapenotifier.android.util.ServiceStatusManager
import com.yapenotifier.android.ui.model.AppStatus
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

    /**
     * Dedicated launcher for the silent boot-time request of POST_NOTIFICATIONS.
     * Does NOT send a test notification — just logs the outcome so we know whether
     * the AuthSessionManager "Sesión expirada" notification will actually be visible.
     */
    private val postNotificationsBootLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.i("MainActivity", "POST_NOTIFICATIONS (boot request): granted=$isGranted")
        com.yapenotifier.android.util.FileLogger.log(
            "AUTH",
            "POST_NOTIFICATIONS permission: granted=$isGranted",
            if (isGranted) "info" else "warning",
        )
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
        setupRecoveryCard()
        loadUserInfo()
        updateAllPermissionStatus()
        createNotificationChannel()
        ensurePostNotificationsPermission()
    }

    /**
     * Android 13+ (TIRAMISU) requires runtime POST_NOTIFICATIONS to post any notification.
     * Without it, AuthSessionManager.showLoginRequiredNotification + the watchdog's
     * actionable notifications are silent no-ops — the user never finds out they need
     * to re-login. Request it once at startup so the channel works.
     */
    private fun ensurePostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            postNotificationsBootLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupRecoveryCard() {
        // Show OEM-specific hint if available
        OemDetection.extraHint()?.let { hint ->
            binding.tvRecoveryOemHint.text = hint
            binding.tvRecoveryOemHint.visibility = android.view.View.VISIBLE
        }
        binding.btnRecoveryOpenSettings.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }
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
            // Si el token está expirado, el observer de viewModel.appStatus ya escribió
            // "⚠️ Sesión expirada" en el banner. No pisemos ese estado con "Capturando OK".
            if (preferencesManager.awaitingLogin.first()) {
                return@launch
            }

            val hasPermission = withContext(Dispatchers.IO) {
                NotificationAccessChecker.isNotificationAccessEnabled(this@MainActivity)
            }

            if (!hasPermission) {
                updateCaptureStatus("❌ Sin permiso de notificaciones", Color.parseColor("#F44336"))
                ServiceStatusManager.updateStatus("❌ Permiso de notificaciones no concedido")
                return@launch
            }

            // Fuente de verdad: estado persistido en SharedPreferences, NO heurística de strings
            val isConnected = ServiceStatusManager.isServiceConnected()

            if (isConnected) {
                updateCaptureStatus("✅ Capturando OK", Color.parseColor("#4CAF50"))
                return@launch
            }

            // Permiso activo pero no conectado en ESTE proceso → pedir rebind
            updateCaptureStatus("🔄 Conectando servicio...", Color.parseColor("#2196F3"))
            ServiceStatusManager.updateStatus("⚠️ Intentando conectar servicio...")
            tryRebindNotificationService()

            // Espera corta solo para refrescar UI (NO para decidir que falló OEM)
            delay(3000)

            if (ServiceStatusManager.isServiceConnected()) {
                updateCaptureStatus("✅ Capturando OK", Color.parseColor("#4CAF50"))
            } else {
                // Mostrar botón de acceso a settings como acción, sin afirmar "desconectado definitivo".
                // El observer de statusHistory actualizará la UI cuando onListenerConnected() dispare.
                updateCaptureStatus("⚠️ Conectando… si no conecta, reactiva el permiso", Color.parseColor("#FF9800"))
                binding.btnEnableNotifications.visibility = android.view.View.VISIBLE
                binding.btnEnableNotifications.isEnabled = true
                binding.btnEnableNotifications.text = "Abrir Ajustes de Permiso (Notificaciones)"
                binding.tvStatus.text = "Permiso activo; esperando reenganche del sistema"
                binding.tvStatus.setTextColor(Color.parseColor("#FF9800"))
                binding.ivNotificationStatus.setImageResource(R.drawable.ic_warning_filled)
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

        // Observe AppStatus — single source of truth for the status card
        lifecycleScope.launch {
            viewModel.appStatus.collectLatest { status ->
                updateCaptureStatusFromAppStatus(status)
            }
        }

        lifecycleScope.launch {
            ServiceStatusManager.statusHistory.collectLatest { history ->
                binding.tvServiceLog.text = history.joinToString(separator = "\n")
                // tvLastServiceStatus is still updated from service log for detail text
                if (history.isNotEmpty()) {
                    binding.tvLastServiceStatus.text = history.first()
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

    /**
     * Routes [AppStatus] to the status-card UI. This is the authoritative update path;
     * [updateCaptureStatusFromServiceStatus] is kept for backwards-compat but is no longer
     * called from the status-card path.
     *
     * Priority: TokenExpired > PermissionRevoked > ListenerDeadManualNeeded >
     *           ListenerReconnecting > CapturingOK.
     * 'CapturingOK' is never shown when awaitingLogin == true.
     */
    private fun updateCaptureStatusFromAppStatus(status: AppStatus) {
        val isDeadManual = status is AppStatus.ListenerDeadManualNeeded
        binding.cardRecovery.visibility =
            if (isDeadManual) android.view.View.VISIBLE else android.view.View.GONE

        when (status) {
            is AppStatus.TokenExpired -> {
                updateCaptureStatus("⚠️ Sesión expirada — toca para iniciar sesión", Color.parseColor("#F44336"))
                binding.cardCaptureStatus.setOnClickListener {
                    val intent = Intent(this, SplashActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
            is AppStatus.PermissionRevoked -> {
                updateCaptureStatus("❌ Permiso de notificaciones revocado — abre ajustes para reactivar", Color.parseColor("#F44336"))
                binding.cardCaptureStatus.setOnClickListener {
                    NotificationAccessChecker.openNotificationListenerSettings(this)
                }
            }
            is AppStatus.ListenerDeadManualNeeded -> {
                updateCaptureStatus("⚠️ Servicio detenido", Color.parseColor("#FF9800"))
                binding.cardCaptureStatus.setOnClickListener(null)
            }
            is AppStatus.ListenerReconnecting -> {
                updateCaptureStatus("🔄 Reconectando servicio...", Color.parseColor("#FF9800"))
                binding.cardCaptureStatus.setOnClickListener(null)
            }
            is AppStatus.CapturingOK -> {
                updateCaptureStatus("✅ Capturando OK", Color.parseColor("#4CAF50"))
                binding.cardCaptureStatus.setOnClickListener(null)
            }
        }
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
        // REACTIVO: observa los flows de DataStore continuamente.
        // Cuando AuthSessionManager.handleTokenExpired() borra el authToken, este
        // bloque recibe la emisión nueva y refresca el TextView automáticamente.
        // Antes era .first() snapshot y quedaba congelado en "Autenticado" tras un 401.
        lifecycleScope.launch {
            kotlinx.coroutines.flow.combine(
                preferencesManager.userEmail,
                preferencesManager.authToken,
                preferencesManager.commerceId,
            ) { email, authToken, commerceId ->
                Triple(email, authToken, commerceId)
            }.collectLatest { (email, authToken, commerceId) ->
                when {
                    commerceId.isNullOrBlank() -> {
                        binding.tvUserInfo.text = "⚠️ Dispositivo no vinculado - Escanea código QR"
                        binding.tvUserInfo.setTextColor(Color.parseColor("#F44336"))
                    }
                    authToken.isNullOrBlank() -> {
                        // Token borrado tras 401 — sesión expirada, captador debe re-loguearse
                        binding.tvUserInfo.text = "⚠️ Sesión expirada — toca para iniciar sesión"
                        binding.tvUserInfo.setTextColor(Color.parseColor("#F44336"))
                    }
                    else -> {
                        binding.tvUserInfo.text = "✅ Usuario: ${email ?: "Autenticado"}"
                        binding.tvUserInfo.setTextColor(Color.parseColor("#4CAF50"))
                    }
                }
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
        // Task 2.4 fix: isNotificationAccessEnabled and isIgnoringBatteryOptimizations are
        // synchronous calls that are safe to call on the main thread. By calling them directly
        // (without Dispatchers.IO), the permission rows are populated immediately on every
        // onResume — preventing the "Permisos del Sistema vacíos" bug observed on MIUI/Phone 1
        // where the coroutine dispatch delay left the section empty until the first emission.
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val notificationAccessEnabled =
            NotificationAccessChecker.isNotificationAccessEnabled(this)
        val isIgnoringOptimizations =
            powerManager.isIgnoringBatteryOptimizations(packageName)
        updateNotificationPermissionStatus(notificationAccessEnabled)
        updateBatteryOptimizationStatus(isIgnoringOptimizations)
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
