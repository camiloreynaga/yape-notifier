package com.yapenotifier.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.R
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
            val context = binding.root.context
            
            binding.apply {
                // App name (extracted from package)
                tvAppName.text = instance.appName
                
                // Package name
                tvPackageName.text = instance.packageName
                
                // Android User ID
                tvAndroidUserId.text = "User ID: ${instance.androidUserId ?: 0}"
                
                // Type Badge (Original / Clon)
                if (instance.isClone) {
                    tvTypeBadge.text = "CLON"
                    tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_dual)
                    tvTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_clone_text))
                    
                    // Show clone badge on icon
                    tvCloneBadge.visibility = View.VISIBLE
                    tvCloneBadge.text = "C"
                    
                    // Change icon background for clone
                    viewIconBackground.setBackgroundResource(R.drawable.bg_rounded_blue)
                } else {
                    tvTypeBadge.text = "ORIGINAL"
                    tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_original)
                    tvTypeBadge.setTextColor(ContextCompat.getColor(context, R.color.badge_original_text))
                    
                    // Hide clone badge on icon
                    tvCloneBadge.visibility = View.GONE
                    
                    // Use default icon background
                    viewIconBackground.setBackgroundResource(R.drawable.bg_rounded_purple_dark)
                }
                
                // Set app-specific icon background color
                when {
                    instance.packageName.contains("yape", ignoreCase = true) -> {
                        viewIconBackground.setBackgroundResource(R.drawable.bg_app_icon_yape)
                    }
                    instance.packageName.contains("plin", ignoreCase = true) -> {
                        viewIconBackground.setBackgroundResource(R.drawable.bg_app_icon_plin)
                    }
                    instance.packageName.contains("bcp", ignoreCase = true) -> {
                        viewIconBackground.setBackgroundResource(R.drawable.bg_app_icon_bcp)
                    }
                }
                
                // Set current label or placeholder
                etInstanceLabel.setText(instance.label ?: "")
                
                // Update hint based on type
                tilInstanceLabel.hint = if (instance.isClone) {
                    "Nombre del clon (ej: Cuenta Rocío)"
                } else {
                    "Nombre original (ej: Cuenta Principal)"
                }
                
                // Update label when text changes (on focus lost)
                etInstanceLabel.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val newLabel = etInstanceLabel.text.toString().trim()
                        val currentLabel = instance.label
                        if (newLabel != currentLabel) {
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
