package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.yapenotifier.android.databinding.ActivityAppInstancesBinding
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.ui.adapter.AppInstanceAdapter
import com.yapenotifier.android.ui.viewmodel.AppInstancesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppInstancesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppInstancesBinding
    private lateinit var viewModel: AppInstancesViewModel
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adapter: AppInstanceAdapter

    private val labelChanges = mutableMapOf<Long, String>()
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppInstancesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[AppInstancesViewModel::class.java]

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
        runBlocking {
            val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
            if (deviceId != null) {
                viewModel.loadAppInstances(deviceId)
            } else {
                binding.tvError.text = "No se pudo obtener el ID del dispositivo"
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
        val changesToSave = labelChanges.toMap()
        labelChanges.clear()
        
        viewModel.saveAllLabels(changesToSave)
        
        // Observe save completion
        viewModel.uiState.observe(this) { state ->
            if (isSaving && !state.saving) {
                isSaving = false
                if (state.saveError == null) {
                    Toast.makeText(this, "Cambios guardados exitosamente", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity after successful save
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    companion object {
        fun shouldShowAppInstances(hasUnnamedInstances: Boolean): Boolean {
            return hasUnnamedInstances
        }
    }
}

