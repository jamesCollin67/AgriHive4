package com.example.agrihive.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityRegisterPageBinding
import com.example.agrihive.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPageBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // SIGN UP BUTTON
        binding.signup.setOnClickListener {
            viewModel.register(
                binding.firstName.text.toString().trim(),
                binding.lastName.text.toString().trim(),
                binding.email.text.toString().trim(),
                binding.password.text.toString(),
                binding.confirmPassword.text.toString(),
                binding.terms.isChecked
            )
        }

        // SUCCESS TOAST
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "Registered successfully! Please verify your email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // ERROR TOAST
        viewModel.registerError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.doneError()
            }
        }

        // NAVIGATE TO LOGIN
        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                viewModel.doneNavigating()
            }
        }

        // Manual SIGN IN click
        binding.signInText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.back.setOnClickListener {
            finish()
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
}
