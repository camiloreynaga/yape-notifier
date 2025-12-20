package com.yapenotifier.android.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityLinkDeviceBinding
import com.yapenotifier.android.ui.viewmodel.LinkDeviceViewModel
import com.yapenotifier.android.util.WizardHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LinkDeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLinkDeviceBinding
    private lateinit var viewModel: LinkDeviceViewModel
    private lateinit var preferencesManager: PreferencesManager
    private val apiService = RetrofitClient.createApiService(this)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(
                this,
                "Se necesita permiso de cámara para escanear el código QR",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val qrScannerLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val scannedCode = result.contents.trim()
            binding.etCode.setText(scannedCode)
            validateCode(scannedCode)
        } else {
            Toast.makeText(this, "No se pudo escanear el código", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferencesManager = PreferencesManager(this)
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[LinkDeviceViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.validationState.observe(this) { state ->
            when (state) {
                is LinkDeviceViewModel.ValidationState.Idle -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvCommerceInfo.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = false
                    binding.tvError.visibility = android.view.View.GONE
                }
                is LinkDeviceViewModel.ValidationState.Validating -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.tvCommerceInfo.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = false
                    binding.tvError.visibility = android.view.View.GONE
                }
                is LinkDeviceViewModel.ValidationState.Valid -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvCommerceInfo.text = "Comercio: ${state.commerce.name}"
                    binding.tvCommerceInfo.visibility = android.view.View.VISIBLE
                    binding.btnLink.isEnabled = true
                    binding.tvError.visibility = android.view.View.GONE
                }
                is LinkDeviceViewModel.ValidationState.Invalid -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.tvCommerceInfo.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = false
                    binding.tvError.text = state.message
                    binding.tvError.visibility = android.view.View.VISIBLE
                }
            }
        }

        viewModel.linkState.observe(this) { state ->
            when (state) {
                is LinkDeviceViewModel.LinkState.Idle -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = true
                }
                is LinkDeviceViewModel.LinkState.Linking -> {
                    binding.progressBar.visibility = android.view.View.VISIBLE
                    binding.btnLink.isEnabled = false
                }
                is LinkDeviceViewModel.LinkState.Success -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = true
                    showSuccessDialog(state.message)
                }
                is LinkDeviceViewModel.LinkState.Error -> {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnLink.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnScanQR.setOnClickListener {
            checkCameraPermissionAndScan()
        }

        binding.etCode.setOnEditorActionListener { _, _, _ ->
            val code = binding.etCode.text.toString().trim()
            if (code.isNotEmpty()) {
                validateCode(code)
            }
            true
        }

        // Auto-validate when code reaches 8 characters
        binding.etCode.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val code = s?.toString()?.trim() ?: ""
                if (code.length == 8) {
                    validateCode(code)
                }
            }
        })

        binding.btnLink.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.isNotEmpty()) {
                showConfirmDialog(code)
            } else {
                Toast.makeText(this, "Por favor ingresa un código", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startQRScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startQRScanner() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Escanea el código QR de vinculación")
        options.setCameraId(0)
        options.setBeepEnabled(false)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(false)
        qrScannerLauncher.launch(options)
    }

    private fun validateCode(code: String) {
        if (code.isBlank()) {
            return
        }
        viewModel.validateCode(code)
    }

    private fun showConfirmDialog(code: String) {
        val commerceInfo = when (val state = viewModel.validationState.value) {
            is LinkDeviceViewModel.ValidationState.Valid -> state.commerce.name
            else -> "el comercio"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar Vinculación")
            .setMessage("¿Deseas vincular este dispositivo al comercio: $commerceInfo?")
            .setPositiveButton("Vincular") { _, _ ->
                viewModel.linkDevice(code)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Dispositivo Vinculado")
            .setMessage(message)
            .setPositiveButton("Continuar") { _, _ ->
                checkAppInstancesAndNavigate()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkAppInstancesAndNavigate() {
        lifecycleScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()?.toLongOrNull()
                if (deviceId == null) {
                    navigateToMain() // Navigates and finishes
                    return@launch
                }

                val response = apiService.getDeviceAppInstances(deviceId)
                if (response.isSuccessful) {
                    val instances = response.body()?.instances ?: emptyList()
                    val hasUnnamedInstances = instances.any {
                        val label = it.label
                        label.isNullOrBlank()
                    }

                    if (hasUnnamedInstances) {
                        val intent = Intent(this@LinkDeviceActivity, AppInstancesActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        checkWizardAndNavigate()
                    }
                } else {
                    navigateToMain()
                }
            } catch (e: Exception) {
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

    private fun checkWizardAndNavigate() {
        lifecycleScope.launch {
            val wizardShown = WizardHelper.checkAndShowWizard(this@LinkDeviceActivity)
            if (!wizardShown) {
                navigateToMain()
            } else {
                finish()
            }
        }
    }
}