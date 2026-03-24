package com.example.agrihive.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityRegisterBinding
import com.example.agrihive.login.LoginActivity

/**
 * Register Activity - Create new user account
 * MVVM Architecture with Firebase Auth backend
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button click
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Sign Up button click
        binding.btnRegister.setOnClickListener {
            viewModel.register(
                binding.firstName.text.toString().trim(),
                binding.lastName.text.toString().trim(),
                binding.email.text.toString().trim(),
                binding.password.text.toString(),
                binding.confirmPassword.text.toString(),
                binding.terms.isChecked
            )
        }

        // Login link click
        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Observe success state
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "Registered successfully! Please verify your email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Observe error state
        viewModel.registerError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.doneError()
            }
        }

        // Observe navigation state
        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                viewModel.doneNavigating()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.firstName.isEnabled = !isLoading
            binding.lastName.isEnabled = !isLoading
            binding.email.isEnabled = !isLoading
            binding.password.isEnabled = !isLoading
            binding.confirmPassword.isEnabled = !isLoading
            binding.terms.isEnabled = !isLoading
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
