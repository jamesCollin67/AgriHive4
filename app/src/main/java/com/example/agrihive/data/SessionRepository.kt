package com.example.agrihive.data

import com.google.firebase.auth.FirebaseAuth

class SessionRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
