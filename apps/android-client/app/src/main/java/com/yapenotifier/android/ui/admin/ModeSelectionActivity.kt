package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yapenotifier.android.databinding.ActivityModeSelectionBinding
import com.yapenotifier.android.ui.LinkDeviceActivity
import com.yapenotifier.android.ui.LoginActivity

/**
 * Initial screen that allows users to choose between Admin mode or Capturer mode.
 */
class ModeSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupVersionInfo()
    }

    private fun setupClickListeners() {
        // Admin mode - navigate to admin login
        binding.cardAdmin.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
        }

        // Capturer mode - navigate to link device
        binding.cardCapturer.setOnClickListener {
            val intent = Intent(this, LinkDeviceActivity::class.java)
            startActivity(intent)
        }

        // Help link
        binding.tvHelp.setOnClickListener {
            // TODO: Open help/setup wizard
        }
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            binding.tvVersion.text = "Versión $versionName • Secure Connection"
        } catch (e: Exception) {
            binding.tvVersion.text = "Versión 1.0.0 • Secure Connection"
        }
    }
}

