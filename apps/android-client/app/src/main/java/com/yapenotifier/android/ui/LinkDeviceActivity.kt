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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.yapenotifier.android.R
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.local.PreferencesManager
import com.yapenotifier.android.databinding.ActivityLinkDeviceBinding
import com.yapenotifier.android.ui.viewmodel.LinkDeviceViewModel
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import com.yapenotifier.android.util.WizardHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LinkDeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLinkDeviceBinding
    private val viewModel: LinkDeviceViewModel by viewModels()

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var apiService: ApiService

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startQRScanner()
        } else {
            Toast.makeText(
                this,
                R.string.camera_permission_needed,
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
            Toast.makeText(this, R.string.could_not_scan_code, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLinkDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Asegurar que existe un deviceUuid antes de verificar vinculación
        ensureDeviceUuid()

        // Verificar si el dispositivo ya está vinculado
        checkIfAlreadyLinked()

        setupObservers()
        setupClickListeners()
    }

    private fun ensureDeviceUuid() {
        lifecycleScope.launch {
            try {
                val deviceUuid = preferencesManager.deviceUuid.first()
                if (deviceUuid.isNullOrBlank()) {
                    // Si no existe UUID, es un error porque debería generarse en Application.onCreate
                    Timber.e("LinkDeviceActivity: Device UUID no encontrado. Debería generarse al iniciar la app.")
                    Toast.makeText(
                        this@LinkDeviceActivity,
                        R.string.error_uuid_not_found,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Timber.d("LinkDeviceActivity: Device UUID verificado: $deviceUuid")
                }
            } catch (e: Exception) {
                Timber.e(e, "LinkDeviceActivity: Error al verificar device UUID")
            }
        }
    }

    private fun checkIfAlreadyLinked() {
        lifecycleScope.launch {
            try {
                // Primero verificar localmente
                val deviceId = preferencesManager.deviceId.first()
                if (!deviceId.isNullOrBlank()) {
                    Timber.d("LinkDeviceActivity: DeviceId local encontrado ($deviceId), verificando en backend...")
                    // Verificar en backend que el dispositivo existe y está vinculado
                    try {
                        val deviceIdLong = deviceId.toLongOrNull()
                        if (deviceIdLong != null) {
                            val response = apiService.getDevice(deviceIdLong)
                            if (response.isSuccessful && response.body()?.device?.commerceId != null) {
                                Timber.d("LinkDeviceActivity: Dispositivo ya vinculado en backend, navegando a MainActivity")
                                Toast.makeText(this@LinkDeviceActivity, R.string.device_already_linked, Toast.LENGTH_SHORT).show()
                                navigateToMain()
                                return@launch
                            } else {
                                Timber.w("LinkDeviceActivity: DeviceId local existe pero no está vinculado en backend, limpiando...")
                                // Limpiar deviceId local si no está vinculado en backend
                                preferencesManager.saveDeviceId("")
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "LinkDeviceActivity: Error al verificar dispositivo en backend")
                        // Si hay error de red, asumir que está vinculado y continuar
                        navigateToMain()
                        return@launch
                    }
                }

                // Si no hay deviceId local, verificar por UUID en backend
                val deviceUuid = preferencesManager.deviceUuid.first()
                if (!deviceUuid.isNullOrBlank()) {
                    Timber.d("LinkDeviceActivity: Verificando dispositivo por UUID en backend...")
                    // Buscar en la lista de dispositivos del usuario
                    try {
                        val devicesResponse = apiService.getDevices(activeOnly = false)
                        if (devicesResponse.isSuccessful) {
                            val devices = devicesResponse.body()?.devices ?: emptyList()
                            val currentDevice = devices.find { it.uuid == deviceUuid }
                            if (currentDevice?.commerceId != null) {
                                // Dispositivo encontrado y vinculado, guardar deviceId
                                Timber.d("LinkDeviceActivity: Dispositivo encontrado en backend (id=${currentDevice.id}), guardando...")
                                preferencesManager.saveDeviceId(currentDevice.id.toString())
                                currentDevice.commerceId.let {
                                    preferencesManager.saveCommerceId(it.toString())
                                }
                                Toast.makeText(this@LinkDeviceActivity, R.string.device_already_linked, Toast.LENGTH_SHORT).show()
                                navigateToMain()
                                return@launch
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "LinkDeviceActivity: Error al buscar dispositivo por UUID")
                    }
                }

                Timber.d("LinkDeviceActivity: Dispositivo no vinculado, mostrando pantalla de vinculación")
            } catch (e: Exception) {
                Timber.e(e, "LinkDeviceActivity: Error al verificar si está vinculado")
            }
        }
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
                    binding.tvCommerceInfo.text = getString(R.string.commerce_info, state.commerce.name)
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
                Toast.makeText(this, R.string.enter_code_prompt, Toast.LENGTH_SHORT).show()
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
        options.setPrompt(getString(R.string.scan_qr_code_prompt))
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
            else -> getString(R.string.default_commerce_name)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_linking_title)
            .setMessage(getString(R.string.confirm_linking_message, commerceInfo))
            .setPositiveButton(R.string.link_button) { _, _ ->
                viewModel.linkDevice(code)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.device_linked_title)
            .setMessage(message)
            .setPositiveButton(R.string.continue_button) { _, _ ->
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
                    Timber.w("Device ID es null después de vincular, navegando a MainActivity")
                    navigateToMain() // Navigates and finishes
                    return@launch
                }

                Timber.d("Verificando instancias para deviceId: $deviceId")
                val response = apiService.getDeviceAppInstances(deviceId)
                if (response.isSuccessful) {
                    val instances = response.body()?.instances ?: emptyList()
                    Timber.d("Instancias encontradas: ${instances.size}")
                    val hasUnnamedInstances = instances.any { it.label.isNullOrBlank() }

                    if (hasUnnamedInstances) {
                        Timber.d("Hay instancias sin nombre, navegando a AppInstancesActivity")
                        val intent = Intent(this@LinkDeviceActivity, AppInstancesActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Timber.d("Todas las instancias tienen nombre, navegando a siguiente pantalla")
                        // Start device health worker after device linking
                        DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this@LinkDeviceActivity)
                        // Send immediate health check to update connection status
                        DeviceHealthWorkerHelper.sendImmediateHealthCheck(this@LinkDeviceActivity)
                        navigateToNextScreen()
                    }
                } else {
                    Timber.w("Error al obtener instancias (${response.code()}), navegando a MainActivity")
                    navigateToMain()
                }
            } catch (e: Exception) {
                Timber.e(e, "Excepción al verificar instancias, navegando a MainActivity")
                navigateToMain()
            }
        }
    }

    private fun navigateToMain() {
        try {
            Timber.d("Navegando a MainActivity después de vincular dispositivo")
            // Start device health worker after device linking
            DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this)
            // Send immediate health check to update connection status
            DeviceHealthWorkerHelper.sendImmediateHealthCheck(this)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Timber.e(e, "Error crítico al navegar a MainActivity")
            // Si MainActivity no existe o hay error, al menos no crashear
            Toast.makeText(this, R.string.device_already_linked, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun navigateToNextScreen() {
        lifecycleScope.launch {
            val navigated = WizardHelper.navigateToNextScreen(this@LinkDeviceActivity)
            if (!navigated) {
                navigateToMain()
            } else {
                finish()
            }
        }
    }
}
