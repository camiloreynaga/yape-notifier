package com.yapenotifier.android.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.data.local.db.AppDatabase
import com.yapenotifier.android.databinding.ActivityCapturedNotificationsBinding
import com.yapenotifier.android.ui.adapter.CapturedNotificationsAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CapturedNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapturedNotificationsBinding
    private val adapter = CapturedNotificationsAdapter()
    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeNotifications()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            db.capturedNotificationDao().getAllNotificationsFlow().collectLatest {
                adapter.submitList(it)
            }
        }
    }
}
