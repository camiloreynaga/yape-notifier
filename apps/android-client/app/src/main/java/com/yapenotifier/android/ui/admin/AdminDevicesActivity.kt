package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.yapenotifier.android.databinding.ActivityAdminDevicesBinding
import com.yapenotifier.android.data.model.Device
import com.yapenotifier.android.ui.admin.adapter.DeviceAdapter
import com.yapenotifier.android.ui.admin.viewmodel.AdminDevicesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminDevicesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminDevicesBinding
    private val viewModel: AdminDevicesViewModel by viewModels()
    private lateinit var adapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel inyectado automáticamente por Hilt

        setupToolbar()
        setupBottomNavigation()
        setupFAB()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        setupObservers()
        loadDevices()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarTitle.text = "Dispositivos"
        binding.toolbarSubtitle.text = "Gestionar dispositivos vinculados"
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_devices
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_notifications -> {
                    val intent = Intent(this, AdminPanelActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_devices -> {
                    // Already on devices tab
                    true
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, AdminSettingsActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddDevice.setOnClickListener {
            val intent = Intent(this, AdminAddDeviceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupClickListeners() {
        binding.btnRefresh.setOnClickListener {
            loadDevices()
            Toast.makeText(this, "Actualizando...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = DeviceAdapter(
            onEditClick = { device ->
                showEditDeviceDialog(device)
            },
            onDeleteClick = { device ->
                showDeleteDeviceDialog(device)
            },
            onExpandClick = { device ->
                adapter.notifyItemChanged(adapter.currentList.indexOf(device))
            }
        )

        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDevices()
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = state.loading

            if (state.loading && state.devices.isEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }

            adapter.submitList(state.devices)

            if (state.devices.isEmpty() && !state.loading) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.rvDevices.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.rvDevices.visibility = View.VISIBLE
            }

            state.error?.let { error ->
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadDevices() {
        viewModel.loadDevices()
    }

    private fun showEditDeviceDialog(device: Device) {
        val input = android.widget.EditText(this)
        input.setText(device.name)
        input.hint = "Nombre del dispositivo"

        AlertDialog.Builder(this)
            .setTitle("Editar Dispositivo")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.updateDeviceAlias(device.id, newName)
                } else {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteDeviceDialog(device: Device) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Dispositivo")
            .setMessage("¿Estás seguro de que deseas eliminar el dispositivo \"${device.name}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteDevice(device.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh devices when returning to this activity
        loadDevices()
    }
}

