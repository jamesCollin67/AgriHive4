package com.example.agrihive.forgot

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * ViewModel for Forgot Password functionality
 * MVVM Architecture - handles password reset link request via Firebase Auth
 */
class ForgotPasswordViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _resetSuccess = MutableLiveData<Boolean>()
    val resetSuccess: LiveData<Boolean> = _resetSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Sends a password reset link to the user's email
     * @param email User's email address
     */
    fun sendResetLink(email: String) {
        // Validate email
        if (email.isBlank()) {
            _errorMessage.value = "Please enter your email address"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email address"
            return
        }

        _isLoading.value = true

        // Send password reset email via Firebase
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _resetSuccess.value = true
                } else {
                    // Handle specific Firebase auth errors
                    val errorMsg = when (task.exception?.message) {
                        null -> "Failed to send reset link"
                        else -> task.exception?.message
                    }
                    _errorMessage.value = errorMsg
                }
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _errorMessage.value = exception.message ?: "An error occurred"
            }
    }

    /**
     * Clears the error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Resets the success state
     */
    fun resetSuccessState() {
        _resetSuccess.value = false
    }
}
