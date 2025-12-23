package com.yapenotifier.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.databinding.ItemMonitoredAppBinding

data class MonitoredAppCheckableItem(
    val packageName: String,
    var isChecked: Boolean = false
)

class MonitoredAppsAdapter(
    private val onAppChecked: (MonitoredAppCheckableItem, Boolean) -> Unit
) : ListAdapter<MonitoredAppCheckableItem, MonitoredAppsAdapter.MonitoredAppViewHolder>(MonitoredAppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitoredAppViewHolder {
        val binding = ItemMonitoredAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonitoredAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonitoredAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedPackages(): List<String> {
        return currentList.filter { it.isChecked }.map { it.packageName }
    }

    inner class MonitoredAppViewHolder(private val binding: ItemMonitoredAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MonitoredAppCheckableItem) {
            binding.switchMonitor.setOnCheckedChangeListener(null)
            binding.tvAppName.text = getAppDisplayName(item.packageName)
            binding.tvPackageName.text = item.packageName
            binding.switchMonitor.isChecked = item.isChecked
            binding.switchMonitor.setOnCheckedChangeListener { _, isChecked ->
                // Use the 'item' passed to bind for better safety inside a listener
                if (item.isChecked != isChecked) {
                    onAppChecked(item, isChecked)
                }
            }
        }
    }

    private class MonitoredAppDiffCallback : DiffUtil.ItemCallback<MonitoredAppCheckableItem>() {
        override fun areItemsTheSame(oldItem: MonitoredAppCheckableItem, newItem: MonitoredAppCheckableItem): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: MonitoredAppCheckableItem, newItem: MonitoredAppCheckableItem): Boolean {
            return oldItem == newItem
        }
    }
    
    companion object {
        private fun getAppDisplayName(packageName: String): String {
            return when (packageName) {
                "com.bcp.innovacxion.yape.movil" -> "Yape"
                "pe.com.interbank.mobilebanking" -> "Interbank"
                "com.scotiabank.mobile.android" -> "Scotiabank"
                else -> packageName
            }
        }
    }
}
