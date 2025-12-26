package com.yapenotifier.android.ui.admin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.data.model.Notification
import com.yapenotifier.android.databinding.ItemNotificationCardBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationCardBinding,
        private val onItemClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                // App icon
                val appIconBg = when (notification.sourceApp.lowercase()) {
                    "yape" -> com.yapenotifier.android.R.drawable.bg_app_icon_yape
                    "plin" -> com.yapenotifier.android.R.drawable.bg_app_icon_plin
                    "bcp" -> com.yapenotifier.android.R.drawable.bg_app_icon_bcp
                    else -> com.yapenotifier.android.R.drawable.bg_app_icon_yape
                }
                ivAppIcon.setBackgroundResource(appIconBg)
                // You can set a specific icon here if needed
                ivAppIcon.setImageResource(com.yapenotifier.android.R.drawable.ic_bell_notification)

                // App info and time
                tvAppInfo.text = buildAppInfo(notification)
                tvTime.text = formatTime(notification.receivedAt)

                // Title and body
                tvTitle.text = "Confirmación de Pago"
                
                // Build body with amount highlighted
                notification.amount?.let { amount ->
                    val currency = notification.currency ?: "PEN"
                    val amountText = when (currency) {
                        "PEN" -> "S/${String.format("%.2f", amount)}"
                        "USD" -> "$${String.format("%.2f", amount)}"
                        else -> "$currency ${String.format("%.2f", amount)}"
                    }
                    val payerName = notification.payerName ?: "Usuario"
                    tvBody.text = "$payerName te envió un pago por $amountText"
                    tvAmount.text = amountText
                    tvAmount.visibility = android.view.View.VISIBLE
                } ?: run {
                    tvBody.text = notification.body
                    tvAmount.visibility = android.view.View.GONE
                }

                // Status badge
                when (notification.status) {
                    "validated" -> {
                        llStatusBadge.visibility = android.view.View.VISIBLE
                        llStatusBadge.setBackgroundResource(com.yapenotifier.android.R.drawable.bg_badge_verified)
                        tvStatus.text = "Verificado"
                        ivCheckmark.visibility = android.view.View.VISIBLE
                        tvCode.visibility = android.view.View.GONE
                    }
                    "pending" -> {
                        llStatusBadge.visibility = android.view.View.GONE
                        tvCode.visibility = android.view.View.GONE
                    }
                    else -> {
                        llStatusBadge.visibility = android.view.View.GONE
                        tvCode.text = "Cód: ${notification.id}"
                        tvCode.visibility = android.view.View.VISIBLE
                    }
                }

                root.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }

        private fun buildAppInfo(notification: Notification): String {
            val appName = when (notification.sourceApp) {
                "yape" -> "Yape"
                "plin" -> "Plin"
                "bcp" -> "BCP"
                "interbank" -> "Interbank"
                "bbva" -> "BBVA"
                "scotiabank" -> "Scotiabank"
                else -> notification.sourceApp
            }

            val instanceLabel = notification.appInstance?.label
            val deviceName = notification.device?.name ?: "Desconocido"

            return buildString {
                append(appName)
                instanceLabel?.let { label -> append(" • $label") }
                append(" • $deviceName")
            }
        }

        private fun formatTime(dateString: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(dateString) ?: return dateString
                val now = Date()
                val diff = now.time - date.time

                when {
                    diff < 60000 -> "Ahora"
                    diff < 3600000 -> "${diff / 60000} min"
                    diff < 86400000 -> "${diff / 3600000} h"
                    else -> "${diff / 86400000} d"
                }
            } catch (e: Exception) {
                dateString
            }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}

