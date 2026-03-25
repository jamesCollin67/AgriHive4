package com.example.agrihive.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityLoginBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.forgot.ForgotPasswordActivity
import com.example.agrihive.log.ActivityLogRepository
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.example.agrihive.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityLogRepository.init(applicationContext)

        binding.switchRemember.isChecked = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)
            .getBoolean("remember_me", false)

        binding.btnLogin.setOnClickListener {
            val email = binding.emailInput.text?.toString() ?: ""
            val password = binding.passwordInput.text?.toString() ?: ""
            viewModel.login(email, password)
        }

        binding.switchRemember.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences("AgriHivePrefs", MODE_PRIVATE).edit()
                .putBoolean("remember_me", isChecked).apply()
        }

        // Note: Password visibility toggle is now handled automatically by TextInputLayout
        // in the activity_login.xml using app:endIconMode="password_toggle"

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnLogin.isEnabled = !isLoading
            binding.emailInput.isEnabled = !isLoading
            binding.passwordInput.isEnabled = !isLoading
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
                // Log the login activity
                activityLogViewModel.addLog(LogType.USER_ACCOUNT, "User logged in")
                
                val intent = Intent(this, DashboardActivity::class.java)
                intent.putExtra("FROM_LOGIN", true)
                startActivity(intent)
                finish()
                viewModel.doneNavigating()
            }
        }

        // Navigate to RegisterActivity
        binding.tvRegisterLink.setOnClickListener {
            viewModel.onSignUpClicked()
        }
        viewModel.navigateToRegister.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, RegisterActivity::class.java))
                viewModel.doneNavigatingRegister()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            viewModel.onForgotPasswordClicked()
        }
        viewModel.navigateToForgotPassword.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
                viewModel.doneNavigatingForgotPassword()
            }
        }
    }
}
