package com.yapenotifier.android.ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.yapenotifier.android.databinding.ActivityAdminSettingsBinding

class AdminSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configuración"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

