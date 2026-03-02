package com.example.agrihive.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // LiveData for loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData for login result
    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _loginError = MutableLiveData<String?>()
    val loginError: LiveData<String?> = _loginError

    // Navigation
    private val _navigateToDashboard = MutableLiveData<Boolean>()
    val navigateToDashboard: LiveData<Boolean> = _navigateToDashboard

    private val _navigateToRegister = MutableLiveData<Boolean>()
    val navigateToRegister: LiveData<Boolean> = _navigateToRegister

    private val _navigateToForgotPassword = MutableLiveData<Boolean>()
    val navigateToForgotPassword: LiveData<Boolean> = _navigateToForgotPassword

    // Login function
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginError.value = "Email and password cannot be empty."
            return
        }

        // Show loading indicator
        _isLoading.value = true

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                // Hide loading indicator
                _isLoading.value = false
                
                if (task.isSuccessful) {
                    _loginSuccess.value = true
                    _navigateToDashboard.value = true
                } else {
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                        else -> exception?.message ?: "Login failed."
                    }
                    _loginError.value = errorMessage
                }
            }
    }

    // Navigation handlers
    fun onSignUpClicked() {
        _navigateToRegister.value = true
    }

    fun doneNavigatingRegister() {
        _navigateToRegister.value = false
    }

    fun onForgotPasswordClicked() {
        _navigateToForgotPassword.value = true
    }

    fun doneNavigatingForgotPassword() {
        _navigateToForgotPassword.value = false
    }

    // Reset error after showing
    fun doneError() {
        _loginError.value = null
    }

    fun doneNavigating() {
        _navigateToDashboard.value = false
    }
}
