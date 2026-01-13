package com.yapenotifier.android.ui.admin.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.R
import com.yapenotifier.android.data.model.Notification
import com.yapenotifier.android.databinding.ItemNotificationCardBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit,
    private val onValidateClick: (Notification) -> Unit,
    private val onMarkInconsistent: (Notification) -> Unit,
    private val onMarkPending: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    companion object {
        // Static map to ensure each instance name gets a unique color
        private val instanceColorMap = mutableMapOf<String, Int>()
        private var nextColorIndex = 0

        // Color palette (excluding green to avoid confusion with "validated" status)
        private val colorPalette = listOf(
            Pair(R.drawable.bg_instance_badge_color_1, "#DC2626"),  // Red
            Pair(R.drawable.bg_instance_badge_color_2, "#1D4ED8"),  // Blue
            Pair(R.drawable.bg_instance_badge_color_4, "#D97706"),  // Orange/Amber
            Pair(R.drawable.bg_instance_badge_color_5, "#7C3AED"),  // Purple
            Pair(R.drawable.bg_instance_badge_color_6, "#0891B2"),  // Cyan
            Pair(R.drawable.bg_instance_badge_color_7, "#BE185D"),  // Pink
            Pair(R.drawable.bg_instance_badge_color_8, "#4B5563"),  // Gray
        )

        /**
         * Get a unique color for an instance label.
         * Each unique name will always get the same color, and no two names share a color
         * (until we run out of colors in the palette).
         */
        fun getInstanceColor(instanceLabel: String): Pair<Int, String> {
            val normalizedLabel = instanceLabel.lowercase().trim()

            // Check if this label already has an assigned color
            val existingIndex = instanceColorMap[normalizedLabel]
            if (existingIndex != null) {
                return colorPalette[existingIndex]
            }

            // Assign next available color
            val colorIndex = nextColorIndex % colorPalette.size
            instanceColorMap[normalizedLabel] = colorIndex
            nextColorIndex++

            return colorPalette[colorIndex]
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding, onItemClick, onValidateClick, onMarkInconsistent, onMarkPending)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        private val binding: ItemNotificationCardBinding,
        private val onItemClick: (Notification) -> Unit,
        private val onValidateClick: (Notification) -> Unit,
        private val onMarkInconsistent: (Notification) -> Unit,
        private val onMarkPending: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            binding.apply {
                // App Badge
                val appName = when (notification.sourceApp.lowercase()) {
                    "yape" -> "Yape"
                    "plin" -> "Plin"
                    "bcp" -> "BCP"
                    "interbank" -> "Interbank"
                    "bbva" -> "BBVA"
                    "scotiabank" -> "Scotiabank"
                    else -> notification.sourceApp.capitalize()
                }
                tvAppBadge.text = appName

                val appBadgeBg = when (notification.sourceApp.lowercase()) {
                    "yape" -> R.drawable.bg_app_badge_yape
                    "plin" -> R.drawable.bg_app_badge_plin
                    "bcp" -> R.drawable.bg_app_badge_bcp
                    else -> R.drawable.bg_app_badge_yape
                }
                tvAppBadge.setBackgroundResource(appBadgeBg)

                // Instance Badge (Color-coded dynamically)
                val instanceLabel = notification.appInstance?.label
                if (instanceLabel != null) {
                    tvInstanceBadge.text = instanceLabel
                    tvInstanceBadge.visibility = View.VISIBLE

                    // Get unique color for this instance (sequential assignment ensures no duplicates)
                    val colorPair = NotificationAdapter.getInstanceColor(instanceLabel)
                    tvInstanceBadge.setBackgroundResource(colorPair.first)
                    tvInstanceBadge.setTextColor(android.graphics.Color.parseColor(colorPair.second))
                } else {
                    tvInstanceBadge.visibility = View.GONE
                }

                // Time - Format: "10 Ene • 02:55"
                tvTime.text = formatDateTime(notification.receivedAt)

                // Payer Name
                tvPayerName.text = notification.payerName ?: "Usuario"

                // Amount
                notification.amount?.let { amount ->
                    val currency = notification.currency ?: "PEN"
                    val amountText = when (currency) {
                        "PEN" -> "S/${String.format("%.2f", amount)}"
                        "USD" -> "\$${String.format("%.2f", amount)}"
                        else -> "$currency ${String.format("%.2f", amount)}"
                    }
                    tvAmount.text = amountText
                    tvAmount.visibility = View.VISIBLE
                } ?: run {
                    tvAmount.visibility = View.GONE
                }

                // Status-based visibility
                when (notification.status) {
                    "validated" -> {
                        // Show validated row
                        llSecurityCodeRow.visibility = View.GONE
                        llValidatedRow.visibility = View.VISIBLE

                        // Show security code if available
                        if (!notification.securityCode.isNullOrEmpty()) {
                            tvSecurityCodeValidated.text = "Cód: ${notification.securityCode}"
                            tvSecurityCodeValidated.visibility = View.VISIBLE
                        } else {
                            tvSecurityCodeValidated.visibility = View.GONE
                        }

                        // Change card background to light green
                        cardRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FDF4"))
                    }
                    "pending" -> {
                        // Always show the row for pending status to allow validation
                        llSecurityCodeRow.visibility = View.VISIBLE
                        llValidatedRow.visibility = View.GONE

                        // Show security code if available, otherwise show spacer to push button right
                        if (!notification.securityCode.isNullOrEmpty()) {
                            llSecurityCode.visibility = View.VISIBLE
                            tvSecurityCode.text = notification.securityCode
                            spacer.visibility = View.GONE
                        } else {
                            llSecurityCode.visibility = View.GONE
                            spacer.visibility = View.VISIBLE
                        }
                        // Always show validate button for pending payments
                        btnValidate.visibility = View.VISIBLE

                        // Default white background
                        cardRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                    }
                    "inconsistent" -> {
                        // Show security code row if available (without validate button)
                        if (!notification.securityCode.isNullOrEmpty()) {
                            llSecurityCodeRow.visibility = View.VISIBLE
                            tvSecurityCode.text = notification.securityCode
                            btnValidate.visibility = View.GONE
                        } else {
                            llSecurityCodeRow.visibility = View.GONE
                        }
                        llValidatedRow.visibility = View.GONE

                        // Light red background for inconsistent
                        cardRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#FEF2F2"))
                    }
                    else -> {
                        llSecurityCodeRow.visibility = View.GONE
                        llValidatedRow.visibility = View.GONE
                        cardRoot.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                    }
                }

                // Click handlers
                cardRoot.setOnClickListener {
                    onItemClick(notification)
                }

                btnValidate.setOnClickListener {
                    onValidateClick(notification)
                }

                // Menu click handler - Show popup menu with all actions
                ivMenu.setOnClickListener { view ->
                    showPopupMenu(view, notification)
                }
            }
        }

        private fun showPopupMenu(view: View, notification: Notification) {
            val popup = android.widget.PopupMenu(view.context, view)
            popup.inflate(R.menu.menu_notification_actions)

            // Disable current status option
            when (notification.status) {
                "validated" -> popup.menu.findItem(R.id.action_mark_validated)?.isEnabled = false
                "inconsistent" -> popup.menu.findItem(R.id.action_mark_inconsistent)?.isEnabled = false
                "pending" -> popup.menu.findItem(R.id.action_mark_pending)?.isEnabled = false
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_mark_validated -> {
                        onValidateClick(notification)
                        true
                    }
                    R.id.action_mark_inconsistent -> {
                        onMarkInconsistent(notification)
                        true
                    }
                    R.id.action_mark_pending -> {
                        onMarkPending(notification)
                        true
                    }
                    R.id.action_view_details -> {
                        onItemClick(notification)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun formatDateTime(dateString: String): String {
            return try {
                // Parse ISO 8601 format: "2026-01-10T02:55:41.000000Z"
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                val date = inputFormat.parse(dateString) ?: return dateString

                // Format: "10 Ene • 02:55"
                val dayFormat = SimpleDateFormat("d", Locale.getDefault())
                val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                val day = dayFormat.format(date)
                val month = monthFormat.format(date).capitalize()
                val time = timeFormat.format(date)

                "$day $month • $time"
            } catch (e: Exception) {
                // Fallback: try to parse without milliseconds
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(dateString) ?: return dateString

                    val dayFormat = SimpleDateFormat("d", Locale.getDefault())
                    val monthFormat = SimpleDateFormat("MMM", Locale("es", "ES"))
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    val day = dayFormat.format(date)
                    val month = monthFormat.format(date).capitalize()
                    val time = timeFormat.format(date)

                    "$day $month • $time"
                } catch (e2: Exception) {
                    dateString
                }
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
