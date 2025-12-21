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
                // App icon and info
                tvAppInfo.text = buildAppInfo(notification)
                tvTime.text = formatTime(notification.receivedAt)

                // Title and body
                tvTitle.text = "Confirmación de Pago"
                tvBody.text = notification.body

                // Amount
                notification.amount?.let { amount ->
                    val currency = notification.currency ?: "PEN"
                    tvAmount.text = when (currency) {
                        "PEN" -> "S/ ${String.format("%.2f", amount)}"
                        "USD" -> "$${String.format("%.2f", amount)}"
                        else -> "$currency ${String.format("%.2f", amount)}"
                    }
                    tvAmount.visibility = android.view.View.VISIBLE
                } ?: run {
                    tvAmount.visibility = android.view.View.GONE
                }

                // Status badge
                when (notification.status) {
                    "validated" -> {
                        tvStatus.text = "Verificado"
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                        tvStatus.visibility = android.view.View.VISIBLE
                    }
                    "pending" -> {
                        tvStatus.visibility = android.view.View.GONE
                    }
                    else -> {
                        tvStatus.text = "Cód: ${notification.id}"
                        tvStatus.setBackgroundColor(android.graphics.Color.parseColor("#757575"))
                        tvStatus.visibility = android.view.View.VISIBLE
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

