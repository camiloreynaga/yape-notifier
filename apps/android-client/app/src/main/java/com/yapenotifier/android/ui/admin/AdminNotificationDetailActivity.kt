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

@AndroidEntryPoint
class AdminNotificationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminNotificationDetailBinding
    private val viewModel: AdminNotificationDetailViewModel by viewModels()
    private var notificationId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        notificationId = intent.getLongExtra("notification_id", -1)
        if (notificationId == -1L) {
            finish()
            return
        }

        // ViewModel inyectado automáticamente por Hilt

        setupToolbar()
        setupClickListeners()
        setupObservers()
        loadNotification()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalle de Notificación"
    }

    private fun setupClickListeners() {
        binding.btnMarkValidated.setOnClickListener {
            viewModel.updateStatus("validated")
        }

        binding.btnMarkInconsistent.setOnClickListener {
            viewModel.updateStatus("inconsistent")
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
        binding.apply {
            // App icon and title
            val appName = when (notification.sourceApp.lowercase()) {
                "yape" -> "Yape"
                "plin" -> "Plin"
                "bcp" -> "BCP"
                "interbank" -> "Interbank"
                "bbva" -> "BBVA"
                "scotiabank" -> "Scotiabank"
                else -> notification.sourceApp
            }
            tvAppName.text = appName
            tvTitle.text = notification.title

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
            tvDate.text = formatDateTime(notification.receivedAt)

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
            tvFullText.text = "${notification.title}\n\n${notification.body}"

            // Status
            tvStatus.text = when (notification.status) {
                "pending" -> "Pendiente"
                "validated" -> "Validado"
                "inconsistent" -> "Inconsistente"
                else -> notification.status
            }

            // Action buttons visibility
            val isPending = notification.status == "pending"
            btnMarkValidated.visibility = if (isPending) View.VISIBLE else View.GONE
            btnMarkInconsistent.visibility = if (isPending) View.VISIBLE else View.GONE
        }
    }

    private fun formatDateTime(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return dateString
            val displayFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            displayFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

