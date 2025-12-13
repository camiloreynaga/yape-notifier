package com.yapenotifier.android.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityNotificationDiagnosticsBinding
import com.yapenotifier.android.util.NotificationAccessChecker
import com.yapenotifier.android.util.OemDetector
import com.yapenotifier.android.util.ServiceRebinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Activity for diagnosing and managing Notification Listener Service status.
 * 
 * This activity provides:
 * - Real-time status of the service
 * - Component state verification
 * - Battery optimization status
 * - OEM-specific recommendations
 * - Actions to enable access and rebind the service
 */
class NotificationDiagnosticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationDiagnosticsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        updateStatus()
        setupOemInfo()
    }

    override fun onResume() {
        super.onResume()
        // Refresh status when returning from settings
        updateStatus()
    }

    private fun setupClickListeners() {
        binding.btnOpenSettings.setOnClickListener {
            NotificationAccessChecker.openNotificationListenerSettings(this)
            Toast.makeText(
                this,
                "Habilita 'Yape Notifier' en la lista de servicios",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnRebindService.setOnClickListener {
            rebindService()
        }

        binding.btnViewCapturedNotifications.setOnClickListener {
            val intent = Intent(this, CapturedNotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        // Check notification access
        val isAccessEnabled = NotificationAccessChecker.isNotificationAccessEnabled(this)
        val accessStatus = if (isAccessEnabled) {
            "✅ Acceso a Notificaciones: HABILITADO"
        } else {
            "❌ Acceso a Notificaciones: DESHABILITADO"
        }
        binding.tvServiceStatus.text = accessStatus

        // Check component state
        val isComponentEnabled = ServiceRebinder.isServiceComponentEnabled(this)
        val componentStatus = if (isComponentEnabled) {
            "✅ Componente del Servicio: HABILITADO"
        } else {
            "❌ Componente del Servicio: DESHABILITADO"
        }
        binding.tvComponentStatus.text = componentStatus

        // Check battery optimization
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
        val batteryStatus = if (isIgnoringOptimizations) {
            "✅ Optimización de Batería: DESACTIVADA"
        } else {
            "⚠️ Optimización de Batería: ACTIVADA (puede afectar el servicio)"
        }
        binding.tvBatteryStatus.text = batteryStatus
    }

    private fun setupOemInfo() {
        val oem = OemDetector.detectOem()
        val oemName = OemDetector.getOemDisplayName(oem)
        binding.tvOemInfo.text = "Fabricante: $oemName\nModelo: ${android.os.Build.MODEL}"

        // Show OEM-specific recommendations
        val recommendations = OemDetector.getOemRecommendations(oem)
        if (recommendations.isNotEmpty()) {
            binding.tvOemRecommendations.text = recommendations.joinToString("\n\n")
            binding.cardOemRecommendations.visibility = android.view.View.VISIBLE
        } else {
            binding.cardOemRecommendations.visibility = android.view.View.GONE
        }
    }

    private fun rebindService() {
        Toast.makeText(this, "Reiniciando servicio...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            val success = ServiceRebinder.rebindNotificationListener(this@NotificationDiagnosticsActivity)
            
            // Wait a moment for the service to reconnect
            delay(1000)
            
            updateStatus()
            
            if (success) {
                Toast.makeText(
                    this@NotificationDiagnosticsActivity,
                    "Servicio reiniciado. Verifica el estado arriba.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@NotificationDiagnosticsActivity,
                    "Error al reiniciar. Puede ser necesario habilitar el acceso manualmente.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

