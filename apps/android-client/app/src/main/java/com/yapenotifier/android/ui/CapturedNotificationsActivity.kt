package com.yapenotifier.android.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.AppDatabase
import com.yapenotifier.android.data.local.CapturedNotificationRepository
import com.yapenotifier.android.data.local.entity.CapturedNotification
import com.yapenotifier.android.databinding.ActivityCapturedNotificationsBinding
import kotlinx.coroutines.launch

/**
 * Activity to display captured notifications for debugging purposes.
 * Shows the last 50 notifications that were captured by the NotificationListenerService.
 */
class CapturedNotificationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCapturedNotificationsBinding
    private lateinit var repository: CapturedNotificationRepository
    private lateinit var adapter: CapturedNotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = CapturedNotificationRepository(database.capturedNotificationDao())

        setupRecyclerView()
        loadNotifications()
    }

    private fun setupRecyclerView() {
        adapter = CapturedNotificationAdapter()
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        lifecycleScope.launch {
            val notifications = repository.getLatestNotificationsSync(50)
            if (notifications.isEmpty()) {
                binding.tvEmpty.visibility = android.view.View.VISIBLE
                binding.recyclerViewNotifications.visibility = android.view.View.GONE
            } else {
                binding.tvEmpty.visibility = android.view.View.GONE
                binding.recyclerViewNotifications.visibility = android.view.View.VISIBLE
                adapter.submitList(notifications)
            }
        }
    }
}

/**
 * RecyclerView adapter for displaying captured notifications.
 */
class CapturedNotificationAdapter : RecyclerView.Adapter<CapturedNotificationAdapter.ViewHolder>() {
    private var notifications = listOf<CapturedNotification>()

    fun submitList(newList: List<CapturedNotification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_captured_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount() = notifications.size

    class ViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvPackageName = itemView.findViewById<android.widget.TextView>(R.id.tvPackageName)
        private val tvTitle = itemView.findViewById<android.widget.TextView>(R.id.tvTitle)
        private val tvText = itemView.findViewById<android.widget.TextView>(R.id.tvText)
        private val tvTime = itemView.findViewById<android.widget.TextView>(R.id.tvTime)
        private val tvExtrasKeys = itemView.findViewById<android.widget.TextView>(R.id.tvExtrasKeys)

        fun bind(notification: CapturedNotification) {
            tvPackageName.text = notification.packageName
            tvTitle.text = notification.title ?: "(Sin título)"
            tvText.text = notification.text ?: "(Sin texto)"
            tvTime.text = "Post: ${notification.getFormattedPostTime()}\nCapturado: ${notification.getFormattedCapturedAt()}"
            tvExtrasKeys.text = "Extras: ${notification.extrasKeys ?: "ninguno"}"
        }
    }
}

