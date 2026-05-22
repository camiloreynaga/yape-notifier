package com.yapenotifier.android.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.data.model.Device
import com.yapenotifier.android.databinding.ItemDeviceCardBinding
import java.text.SimpleDateFormat
import java.util.*

class DeviceAdapter(
    private val onEditClick: (Device) -> Unit,
    private val onDeleteClick: (Device) -> Unit,
    private val onExpandClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private val expandedDevices = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onEditClick, onDeleteClick, onExpandClick, expandedDevices)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceCardBinding,
        private val onEditClick: (Device) -> Unit,
        private val onDeleteClick: (Device) -> Unit,
        private val onExpandClick: (Device) -> Unit,
        private val expandedDevices: MutableSet<Long>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.apply {
                // Device name
                tvDeviceName.text = device.name

                // Status badge (Online/Offline) - Use last_heartbeat for connection status
                val isOnline = isDeviceOnline(device)
                if (isOnline) {
                    tvStatus.text = "En línea"
                    tvStatus.setBackgroundResource(com.yapenotifier.android.R.drawable.bg_badge_online)
                } else {
                    tvStatus.text = "Desconectado"
                    tvStatus.setBackgroundResource(com.yapenotifier.android.R.drawable.bg_badge_offline)
                }

                // Last activity - Use last_heartbeat if available, otherwise last_seen_at
                val lastActivity = device.lastHeartbeat ?: device.lastSeenAt
                tvLastActivity.text = formatLastActivity(lastActivity)

                // Expand/collapse
                val isExpanded = expandedDevices.contains(device.id)
                llExpandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
                ivExpand.setImageResource(
                    if (isExpanded) {
                        com.yapenotifier.android.R.drawable.ic_expand_less
                    } else {
                        com.yapenotifier.android.R.drawable.ic_expand_more
                    }
                )

                // Health status
                val healthStatus = determineHealthStatus(device)
                tvHealthStatus.text = healthStatus.first
                tvHealthStatus.setBackgroundResource(healthStatus.second)

                // Battery, WiFi, Permissions indicators
                device.batteryLevel?.let { level ->
                    tvBatteryLevel.text = "$level%"
                    ivBattery.setImageResource(
                        when {
                            level > 50 -> com.yapenotifier.android.R.drawable.ic_battery_full
                            level > 20 -> com.yapenotifier.android.R.drawable.ic_battery_medium
                            else -> com.yapenotifier.android.R.drawable.ic_battery_low
                        }
                    )
                } ?: run {
                    tvBatteryLevel.text = "N/A"
                    ivBattery.setImageResource(com.yapenotifier.android.R.drawable.ic_battery_unknown)
                }

                // Notification permission
                device.notificationPermissionEnabled?.let { enabled ->
                    ivNotificationPermission.setImageResource(
                        if (enabled) {
                            com.yapenotifier.android.R.drawable.ic_check_circle
                        } else {
                            com.yapenotifier.android.R.drawable.ic_error_circle
                        }
                    )
                } ?: run {
                    ivNotificationPermission.setImageResource(com.yapenotifier.android.R.drawable.ic_help_circle)
                }

                // Battery optimization
                device.batteryOptimizationDisabled?.let { disabled ->
                    ivBatteryOptimization.setImageResource(
                        if (disabled) {
                            com.yapenotifier.android.R.drawable.ic_check_circle
                        } else {
                            com.yapenotifier.android.R.drawable.ic_error_circle
                        }
                    )
                } ?: run {
                    ivBatteryOptimization.setImageResource(com.yapenotifier.android.R.drawable.ic_help_circle)
                }

                // Click listeners
                root.setOnClickListener {
                    if (isExpanded) {
                        expandedDevices.remove(device.id)
                    } else {
                        expandedDevices.add(device.id)
                    }
                    onExpandClick(device)
                }

                btnEdit.setOnClickListener {
                    onEditClick(device)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(device)
                }
            }
        }

        private fun isDeviceOnline(device: Device): Boolean {
            // Use last_heartbeat instead of last_seen_at for connection status
            // Device is online if last_heartbeat is within 5 minutes
            val lastHeartbeat = device.lastHeartbeat ?: return false
            return try {
                // Parse ISO 8601 format or standard format
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val sdf2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val lastHeartbeatDate = try {
                    sdf.parse(lastHeartbeat)
                } catch (e: Exception) {
                    sdf2.parse(lastHeartbeat)
                } ?: return false
                
                val now = Date()
                val diff = now.time - lastHeartbeatDate.time
                diff < 5 * 60 * 1000 // 5 minutes
            } catch (e: Exception) {
                false
            }
        }

        private fun formatLastActivity(lastSeenAt: String?): String {
            if (lastSeenAt == null) return "Nunca"
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val lastSeen = sdf.parse(lastSeenAt) ?: return "Nunca"
                val now = Date()
                val diff = now.time - lastSeen.time

                when {
                    diff < 60000 -> "Hace un momento"
                    diff < 3600000 -> "Hace ${diff / 60000} minutos"
                    diff < 86400000 -> "Hace ${diff / 3600000} horas"
                    else -> "Hace ${diff / 86400000} días"
                }
            } catch (e: Exception) {
                "Nunca"
            }
        }

        private fun determineHealthStatus(device: Device): Pair<String, Int> {
            var issues = 0
            if (device.batteryLevel != null && device.batteryLevel < 20) issues++
            if (device.notificationPermissionEnabled == false) issues++
            if (device.batteryOptimizationDisabled == false) issues++
            if (!isDeviceOnline(device)) issues++

            return when {
                issues == 0 -> Pair("OK", com.yapenotifier.android.R.drawable.bg_badge_success)
                issues <= 2 -> Pair("Advertencia", com.yapenotifier.android.R.drawable.bg_badge_warning)
                else -> Pair("Error", com.yapenotifier.android.R.drawable.bg_badge_error)
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}

