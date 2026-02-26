package com.example.agrihive.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityLoginPageBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.forgot.ForgotPasswordActivity
import com.example.agrihive.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPageBinding
    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false

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

        // Set initial password toggle drawable
        binding.passwordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)

        // Password visibility toggle
        binding.passwordInput.setOnClickListener {
            togglePasswordVisibility()
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

    private fun togglePasswordVisibility() {
        val editText = binding.passwordInput
        if (isPasswordVisible) {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0)
            isPasswordVisible = false
        } else {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_off, 0)
            isPasswordVisible = true
        }
        // Move cursor to end
        editText.setSelection(editText.text?.length ?: 0)
    }
}
