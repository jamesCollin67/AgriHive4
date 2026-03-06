package com.example.agrihive.forgot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityForgotPasswordBinding
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.example.agrihive.login.LoginActivity
import com.example.agrihive.utils.NetworkUtils

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BACK ARROW CLICK -> navigate to login
        binding.backArrow.setOnClickListener {
            navigateToLogin()
        }

        // SEND PASSWORD RESET LINK
        binding.sendVerification.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            
            // Check internet connection first
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
            } else {
                viewModel.sendVerificationCode(email)
            }
        }

        // OBSERVE ERRORS
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneError()
            }
        }

        // OBSERVE SUCCESS
        viewModel.successMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneSuccess()
                // Log the password reset request
                val email = binding.emailInput.text.toString()
                activityLogViewModel.addLog(LogType.USER_ACCOUNT, "Password reset requested for: $email")
                // Navigate back to login after successful send
                navigateToLogin()
            }
        }

        // OBSERVE LOADING STATE
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.sendVerification.isEnabled = !isLoading
            binding.emailInput.isEnabled = !isLoading
        }

        // Handle system back gesture using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToLogin()
            }
        })
    }

    // Centralized function to navigate to login
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        viewModel.doneNavigating()
    }
}
