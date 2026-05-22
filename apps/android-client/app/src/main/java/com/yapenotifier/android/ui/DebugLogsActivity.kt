package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.yapenotifier.android.databinding.ActivityDebugLogsBinding
import com.yapenotifier.android.util.FileLogger
import kotlinx.coroutines.launch

/**
 * Activity to view debug logs for diagnosing service disconnections.
 * Shows persistent logs that help identify whether MIUI/system or the app caused disconnection.
 */
class DebugLogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDebugLogsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDebugLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadLogs()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnRefresh.setOnClickListener {
            loadLogs()
            Toast.makeText(this, "Logs actualizados", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearLogs.setOnClickListener {
            showClearConfirmation()
        }

        binding.btnShareLogs.setOnClickListener {
            shareLogs()
        }
    }

    private fun loadLogs() {
        lifecycleScope.launch {
            val logs = FileLogger.readLogs()
            binding.tvLogs.text = logs

            // Scroll to top (most recent logs)
            binding.scrollView.post {
                binding.scrollView.fullScroll(android.view.View.FOCUS_UP)
            }
        }
    }

    private fun showClearConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Limpiar Logs")
            .setMessage("¿Estás seguro de que quieres eliminar todos los logs de debug?")
            .setPositiveButton("Eliminar") { _, _ ->
                FileLogger.clearLogs()
                loadLogs()
                Toast.makeText(this, "Logs eliminados", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareLogs() {
        val logFile = FileLogger.getLogFile()

        if (logFile == null || !logFile.exists()) {
            Toast.makeText(this, "No hay logs para compartir", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "YapeNotifier Debug Logs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Compartir logs"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
