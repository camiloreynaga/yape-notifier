package com.yapenotifier.android.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivityAdminNotificationDetailBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import kotlinx.coroutines.launch

class AdminNotificationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminNotificationDetailBinding
    private val apiService: ApiService = RetrofitClient.createApiService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadNotification()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detalle de Notificación"
    }

    private fun loadNotification() {
        val notificationId = intent.getLongExtra("notification_id", -1)
        if (notificationId == -1L) {
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val response = apiService.getNotification(notificationId)
                if (response.isSuccessful) {
                    val notification = response.body()
                    // TODO: Display notification details
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

