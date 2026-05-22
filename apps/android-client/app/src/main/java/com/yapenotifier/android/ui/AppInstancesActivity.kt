package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.yapenotifier.android.R
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityAppInstancesBinding
import com.yapenotifier.android.ui.adapter.AppInstanceAdapter
import com.yapenotifier.android.ui.viewmodel.AppInstancesViewModel
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppInstancesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppInstancesBinding
    private val viewModel: AppInstancesViewModel by viewModels()

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private lateinit var adapter: AppInstanceAdapter

    private val labelChanges = mutableMapOf<Long, String>()
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppInstancesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ViewModel y dependencias inyectados automáticamente por Hilt

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadAppInstances()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AppInstanceAdapter { instanceId, label ->
            labelChanges[instanceId] = label
        }
        binding.rvAppInstances.layoutManager = LinearLayoutManager(this)
        binding.rvAppInstances.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // Update loading state
            binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE

            // Update instances list
            adapter.submitList(state.instances)

            // Show/hide empty state
            binding.tvEmpty.visibility = if (state.instances.isEmpty() && !state.loading) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Show multiple instances alert
            if (state.multipleInstancesDetected && state.multipleInstancesMessage != null) {
                // Show alert card or banner
                android.widget.TextView(this).apply {
                    text = state.multipleInstancesMessage
                    setBackgroundColor("#FFF3CD".toColorInt())
                    setTextColor("#856404".toColorInt())
                    setPadding(16, 12, 16, 12)
                }
                // You can add this to a card or alert in the layout
            }

            // Show error
            if (state.error != null) {
                binding.tvError.text = state.error
                binding.tvError.visibility = View.VISIBLE
                Toast.makeText(this, state.error, Toast.LENGTH_LONG).show()
            } else {
                binding.tvError.visibility = View.GONE
            }

            // Update save button state
            binding.btnSave.isEnabled = !state.loading && !state.saving && state.instances.isNotEmpty()
            binding.btnSave.text = if (state.saving) "Guardando..." else "Guardar Cambios"

            // Show save error
            if (state.saveError != null) {
                Toast.makeText(this, state.saveError, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            if (labelChanges.isNotEmpty()) {
                saveAllLabels()
            } else {
                Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAppInstances() {
        lifecycleScope.launch {
            val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
            if (deviceId != null) {
                viewModel.loadAppInstances(deviceId)
            } else {
                binding.tvError.text = getString(R.string.error_getting_device_id)
                binding.tvError.visibility = View.VISIBLE
            }
        }
    }

    private fun saveAllLabels() {
        if (labelChanges.isEmpty()) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSaving) {
            return
        }

        isSaving = true
        val changesToSave = labelChanges.mapKeys { it.key.toString() }
        labelChanges.clear()

        viewModel.saveAllLabels(changesToSave)

        // Observe save completion
        viewModel.uiState.observe(this) { state ->
            if (isSaving && !state.saving) {
                isSaving = false
                if (state.saveError == null) {
                    Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
                    // Start device health worker after saving app instances
                    DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this@AppInstancesActivity)
                    // Check if all instances have names now
                    lifecycleScope.launch {
                        checkAllInstancesNamedAndNavigate()
                    }
                }
            }
        }
    }

    private fun checkAllInstancesNamedAndNavigate() {
        lifecycleScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
                if (deviceId != null) {
                    val hasUnnamed = viewModel.hasUnnamedInstancesFromBackend(deviceId)
                    if (hasUnnamed) {
                        // Still have unnamed instances, show message
                        Toast.makeText(
                            this@AppInstancesActivity,
                            "Aún hay instancias sin nombre. Por favor, completa todas las instancias.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // All instances have names, navigate to MainActivity
                        val intent = Intent(this@AppInstancesActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // No device ID, navigate to MainActivity anyway
                    navigateToMain()
                }
            } catch (_: Exception) {
                // On error, navigate to MainActivity
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}