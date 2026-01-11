package com.yapenotifier.android.ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.yapenotifier.android.R
import com.yapenotifier.android.databinding.ActivityAdminLoginBinding
import com.yapenotifier.android.ui.admin.viewmodel.AdminLoginViewModel
import com.yapenotifier.android.ui.CreateCommerceActivity
import com.yapenotifier.android.ui.RegisterActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AdminLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminLoginBinding
    private val viewModel: AdminLoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("AdminLoginActivity: onCreate iniciado")
        try {
            binding = ActivityAdminLoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Timber.d("AdminLoginActivity: ViewModel creado")
            setupObservers()
            setupClickListeners()
            setupTextWatchers()
            Timber.d("AdminLoginActivity: Setup completo")
        } catch (e: Exception) {
            Timber.e(e, "AdminLoginActivity: Error crítico en onCreate")
            Toast.makeText(this, "Error al iniciar: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            result?.let {
                if (it.success) {
                    Timber.d("Login exitoso, needsCommerceCreation: ${it.needsCommerceCreation}")
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    try {
                        if (it.needsCommerceCreation) {
                            Timber.d("Navegando a CreateCommerceActivity")
                            navigateToCreateCommerce()
                        } else {
                            Timber.d("Navegando a AdminPanelActivity")
                            navigateToAdminPanel()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error al navegar después del login")
                        Toast.makeText(this, "Error al navegar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Timber.w("Login fallido: ${it.message}")
                    Snackbar.make(
                        binding.root,
                        it.message ?: getString(R.string.login_error),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSignIn.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val emailOrPhone = binding.etEmailOrPhone.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (validateInput(emailOrPhone, password)) {
                viewModel.login(emailOrPhone, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            // TODO: Implement forgot password flow
            Toast.makeText(this, "Funcionalidad próximamente disponible", Toast.LENGTH_SHORT).show()
        }

        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("mode", "admin")
            startActivity(intent)
        }
    }

    private fun setupTextWatchers() {
        // Clear error when user starts typing
        binding.etEmailOrPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmailOrPhone.error = null
            }
            override fun afterTextChanged(s: Editable?) {
                // Real-time validation
                val input = s?.toString()?.trim() ?: ""
                if (input.isNotEmpty()) {
                    val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()
                    val isPhone = input.matches(Regex("^[+]?[0-9]{10,15}$"))
                    if (!isEmail && !isPhone) {
                        binding.tilEmailOrPhone.error = getString(R.string.invalid_email_or_phone)
                    }
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

    private fun validateInput(emailOrPhone: String, password: String): Boolean {
        var isValid = true

        if (emailOrPhone.isEmpty()) {
            binding.tilEmailOrPhone.error = getString(R.string.email_or_phone_required)
            isValid = false
        } else {
            // Allow email or phone format
            val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()
            val isPhone = emailOrPhone.matches(Regex("^[+]?[0-9]{10,15}$"))
            if (!isEmail && !isPhone) {
                binding.tilEmailOrPhone.error = getString(R.string.invalid_email_or_phone)
                isValid = false
            }
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

    private fun navigateToAdminPanel() {
        val intent = Intent(this, AdminPanelActivity::class.java)
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
}



