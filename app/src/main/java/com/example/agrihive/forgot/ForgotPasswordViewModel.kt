package com.example.agrihive.forgot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import java.util.regex.Pattern

class ForgotPasswordViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // LiveData for status messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Navigation LiveData
    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    // For simplicity, we store a fake OTP here (in real app use Firebase or your backend)
    private var generatedOtp: String? = null

    // Send verification code
    fun sendVerificationCode(email: String) {
        if (email.isBlank()) {
            _errorMessage.value = "Email cannot be empty."
            return
        }

        // Simple email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email."
            return
        }

        // Fake OTP generation (5-digit)
        generatedOtp = (10000..99999).random().toString()
        // TODO: Integrate Firebase Email / OTP API to send actual code
        _successMessage.value = "Verification code sent to $email"
    }

    // Confirm OTP
    fun confirmOtp(inputOtp: String) {
        if (inputOtp.length != 5) {
            _errorMessage.value = "Please enter the 5-digit code."
            return
        }

        if (generatedOtp == null) {
            _errorMessage.value = "Please request a verification code first."
            return
        }

        if (inputOtp != generatedOtp) {
            _errorMessage.value = "Invalid verification code."
            return
        }

        // OTP is correct
        _successMessage.value = "OTP verified! You can reset your password now."
    }

    fun doneError() {
        _errorMessage.value = null
    }

    fun doneSuccess() {
        _successMessage.value = null
    }

    fun navigateBackToLogin() {
        _navigateToLogin.value = true
    }

    fun doneNavigating() {
        _navigateToLogin.value = false
    }
}
