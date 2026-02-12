package com.example.agrihive.register

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityRegisterPageBinding
import com.example.agrihive.login.LoginActivity
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterPageBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sign up button
        binding.signup.setOnClickListener {
            val firstName = binding.firstName.text.toString().trim()
            val lastName = binding.lastName.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()
            val termsAccepted = binding.terms.isChecked

            viewModel.register(firstName, lastName, email, password, confirmPassword, termsAccepted)
        }

        // Observe error
        viewModel.registerError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.doneError()
            }
        }

        // Observe success
        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(
                    this,
                    "Registration successful! Please verify your email.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Navigate to LoginActivity
        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                viewModel.doneNavigating()
            }
        }

        // Back button click
        binding.back.setOnClickListener { navigateBackToLogin() }

        // System back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateBackToLogin()
            }
        })
    }

    private fun navigateBackToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
