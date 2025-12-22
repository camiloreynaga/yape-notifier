package com.yapenotifier.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.data.model.MonitorPackage
import com.yapenotifier.android.databinding.ItemMonitoredAppBinding

class MonitoredAppAdapter(
    private val onToggleStatus: (Long) -> Unit
) : ListAdapter<MonitorPackage, MonitoredAppAdapter.MonitoredAppViewHolder>(MonitoredAppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitoredAppViewHolder {
        val binding = ItemMonitoredAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonitoredAppViewHolder(binding, onToggleStatus)
    }

    override fun onBindViewHolder(holder: MonitoredAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MonitoredAppViewHolder(
        private val binding: ItemMonitoredAppBinding,
        private val onToggleStatus: (Long) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(packageItem: MonitorPackage) {
            binding.apply {
                // App name or package name
                tvAppName.text = packageItem.appName ?: packageItem.packageName
                tvPackageName.text = packageItem.packageName

                // Description
                if (packageItem.description.isNullOrBlank()) {
                    tvDescription.visibility = android.view.View.GONE
                } else {
                    tvDescription.text = packageItem.description
                    tvDescription.visibility = android.view.View.VISIBLE
                }

                // Status
                switchMonitor.isChecked = packageItem.isActive
                tvStatus.text = if (packageItem.isActive) {
                    "Monitoreada"
                } else {
                    "No monitoreada"
                }
                tvStatus.setTextColor(
                    if (packageItem.isActive) {
                        android.graphics.Color.parseColor("#4CAF50")
                    } else {
                        android.graphics.Color.parseColor("#757575")
                    }
                )

                // Toggle listener
                switchMonitor.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != packageItem.isActive) {
                        onToggleStatus(packageItem.id)
                    }
                }

                // Priority badge (if available)
                if (packageItem.priority != null && packageItem.priority > 0) {
                    tvPriority.text = "Prioridad: ${packageItem.priority}"
                    tvPriority.visibility = android.view.View.VISIBLE
                } else {
                    tvPriority.visibility = android.view.View.GONE
                }
            }
        }
    }

    class MonitoredAppDiffCallback : DiffUtil.ItemCallback<MonitorPackage>() {
        override fun areItemsTheSame(oldItem: MonitorPackage, newItem: MonitorPackage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MonitorPackage, newItem: MonitorPackage): Boolean {
            return oldItem == newItem
        }
    }
}
