package com.example.agrihive.register

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.data.UserSessionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.regex.Pattern

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val sessionManager: UserSessionManager = UserSessionManager(application)

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    private val _registerError = MutableLiveData<String?>()
    val registerError: LiveData<String?> = _registerError

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

        // VALIDATION
        if (firstName.isBlank() || lastName.isBlank() ||
            email.isBlank() || password.isBlank() || confirmPassword.isBlank()
        ) {
            _registerError.value = "All fields are required."
            return
        }

        if (!termsAccepted) {
            _registerError.value = "You must accept the terms and policy."
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _registerError.value = "Please enter a valid email."
            return
        }

        val passwordPattern =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!]).{8,}\$"

        if (!Pattern.compile(passwordPattern).matcher(password).matches()) {
            _registerError.value =
                "Password must contain 8+ chars, uppercase, lowercase, number, special character."
            return
        }

        if (password != confirmPassword) {
            _registerError.value = "Passwords do not match."
            return
        }

        // FIREBASE AUTH
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val uid = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener

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
                        .addOnSuccessListener {
                            // Save user data to local session for immediate profile display
                            sessionManager.saveUserData(
                                firstName = firstName,
                                lastName = lastName,
                                email = email
                            )

                            firebaseAuth.currentUser?.sendEmailVerification()

                            firebaseAuth.signOut()

                            _registerSuccess.value = true
                            _navigateToLogin.value = true
                        }
                        .addOnFailureListener {
                            _registerError.value =
                                it.message ?: "Failed to save user data."
                        }

                } else {
                    _registerError.value =
                        task.exception?.message ?: "Registration failed."
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
