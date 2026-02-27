package com.example.agrihive.forgot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

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

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Send password reset link using Firebase Auth
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

        // Show loading
        _isLoading.value = true

        // Send password reset email via Firebase
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    // Firebase sends the password reset email automatically
                    _successMessage.value = "Password reset link sent to $email. Please check your email inbox (and spam folder)."
                } else {
                    // Handle error - could be invalid email, user not found, etc.
                    val errorMsg = task.exception?.message ?: "Failed to send password reset email. Please try again."
                    _errorMessage.value = errorMsg
                }
            }
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
