package com.yapenotifier.android.ui.admin

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.yapenotifier.android.databinding.ActivityAdminAddDeviceBinding
import com.yapenotifier.android.data.api.ApiService
import com.yapenotifier.android.data.api.RetrofitClient
import com.yapenotifier.android.data.model.LinkCodeGenerateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class AdminAddDeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminAddDeviceBinding
    private val apiService: ApiService = RetrofitClient.createApiService(this)
    private var pollingHandler: Handler? = null
    private var linkCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAddDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        generateLinkCode()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Connect Device"
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener {
            finish()
        }

        binding.btnCopyCode.setOnClickListener {
            linkCode?.let { code ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Link Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Código copiado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateLinkCode() {
        val deviceAlias = binding.etDeviceAlias.text.toString().takeIf { it.isNotBlank() }
            ?: "Yape Cashier 1"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.generateLinkCode(
                    LinkCodeGenerateRequest(deviceAlias)
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val linkCodeData = response.body()
                        if (linkCodeData != null) {
                            linkCode = linkCodeData.linkCode
                            displayLinkCode(linkCodeData.linkCode, linkCodeData.qrCodeData)
                            startPolling(linkCodeData.linkCode)
                        } else {
                            Toast.makeText(this@AdminAddDeviceActivity, "Error al generar código", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@AdminAddDeviceActivity, "Error ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdminAddDeviceActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayLinkCode(code: String, qrData: String) {
        // Format code as XXX - XXX
        val formattedCode = if (code.length >= 6) {
            "${code.take(3)} - ${code.takeLast(3)}"
        } else {
            code
        }
        binding.tvLinkCode.text = formattedCode

        // Generate QR code
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
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }

            binding.ivQRCode.setImageBitmap(bitmap)
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
                        // Device linked successfully
                        stopPolling()
                        showSuccessMessage()
                    }
                }
            } catch (e: Exception) {
                // Continue polling
            }
        }
    }

    private fun stopPolling() {
        pollingHandler?.removeCallbacksAndMessages(null)
        pollingHandler = null
    }

    private fun showSuccessMessage() {
        binding.tvStatus.text = "Dispositivo vinculado exitosamente"
        binding.progressBar.visibility = android.view.View.GONE
        Toast.makeText(this, "Dispositivo vinculado exitosamente", Toast.LENGTH_LONG).show()
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

