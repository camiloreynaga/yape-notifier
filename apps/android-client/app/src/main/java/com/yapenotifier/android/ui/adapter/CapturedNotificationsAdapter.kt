package com.yapenotifier.android.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.db.CapturedNotification
import com.yapenotifier.android.databinding.ItemCapturedNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CapturedNotificationsAdapter : ListAdapter<CapturedNotification, CapturedNotificationsAdapter.NotificationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemCapturedNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification)
    }

    class NotificationViewHolder(private val binding: ItemCapturedNotificationBinding) : RecyclerView.ViewHolder(binding.root) {
        private val timestampFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        fun bind(notification: CapturedNotification) {
            binding.tvPackageName.text = notification.packageName
            binding.tvTitle.text = notification.title
            binding.tvBody.text = notification.body
            binding.tvTimestamp.text = timestampFormat.format(Date(notification.timestamp))
            binding.tvStatus.text = notification.status

            // Set status color
            val statusColor = when (notification.status) {
                "SENT" -> Color.parseColor("#4CAF50") // Green
                "FAILED" -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#FFA726") // Orange
            }
            binding.tvStatus.background.setTint(statusColor)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CapturedNotification>() {
        override fun areItemsTheSame(oldItem: CapturedNotification, newItem: CapturedNotification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CapturedNotification, newItem: CapturedNotification): Boolean {
            return oldItem == newItem
        }
    }
}
