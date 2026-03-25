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

    /**
     * Saves user data to SharedPreferences.
     * Only non-null parameters will be updated, preserving existing data for others.
     */
    fun saveUserData(
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        farm: String? = null,
        location: String? = null,
        apiaries: Int? = null,
        uid: String? = null
    ) {
        val editor = prefs.edit()
        firstName?.let { editor.putString(KEY_FIRST_NAME, it) }
        lastName?.let { editor.putString(KEY_LAST_NAME, it) }
        email?.let { editor.putString(KEY_EMAIL, it) }
        farm?.let { editor.putString(KEY_FARM, it) }
        location?.let { editor.putString(KEY_LOCATION, it) }
        apiaries?.let { editor.putInt(KEY_APIARIES, it) }
        uid?.let { editor.putString(KEY_UID, it) }
        editor.apply()
    }

    fun getFirstName(): String = prefs.getString(KEY_FIRST_NAME, "") ?: ""
    fun getLastName(): String = prefs.getString(KEY_LAST_NAME, "") ?: ""
    fun getEmail(): String = prefs.getString(KEY_EMAIL, "") ?: ""
    fun getFarm(): String = prefs.getString(KEY_FARM, "") ?: ""
    fun getLocation(): String = prefs.getString(KEY_LOCATION, "") ?: ""
    fun getApiaries(): Int = prefs.getInt(KEY_APIARIES, 0)
    fun getUid(): String = prefs.getString(KEY_UID, "") ?: ""

    fun hasUserData(): Boolean {
        // UID and Email are the most reliable indicators of a valid session
        return getUid().isNotEmpty() && getEmail().isNotEmpty()
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }
}
