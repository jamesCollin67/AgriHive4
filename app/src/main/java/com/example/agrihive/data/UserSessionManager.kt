package com.example.agrihive.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user session data in SharedPreferences for offline/cached display.
 * Used by Profile, Settings, and Register flows.
 */
class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "AgriHiveUserSession"
        private const val KEY_FIRST_NAME = "firstName"
        private const val KEY_LAST_NAME = "lastName"
        private const val KEY_EMAIL = "email"
        private const val KEY_FARM = "farm"
        private const val KEY_LOCATION = "location"
        private const val KEY_APIARIES = "apiaries"
        private const val KEY_UID = "uid"
    }

    fun saveUserData(
        firstName: String = "",
        lastName: String = "",
        email: String = "",
        farm: String = "",
        location: String = "",
        apiaries: Int = 0,
        uid: String = ""
    ) {
        prefs.edit()
            .putString(KEY_FIRST_NAME, firstName)
            .putString(KEY_LAST_NAME, lastName)
            .putString(KEY_EMAIL, email)
            .putString(KEY_FARM, farm)
            .putString(KEY_LOCATION, location)
            .putInt(KEY_APIARIES, apiaries)
            .putString(KEY_UID, uid)
            .apply()
    }

    fun getFirstName(): String = prefs.getString(KEY_FIRST_NAME, "") ?: ""
    fun getLastName(): String = prefs.getString(KEY_LAST_NAME, "") ?: ""
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getFarm(): String = prefs.getString(KEY_FARM, "") ?: ""
    fun getLocation(): String = prefs.getString(KEY_LOCATION, "") ?: ""
    fun getApiaries(): Int = prefs.getInt(KEY_APIARIES, 0)
    fun getUid(): String = prefs.getString(KEY_UID, "") ?: ""

    fun hasUserData(): Boolean {
        return getEmail().isNotEmpty() || getFirstName().isNotEmpty()
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }
}
