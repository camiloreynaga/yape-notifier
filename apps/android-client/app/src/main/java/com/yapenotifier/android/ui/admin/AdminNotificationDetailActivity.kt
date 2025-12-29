package com.yapenotifier.android.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivityAdminNotificationDetailBinding
import com.yapenotifier.android.ui.admin.viewmodel.AdminNotificationDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone

@AndroidEntryPoint
class AdminNotificationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminNotificationDetailBinding
    private val viewModel: AdminNotificationDetailViewModel by viewModels()
    private var notificationId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityAdminNotificationDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            notificationId = intent.getLongExtra("notification_id", -1L)
            if (notificationId == -1L) {
                Toast.makeText(this, "ID de notificación no válido", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // ViewModel inyectado automáticamente por Hilt

            setupToolbar()
            setupClickListeners()
            setupObservers()
            loadNotification()
        } catch (e: Exception) {
            android.util.Log.e("AdminNotificationDetail", "Error en onCreate", e)
            Toast.makeText(this, "Error al cargar detalles: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Detalle de Notificación"
        } catch (e: Exception) {
            android.util.Log.w("AdminNotificationDetail", "Error configurando toolbar", e)
        }
    }

    private fun setupClickListeners() {
        binding.btnMarkValidated.setOnClickListener {
            viewModel.updateStatus("validated")
        }

        binding.btnMarkInconsistent.setOnClickListener {
            viewModel.updateStatus("inconsistent")
        }

        binding.btnMarkPending.setOnClickListener {
            viewModel.updateStatus("pending")
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE

            state.notification?.let { notification ->
                displayNotification(notification)
            }

            state.error?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }

            if (state.statusUpdated) {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
                loadNotification() // Reload to get updated status
            }
        }
    }

    private fun loadNotification() {
        viewModel.loadNotification(notificationId)
    }

    private fun displayNotification(notification: com.yapenotifier.android.data.model.Notification) {
        try {
            binding.apply {
                // App icon and title
                val appName = when (notification.sourceApp?.lowercase() ?: "") {
                    "yape" -> "Yape"
                    "plin" -> "Plin"
                    "bcp" -> "BCP"
                    "interbank" -> "Interbank"
                    "bbva" -> "BBVA"
                    "scotiabank" -> "Scotiabank"
                    else -> notification.sourceApp ?: "Desconocido"
                }
                tvAppName.text = appName
                tvTitle.text = notification.title ?: "Sin título"

                // Payment information
                notification.payerName?.let {
                    tvPayerName.text = it
                    tvPayerName.visibility = View.VISIBLE
                } ?: run {
                    tvPayerName.visibility = View.GONE
                }

                notification.amount?.let { amount ->
                    val currency = notification.currency ?: "PEN"
                    val amountText = when (currency) {
                        "PEN" -> "S/${String.format("%.2f", amount)}"
                        "USD" -> "$${String.format("%.2f", amount)}"
                        else -> "$currency ${String.format("%.2f", amount)}"
                    }
                    tvAmount.text = amountText
                    tvAmount.visibility = View.VISIBLE
                } ?: run {
                    tvAmount.visibility = View.GONE
                }

                // Date and time
                tvDate.text = formatDateTime(notification.receivedAt ?: "")

                // Technical information
                tvPackageName.text = notification.packageName ?: "N/A"
                notification.appInstance?.label?.let {
                    tvInstanceLabel.text = it
                    tvInstanceLabel.visibility = View.VISIBLE
                } ?: run {
                    tvInstanceLabel.visibility = View.GONE
                }
                tvDeviceName.text = notification.device?.name ?: "N/A"
                notification.androidUserId?.let {
                    tvAndroidUserId.text = it.toString()
                    tvAndroidUserId.visibility = View.VISIBLE
                } ?: run {
                    tvAndroidUserId.visibility = View.GONE
                }

                // Full notification text
                val title = notification.title ?: ""
                val body = notification.body ?: ""
                tvFullText.text = if (title.isNotEmpty() && body.isNotEmpty()) {
                    "$title\n\n$body"
                } else if (title.isNotEmpty()) {
                    title
                } else if (body.isNotEmpty()) {
                    body
                } else {
                    "Sin contenido"
                }

                // Status
                tvStatus.text = when (notification.status) {
                    "pending" -> "Pendiente"
                    "validated" -> "Validado"
                    "inconsistent" -> "Inconsistente"
                    else -> notification.status ?: "Desconocido"
                }

                // Action buttons visibility - Show all buttons, highlight current status
                btnMarkValidated.visibility = View.VISIBLE
                btnMarkInconsistent.visibility = View.VISIBLE
                btnMarkPending.visibility = View.VISIBLE
                
                // Highlight current status button
                when (notification.status) {
                    "pending" -> {
                        btnMarkPending.isEnabled = false
                        btnMarkValidated.isEnabled = true
                        btnMarkInconsistent.isEnabled = true
                    }
                    "validated" -> {
                        btnMarkValidated.isEnabled = false
                        btnMarkPending.isEnabled = true
                        btnMarkInconsistent.isEnabled = true
                    }
                    "inconsistent" -> {
                        btnMarkInconsistent.isEnabled = false
                        btnMarkPending.isEnabled = true
                        btnMarkValidated.isEnabled = true
                    }
                    else -> {
                        // Enable all buttons if status is unknown
                        btnMarkPending.isEnabled = true
                        btnMarkValidated.isEnabled = true
                        btnMarkInconsistent.isEnabled = true
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminNotificationDetail", "Error mostrando notificación", e)
            Toast.makeText(this, "Error al mostrar detalles: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatDateTime(dateString: String?): String {
        if (dateString.isNullOrBlank()) {
            return "Fecha no disponible"
        }
        return try {
            // Try ISO 8601 format first (from API)
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            
            var date: Date? = null
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    date = sdf.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) {
                    // Try next format
                }
            }
            
            if (date != null) {
                val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                displayFormat.format(date)
            } else {
                dateString // Return original if parsing fails
            }
        } catch (e: Exception) {
            android.util.Log.w("AdminNotificationDetail", "Error formateando fecha: $dateString", e)
            dateString
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

