package com.yapenotifier.android.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.yapenotifier.android.R
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.model.LinkCodeGenerateRequest
import com.yapenotifier.android.databinding.ActivityAdminAddDeviceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class AdminAddDeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminAddDeviceBinding
    
    @Inject
    lateinit var apiService: ApiService
    private var pollingHandler: Handler? = null
    private var linkCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("AdminAddDeviceActivity: onCreate iniciado")
        try {
            binding = ActivityAdminAddDeviceBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupClickListeners()
            generateLinkCode()
            Timber.d("AdminAddDeviceDActivity: setup completo")
        } catch (e: Exception) {
            Timber.e(e, "AdminAddDeviceActivity: Error crítico en onCreate")
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnCopyCode.setOnClickListener {
            linkCode?.let { code ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Link Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateLinkCode() {
        val deviceAlias = binding.etDeviceAlias.text.toString().takeIf { it.isNotBlank() }
            ?: "Yape Cashier 1"

        Timber.d("AdminAddDeviceActivity: Generando código de vinculación con alias: $deviceAlias")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.generateLinkCode(
                    LinkCodeGenerateRequest(deviceAlias)
                )

                withContext(Dispatchers.Main) {
                    Timber.d("AdminAddDeviceActivity: Respuesta de generateLinkCode - isSuccessful=${response.isSuccessful}, code=${response.code()}")
                    if (response.isSuccessful) {
                        val linkCodeData = response.body()
                        if (linkCodeData != null) {
                            Timber.d("AdminAddDeviceActivity: Código generado exitosamente: ${linkCodeData.linkCode}")
                            linkCode = linkCodeData.linkCode
                            displayLinkCode(linkCodeData.linkCode, linkCodeData.qrCodeData)
                            startPolling(linkCodeData.linkCode)
                        } else {
                            Timber.e("AdminAddDeviceActivity: Respuesta vacía al generar código")
                            Toast.makeText(this@AdminAddDeviceActivity, "Error al generar código: respuesta vacía", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Timber.e("AdminAddDeviceActivity: Error al generar código - code=${response.code()}, body=$errorBody")
                        Toast.makeText(this@AdminAddDeviceActivity, "Error ${response.code()}: ${errorBody ?: "Sin detalles"}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "AdminAddDeviceActivity: Excepción al generar código")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminAddDeviceActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayLinkCode(code: String, qrData: String) {
        val formattedCode = when {
            code.length == 8 -> {
                "${code.take(4)} - ${code.takeLast(4)}"
            }
            code.length >= 6 -> {
                "${code.take(3)} - ${code.takeLast(3)}"
            }
            else -> {
                code
            }
        }
        binding.tvLinkCode.text = formattedCode

        generateQRCode(qrData)
    }

    private fun generateQRCode(data: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                data,
                BarcodeFormat.QR_CODE,
                512,
                512,
                mapOf(EncodeHintType.MARGIN to 1)
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Toast.makeText(this, "Error al generar QR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startPolling(code: String) {
        pollingHandler = Handler(Looper.getMainLooper())
        val pollingRunnable = object : Runnable {
            override fun run() {
                checkLinkStatus(code)
                pollingHandler?.postDelayed(this, 2000) // Poll every 2 seconds
            }
        }
        pollingHandler?.post(pollingRunnable)
    }

    private fun checkLinkStatus(code: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.validateLinkCode(code)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.valid == true) {
                        stopPolling()
                        showSuccessMessage()
                    }
                }
            } catch (_: Exception) {
                // Continue polling
            }
        }
    }

    private fun stopPolling() {
        pollingHandler?.removeCallbacksAndMessages(null)
        pollingHandler = null
    }

    private fun showSuccessMessage() {
        binding.tvStatus.text = getString(R.string.device_linked_successfully)
        binding.progressBar.visibility = android.view.View.GONE
        Toast.makeText(this, getString(R.string.device_linked_successfully), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
