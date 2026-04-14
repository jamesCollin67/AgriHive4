package com.example.agrihive.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agrihive.data.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager = UserSessionManager(application)

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
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != null) {
                        fetchAndSaveUserData(uid)
                    } else {
                        _isLoading.value = false
                        _loginSuccess.value = true
                        _navigateToDashboard.value = true
                    }
                } else {
                    _isLoading.value = false
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

    private fun fetchAndSaveUserData(uid: String) {
        // Enforce email verification — unverified users cannot access the app
        if (firebaseAuth.currentUser?.isEmailVerified == false) {
            firebaseAuth.signOut()
            _isLoading.value = false
            _loginError.value = "Please verify your email address before logging in. Check your inbox for a verification link."
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val farm = document.getString("farm") ?: ""
                    val location = document.getString("location") ?: ""
                    val apiaries = document.getLong("apiaries")?.toInt() ?: 0
                    
                    sessionManager.saveUserData(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        farm = farm,
                        location = location,
                        apiaries = apiaries,
                        uid = uid
                    )
                    
                    // Update FCM token on login
                    updateFcmToken(uid)
                }
                _isLoading.value = false
                _loginSuccess.value = true
                _navigateToDashboard.value = true
            }
            .addOnFailureListener {
                _isLoading.value = false
                _loginSuccess.value = true // Still navigate to dashboard even if fetch fails
                _navigateToDashboard.value = true
            }
    }

    private fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                firestore.collection("users").document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token updated on login: $token")
                    }
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
