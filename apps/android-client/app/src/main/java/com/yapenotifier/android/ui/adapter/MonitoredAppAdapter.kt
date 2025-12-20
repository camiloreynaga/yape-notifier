package com.yapenotifier.android.ui.adapter

import android.widget.CompoundButton
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.databinding.ItemMonitoredAppBinding

data class MonitoredAppItem(
    val packageName: String,
    val displayName: String,
    var isSelected: Boolean
)

class MonitoredAppAdapter(
    private val onItemCheckedChanged: (String, Boolean) -> Unit
) : ListAdapter<MonitoredAppItem, MonitoredAppAdapter.MonitoredAppViewHolder>(MonitoredAppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitoredAppViewHolder {
        val binding = ItemMonitoredAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonitoredAppViewHolder(binding, onItemCheckedChanged)
    }

    override fun onBindViewHolder(holder: MonitoredAppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MonitoredAppViewHolder(
        private val binding: ItemMonitoredAppBinding,
        private val onItemCheckedChanged: (String, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MonitoredAppItem) {
            binding.apply {
                tvAppName.text = item.displayName
                tvPackageName.text = item.packageName
                
                // Remove listener first to avoid triggering during setChecked
                checkbox.setOnCheckedChangeListener(null)
                checkbox.isChecked = item.isSelected
                
                // Set listener with explicit types to help type inference
                checkbox.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    item.isSelected = isChecked
                    onItemCheckedChanged(item.packageName, isChecked)
                }
            }
        }
    }

    class MonitoredAppDiffCallback : DiffUtil.ItemCallback<MonitoredAppItem>() {
        override fun areItemsTheSame(oldItem: MonitoredAppItem, newItem: MonitoredAppItem): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: MonitoredAppItem, newItem: MonitoredAppItem): Boolean {
            return oldItem == newItem
        }
    }
}

