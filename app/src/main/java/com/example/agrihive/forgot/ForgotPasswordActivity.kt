package com.example.agrihive.forgot

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityForgotPasswordBinding
import com.example.agrihive.login.LoginActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BACK ARROW CLICK -> navigate to login
        binding.backArrow.setOnClickListener {
            navigateToLogin()
        }

        // SEND VERIFICATION
        binding.sendVerification.setOnClickListener {
            val email = binding.emailInput.text.toString().trim()
            viewModel.sendVerificationCode(email)
        }

        // CONFIRM OTP
        binding.confirmButton.setOnClickListener {
            val otp = getOtpFromFields()
            viewModel.confirmOtp(otp)
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
            }
        }

        // NAVIGATE TO LOGIN
        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) navigateToLogin()
        }

        // Handle system back gesture using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateToLogin()
            }
        })
    }

    // Helper to read OTP from EditText boxes
    private fun getOtpFromFields(): String {
        val codeLayout = binding.codeLayout as LinearLayout
        var otp = ""
        for (i in 0 until codeLayout.childCount) {
            val editText = codeLayout.getChildAt(i) as EditText
            otp += editText.text.toString()
        }
        return otp
    }

    // Centralized function to navigate to login
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        viewModel.doneNavigating()
    }
}
