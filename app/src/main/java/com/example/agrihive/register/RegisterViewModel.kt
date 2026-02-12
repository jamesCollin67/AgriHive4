package com.example.agrihive.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

class RegisterViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    // LiveData for success/error
    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _registerError = MutableLiveData<String?>()
    val registerError: LiveData<String?> = _registerError

    // Navigation
    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ) {
        // Validate input fields
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _registerError.value = "All fields are required."
            return
        }

        if (!termsAccepted) {
            _registerError.value = "You must accept the terms and policy."
            return
        }

        // Email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerError.value = "Please enter a valid email."
            return
        }

        // Password validation
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$"
        if (!Pattern.compile(passwordPattern).matcher(password).matches()) {
            _registerError.value =
                "Password must contain at least 8 characters, including uppercase, lowercase, number, and special character."
            return
        }

        if (password != confirmPassword) {
            _registerError.value = "Passwords do not match."
            return
        }

        // Firebase registration
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.currentUser!!.uid

                    val user = hashMapOf(
                        "uid" to uid,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "farm" to "",
                        "location" to "",
                        "apiaries" to 0
                    )

                    firestore.collection("users")
                        .document(uid)
                        .set(user)
                    firebaseAuth.currentUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            _registerSuccess.value = true
                        } else {
                            _registerError.value = "Failed to send verification email."
                        }
                    }
                    _navigateToLogin.value = true
                } else {
                    _registerError.value = task.exception?.message ?: "Registration failed."
                }
            }
    }

    fun doneError() {
        _registerError.value = null
    }

    fun doneNavigating() {
        _navigateToLogin.value = false
    }
}
