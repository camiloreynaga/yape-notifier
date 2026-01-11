package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.yapenotifier.android.R
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
        binding.toolbarTitle.text = getString(R.string.admin_devices_title)
        binding.toolbarSubtitle.text = getString(R.string.admin_devices_subtitle)
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

            // Show skeleton while loading and no data yet
            if (state.loading && state.devices.isEmpty()) {
                binding.skeletonContainer.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvDevices.visibility = View.GONE
            } else {
                binding.skeletonContainer.visibility = View.GONE
            }

            adapter.submitList(state.devices)

            // Show empty state when no data and not loading
            if (state.devices.isEmpty() && !state.loading) {
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.rvDevices.visibility = View.GONE
            } else if (state.devices.isNotEmpty()) {
                binding.emptyStateContainer.visibility = View.GONE
                binding.rvDevices.visibility = View.VISIBLE
            }

            // Show error with Snackbar
            state.error?.let { error ->
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry) { loadDevices() }
                    .show()
            }
        }

        // Setup empty state action button
        binding.btnEmptyAction.setOnClickListener {
            val intent = Intent(this, AdminAddDeviceActivity::class.java)
            startActivity(intent)
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
                    Snackbar.make(binding.root, R.string.device_updated, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, "El nombre no puede estar vacío", Snackbar.LENGTH_SHORT).show()
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
                Snackbar.make(binding.root, R.string.device_deleted, Snackbar.LENGTH_SHORT).show()
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
