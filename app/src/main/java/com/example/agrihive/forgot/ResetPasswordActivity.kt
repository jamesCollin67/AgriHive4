package com.example.agrihive.forgot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityResetPasswordBinding
import com.example.agrihive.login.LoginActivity

/**
 * Reset Password Page — Enter verification code and new password.
 * (Spec: Onboarding - Forgot & Reset Password flow, screen 2)
 * MVVM Architecture with Firebase Auth integration
 */
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding
    private val viewModel: ResetPasswordViewModel by viewModels()

    companion object {
        const val EXTRA_EMAIL = "email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val email = intent.getStringExtra(EXTRA_EMAIL) ?: ""

        // Back button click
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Reset password button click
        binding.btnReset.setOnClickListener {
            val code = binding.etCode.text?.toString()?.trim() ?: ""
            val newPassword = binding.etNewPassword.text?.toString() ?: ""
            val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""
            viewModel.resetPassword(email, code, newPassword, confirmPassword)
        }

        // Observe ViewModel success state
        viewModel.success.observe(this) { success ->
            if (success == true) {
                Toast.makeText(this, "Password updated successfully! Please login.", Toast.LENGTH_LONG).show()
                viewModel.doneSuccess()
                // Navigate to login and clear back stack
                startActivity(Intent(this, LoginActivity::class.java).setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ))
                finish()
            }
        }

        // Observe ViewModel error state
        viewModel.error.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneError()
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnReset.isEnabled = !isLoading
            binding.etCode.isEnabled = !isLoading
            binding.etNewPassword.isEnabled = !isLoading
            binding.etConfirmPassword.isEnabled = !isLoading
        }
    }
}
