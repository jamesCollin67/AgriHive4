package com.example.agrihive.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
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

        // Password visibility toggle - only when touching the eye icon
        binding.passwordInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Check if touch was on the drawable (right side)
                val drawableEnd = binding.passwordInput.compoundDrawables[2]
                if (drawableEnd != null) {
                    val touchX = event.rawX
                    val drawableRight = binding.passwordInput.right - binding.passwordInput.paddingRight
                    val drawableLeft = drawableRight - drawableEnd.bounds.width()
                    
                    if (touchX >= drawableLeft && touchX <= drawableRight) {
                        togglePasswordVisibility()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
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
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("FROM_LOGIN", true)
                startActivity(intent)
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

        // Facebook login button
        binding.facebook.setOnClickListener {
            openUrl("https://www.facebook.com")
        }

        // Google login button
        binding.google.setOnClickListener {
            openUrl("https://accounts.google.com")
        }
    }

    // Helper function to open URLs
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open browser", Toast.LENGTH_SHORT).show()
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
