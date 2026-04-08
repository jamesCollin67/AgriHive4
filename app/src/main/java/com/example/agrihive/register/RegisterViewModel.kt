package com.example.agrihive.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agrihive.data.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

/**
 * ViewModel for User Registration
 * MVVM Architecture - handles user registration with Firebase Auth and Firestore
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager: UserSessionManager = UserSessionManager(application)

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _registerError = MutableLiveData<String?>()
    val registerError: LiveData<String?> = _registerError

    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    /**
     * Registers a new user with Firebase Auth
     * @param firstName User's first name
     * @param lastName User's last name
     * @param email User's email address
     * @param password User's password
     * @param confirmPassword Password confirmation
     * @param termsAccepted Whether terms and conditions are accepted
     */
    fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean,
        farmName: String,
        farmLocation: String
    ) {
        // Validation - Check empty fields
        if (firstName.isBlank() || lastName.isBlank() ||
            email.isBlank() || password.isBlank() || confirmPassword.isBlank() ||
            farmName.isBlank() || farmLocation.isBlank()
        ) {
            _registerError.value = "All fields are required."
            return
        }

        // Validation - Check terms acceptance
        if (!termsAccepted) {
            _registerError.value = "You must accept the terms and policy."
            return
        }

        // Validation - Check email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerError.value = "Please enter a valid email."
            return
        }

        // Validation - Check password strength
        val passwordPattern =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$"

        if (!Pattern.compile(passwordPattern).matcher(password).matches()) {
            _registerError.value =
                "Password must contain 8+ chars, uppercase, lowercase, number, special character."
            return
        }

        // Validation - Check password match
        if (password != confirmPassword) {
            _registerError.value = "Passwords do not match."
            return
        }

        // Set loading state
        _isLoading.value = true

        // Firebase Authentication - Create user with email and password
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    // Get the newly created user's UID
                    val uid = firebaseAuth.currentUser?.uid ?: run {
                        _isLoading.value = false
                        _registerError.value = "Failed to get user ID"
                        return@addOnCompleteListener
                    }

                    // Create user data map for Firestore
                    val user = hashMapOf(
                        "uid" to uid,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "farm" to farmName,
                        "farmName" to farmName,
                        "location" to farmLocation,
                        "farmLocation" to farmLocation,
                        "apiaries" to 0,
                        "createdAt" to System.currentTimeMillis()
                    )

                    // Save user data to Firestore
                    firestore.collection("users")
                        .document(uid)
                        .set(user)
                        .addOnSuccessListener {
                            // Save user session locally for immediate access
                            sessionManager.saveUserData(
                                firstName = firstName,
                                lastName = lastName,
                                email = email
                            )

                            // Send email verification
                            firebaseAuth.currentUser?.sendEmailVerification()

                            // Sign out user until they verify email
                            firebaseAuth.signOut()

                            _isLoading.value = false
                            _registerSuccess.value = true
                            _navigateToLogin.value = true
                        }
                        .addOnFailureListener { exception ->
                            _isLoading.value = false
                            _registerError.value = exception.message ?: "Failed to save user data."
                        }

                } else {
                    _isLoading.value = false
                    // Handle specific Firebase auth errors
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "This email is already registered"
                        task.exception?.message?.contains("weak password") == true ->
                            "Password is too weak"
                        else ->
                            task.exception?.message ?: "Registration failed"
                    }
                    _registerError.value = errorMessage
                }
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _registerError.value = exception.message ?: "An error occurred"
            }
    }

    /**
     * Clears the error state after handling
     */
    fun doneError() {
        _registerError.value = null
    }

    /**
     * Clears the navigation state after handling
     */
    fun doneNavigating() {
        _navigateToLogin.value = false
    }
}
