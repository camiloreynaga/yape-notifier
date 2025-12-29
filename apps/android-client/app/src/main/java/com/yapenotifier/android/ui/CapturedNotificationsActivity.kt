package com.yapenotifier.android.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.yapenotifier.android.databinding.ActivityCapturedNotificationsBinding
import com.yapenotifier.android.ui.adapter.CapturedNotificationsAdapter
import com.yapenotifier.android.ui.viewmodel.CapturedNotificationsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CapturedNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapturedNotificationsBinding
    private val viewModel: CapturedNotificationsViewModel by viewModels()
    private val adapter = CapturedNotificationsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel inyectado automáticamente por Hilt

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnRetryFailed.setOnClickListener {
            viewModel.retryFailedNotifications()
            Toast.makeText(this, "Reintentando envío de notificaciones fallidas...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.allNotifications.observe(this) {
            adapter.submitList(it)
        }
    }
}
