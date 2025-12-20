package com.yapenotifier.android.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.data.model.AppInstance
import com.yapenotifier.android.databinding.ItemAppInstanceBinding

class AppInstanceAdapter(
    private val onLabelChanged: (Long, String) -> Unit
) : ListAdapter<AppInstance, AppInstanceAdapter.AppInstanceViewHolder>(AppInstanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppInstanceViewHolder {
        val binding = ItemAppInstanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppInstanceViewHolder(binding, onLabelChanged)
    }

    override fun onBindViewHolder(holder: AppInstanceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AppInstanceViewHolder(
        private val binding: ItemAppInstanceBinding,
        private val onLabelChanged: (Long, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(instance: AppInstance) {
            binding.apply {
                tvPackageName.text = instance.packageName
                tvAndroidUserId.text = "Usuario Android: ${instance.androidUserId}"
                
                // Set current label or placeholder
                etInstanceLabel.setText(instance.instanceLabel ?: "")
                etInstanceLabel.hint = "Ej: Yape 1 (Rocío)"
                
                // Update label when text changes (debounced or on focus lost)
                etInstanceLabel.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newLabel = etInstanceLabel.text.toString().trim()
                        if (newLabel != instance.instanceLabel) {
                            onLabelChanged(instance.id, newLabel)
                        }
                    }
                }
            }
        }
    }

    class AppInstanceDiffCallback : DiffUtil.ItemCallback<AppInstance>() {
        override fun areItemsTheSame(oldItem: AppInstance, newItem: AppInstance): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppInstance, newItem: AppInstance): Boolean {
            return oldItem == newItem
        }
    }
}

