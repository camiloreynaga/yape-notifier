package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.yapenotifier.android.databinding.ActivityAdminDevicesBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.Device
import kotlinx.coroutines.launch

class AdminDevicesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDevicesBinding
    private val apiService: ApiService = RetrofitClient.createApiService(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupFAB()
        loadDevices()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Dispositivos"
    }

    private fun setupFAB() {
        binding.fabAddDevice.setOnClickListener {
            val intent = Intent(this, AdminAddDeviceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadDevices() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val response = apiService.getDevices()

                if (response.isSuccessful) {
                    val devicesResponse = response.body()
                    val devices = devicesResponse?.devices ?: emptyList()
                    displayDevices(devices)
                } else {
                    Toast.makeText(this@AdminDevicesActivity, "Error al cargar dispositivos", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminDevicesActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayDevices(devices: List<Device>) {
        if (devices.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvDevices.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvDevices.visibility = View.VISIBLE
            // TODO: Setup RecyclerView adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

