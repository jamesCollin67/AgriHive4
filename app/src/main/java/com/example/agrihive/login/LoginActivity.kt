package com.example.agrihive.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityLoginPageBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.forgot.ForgotPasswordActivity
import com.example.agrihive.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sign in button
        binding.btnSignIn.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            viewModel.login(email, password)
        }

        // Observe login success/error
        viewModel.loginSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loginError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneError()
            }
        }

        // Navigate to DashboardActivity
        viewModel.navigateToDashboard.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                viewModel.doneNavigating()
            }
        }

        // Navigate to RegisterActivity
        binding.signUp.setOnClickListener {
            viewModel.onSignUpClicked()
        }
        viewModel.navigateToRegister.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, RegisterActivity::class.java))
                viewModel.doneNavigatingRegister()
            }
        }

        // Navigate to ForgotPasswordActivity
        binding.forgotPassword.setOnClickListener {
            viewModel.onForgotPasswordClicked()
        }
        viewModel.navigateToForgotPassword.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
                viewModel.doneNavigatingForgotPassword()
            }
        }

        // Optional: back button
        binding.btnBack.setOnClickListener {
            finish() // go back to previous activity
        }
    }
}
