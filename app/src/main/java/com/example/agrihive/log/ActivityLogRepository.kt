package com.example.agrihive.log

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ActivityLogRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private var prefs: SharedPreferences? = null
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

        @Volatile
        private var instance: ActivityLogRepository? = null

        fun init(context: Context) {
            prefs = context.applicationContext.getSharedPreferences("ActivityLogPrefs", Context.MODE_PRIVATE)
        }

        fun getInstance(): ActivityLogRepository {
            return instance ?: synchronized(this) {
                instance ?: ActivityLogRepository().also { instance = it }
            }
        }
    }

    private val sharedPrefs: SharedPreferences?
        get() = prefs

    /**
     * Save an activity log entry to LOCAL storage
     */
    fun saveActivityLog(
        type: LogType,
        title: String,
        description: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onFailure(Exception("User not logged in"))
            return
        }

        // Get user name for the log
        getUserName(uid) { userName ->
            // Create new log item
            val newLog = ActivityLogItem(
                id = System.currentTimeMillis().toString(),
                type = type,
                title = title,
                description = description,
                timestamp = Date(),
                userName = userName
            )

            // Save to local storage
            val currentLogs = getLocalLogs().toMutableList()
            currentLogs.add(0, newLog) // Add at beginning (newest first)
            saveLocalLogs(currentLogs, uid)

            // Also try to save to Firestore (optional - not required for display)
            val logData = hashMapOf(
                "uid" to uid,
                "type" to type.name,
                "title" to title,
                "description" to description,
                "timestamp" to Date(),
                "userName" to userName
            )

            firestore.collection("activity_logs")
                .add(logData)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { exception ->
                    // Still call success - local save worked
                    onSuccess()
                }
        }
    }

    /**
     * Get all activity logs for the current user
     * First checks local storage, then fetches from Firestore if needed
     */
    fun getActivityLogs(
        onSuccess: (List<ActivityLogItem>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        // First, try to get local logs
        val localLogs = getLocalLogs()

        if (localLogs.isNotEmpty()) {
            // If we have local logs, return them immediately
            onSuccess(localLogs)
            return
        }

        // If no local logs, fetch from Firestore and save to local storage
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onSuccess(emptyList())
            return
        }

        // Fetch from Firestore
        firestore.collection("activity_logs")
            .whereEqualTo("uid", uid)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val logs = mutableListOf<ActivityLogItem>()
                for (document in documents) {
                    val id = document.id
                    val typeStr = document.getString("type") ?: "USER_ACCOUNT"
                    val type = try {
                        LogType.valueOf(typeStr)
                    } catch (e: Exception) {
                        LogType.USER_ACCOUNT
                    }
                    val title = document.getString("title") ?: ""
                    val description = document.getString("description") ?: ""
                    val timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()
                    val userName = document.getString("userName")

                    logs.add(
                        ActivityLogItem(
                            id = id,
                            type = type,
                            title = title,
                            description = description,
                            timestamp = timestamp,
                            userName = userName
                        )
                    )
                }
                // Save to local storage for future use
                saveLocalLogs(logs, uid)
                onSuccess(logs)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Save logs to local SharedPreferences for persistence
     */
    private fun saveLocalLogs(logs: List<ActivityLogItem>, uid: String) {
        val jsonArray = JSONArray()
        for (log in logs) {
            val jsonObject = JSONObject().apply {
                put("id", log.id)
                put("type", log.type.name)
                put("title", log.title)
                put("description", log.description)
                put("timestamp", dateFormat.format(log.timestamp))
                put("userName", log.userName ?: "")
            }
            jsonArray.put(jsonObject)
        }
        sharedPrefs?.edit()?.apply {
            putString("cached_logs", jsonArray.toString())
            putString("logs_uid", uid)
            apply()
        }
    }

    /**
     * Get logs from local SharedPreferences
     */
    private fun getLocalLogs(): List<ActivityLogItem> {
        val logsJson = sharedPrefs?.getString("cached_logs", null) ?: return emptyList()
        val savedUid = sharedPrefs?.getString("logs_uid", "") ?: ""
        val currentUid = auth.currentUser?.uid

        // If user is logged in, only return logs for current user
        if (currentUid != null && savedUid != currentUid) {
            // Different user - return empty (will load from Firebase)
            return emptyList()
        }

        // If no user is currently logged in, return saved logs
        return try {
            val jsonArray = JSONArray(logsJson)
            val logs = mutableListOf<ActivityLogItem>()
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val typeStr = jsonObject.optString("type", "USER_ACCOUNT")
                val type = try {
                    LogType.valueOf(typeStr)
                } catch (e: Exception) {
                    LogType.USER_ACCOUNT
                }
                val timestampStr = jsonObject.optString("timestamp", "")
                val timestamp = try {
                    dateFormat.parse(timestampStr) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                logs.add(
                    ActivityLogItem(
                        id = jsonObject.optString("id", ""),
                        type = type,
                        title = jsonObject.optString("title", ""),
                        description = jsonObject.optString("description", ""),
                        timestamp = timestamp,
                        userName = jsonObject.optString("userName", "").ifEmpty { null }
                    )
                )
            }
            logs
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear locally cached logs (call on logout)
     */
    fun clearLocalLogs() {
        sharedPrefs?.edit()?.apply {
            remove("cached_logs")
            remove("logs_uid")
            apply()
        }
    }

    /**
     * Get user name from Firestore users collection
     */
    private fun getUserName(uid: String, onResult: (String) -> Unit) {
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val firstName = document.getString("firstName") ?: ""
                val lastName = document.getString("lastName") ?: ""
                val fullName = "$firstName $lastName".trim()
                onResult(fullName.ifEmpty { "Unknown User" })
            }
            .addOnFailureListener {
                onResult("Unknown User")
            }
    }
}
