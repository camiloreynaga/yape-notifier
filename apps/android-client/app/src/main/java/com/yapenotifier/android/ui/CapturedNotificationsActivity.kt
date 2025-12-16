package com.yapenotifier.android.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.yapenotifier.android.databinding.ActivityCapturedNotificationsBinding
import com.yapenotifier.android.ui.adapter.CapturedNotificationsAdapter
import com.yapenotifier.android.ui.viewmodel.CapturedNotificationsViewModel

class CapturedNotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapturedNotificationsBinding
    private lateinit var viewModel: CapturedNotificationsViewModel
    private val adapter = CapturedNotificationsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CapturedNotificationsViewModel::class.java)

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
