package com.yapenotifier.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.databinding.ItemMonitoredAppBinding

data class MonitoredAppCheckableItem(
    val packageName: String,
    var isChecked: Boolean = false
)

class MonitoredAppsAdapter(
    private val appItems: MutableList<MonitoredAppCheckableItem>
) : RecyclerView.Adapter<MonitoredAppsAdapter.MonitoredAppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitoredAppViewHolder {
        val binding = ItemMonitoredAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MonitoredAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonitoredAppViewHolder, position: Int) {
        holder.bind(appItems[position])
    }

    override fun getItemCount(): Int = appItems.size

    fun getSelectedPackages(): List<String> {
        return appItems.filter { it.isChecked }.map { it.packageName }
    }

    private fun getAppDisplayName(packageName: String): String {
        return when (packageName) {
            "com.bcp.innovacxion.yape.movil" -> "Yape"
            "pe.com.interbank.mobilebanking" -> "Interbank"
            "com.scotiabank.mobile.android" -> "Scotiabank"
            else -> packageName
        }
    }

    inner class MonitoredAppViewHolder(private val binding: ItemMonitoredAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MonitoredAppCheckableItem) {
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.tvAppName.text = getAppDisplayName(item.packageName)
            binding.tvPackageName.text = item.packageName
            binding.checkbox.isChecked = item.isChecked
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    appItems[bindingAdapterPosition].isChecked = isChecked
                }
            }
        }
    }
}
