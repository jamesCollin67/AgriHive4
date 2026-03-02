package com.example.agrihive.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        private const val PREFS_NAME = "AgriHiveUserPrefs"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_FARM = "farm"
        private const val KEY_LOCATION = "location"
        private const val KEY_PHOTO_URL = "photo_url"
        private const val KEY_UID = "uid"
    }

    fun saveUserData(
        firstName: String,
        lastName: String,
        email: String,
        farm: String = "",
        location: String = "",
        photoUrl: String = ""
    ) {
        prefs.edit().apply {
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_EMAIL, email)
            putString(KEY_FARM, farm)
            putString(KEY_LOCATION, location)
            putString(KEY_PHOTO_URL, photoUrl)
            firebaseAuth.currentUser?.uid?.let { putString(KEY_UID, it) }
            apply()
        }
    }

    fun getFirstName(): String = prefs.getString(KEY_FIRST_NAME, "") ?: ""

    fun getLastName(): String = prefs.getString(KEY_LAST_NAME, "") ?: ""

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun getFarm(): String = prefs.getString(KEY_FARM, "") ?: ""

    fun getLocation(): String = prefs.getString(KEY_LOCATION, "") ?: ""

    fun getPhotoUrl(): String = prefs.getString(KEY_PHOTO_URL, "") ?: ""

    fun getUid(): String = prefs.getString(KEY_UID, "") ?: ""

    fun hasUserData(): Boolean {
        return prefs.getString(KEY_FIRST_NAME, "")?.isNotEmpty() == true ||
               prefs.getString(KEY_LAST_NAME, "")?.isNotEmpty() == true
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }
}
