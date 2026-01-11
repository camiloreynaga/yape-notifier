package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityLoginBinding
import com.yapenotifier.android.ui.viewmodel.LoginViewModel
import com.yapenotifier.android.util.DeviceHealthWorkerHelper
import com.yapenotifier.android.util.WizardHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            result?.let {
                if (it.success) {
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    if (it.needsCommerceCreation) {
                        navigateToCreateCommerce()
                    } else if (it.needsDeviceLinking) {
                        navigateToLinkDevice()
                    } else {
                        // Start device health worker after successful login
                        DeviceHealthWorkerHelper.scheduleDeviceHealthWorker(this@LoginActivity)
                        navigateToNextScreen()
                    }
                } else {
                    Snackbar.make(
                        binding.root,
                        it.message ?: getString(R.string.login_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupTextWatchers() {
        // Clear error when user starts typing
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.error = null
            }
            override fun afterTextChanged(s: Editable?) {
                // Real-time email validation (only show error when invalid and has content)
                val email = s?.toString()?.trim() ?: ""
                if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.tilEmail.error = getString(R.string.invalid_email)
                }
            }
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.email_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.invalid_email)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        }

        return isValid
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToCreateCommerce() {
        val intent = Intent(this, CreateCommerceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLinkDevice() {
        val intent = Intent(this, LinkDeviceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToNextScreen() {
        lifecycleScope.launch {
            val navigated = WizardHelper.navigateToNextScreen(this@LoginActivity)
            if (!navigated) {
                navigateToMain()
            } else {
                finish()
            }
        }
    }
}
