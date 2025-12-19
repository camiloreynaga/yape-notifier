package com.yapenotifier.android.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.yapenotifier.android.databinding.ActivityLoginBinding
import com.yapenotifier.android.ui.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))[LoginViewModel::class.java]

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.loginResult.observe(this) { result ->
            result?.let {
                if (it.success) {
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    if (it.needsCommerceCreation) {
                        navigateToCreateCommerce()
                    } else {
                        navigateToMain()
                    }
                } else {
                    Toast.makeText(this, it.message ?: "Error al iniciar sesión", Toast.LENGTH_LONG).show()
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

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email requerido"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Contraseña requerida"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email inválido"
            return false
        }
        return true
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
}
