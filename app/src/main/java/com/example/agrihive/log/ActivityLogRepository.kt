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
     * Save an activity log entry to Firestore with user ID
     * Each user's logs are stored separately under their own UID
     * Optimized: Removed extra network call for username to speed up login
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

        // Create log data with user ID for proper isolation
        // Use "User" as default to avoid extra Firestore read for speed
        val logData = hashMapOf(
            "uid" to uid,  // This ensures logs are user-specific
            "type" to type.name,
            "title" to title,
            "description" to description,
            "timestamp" to Date(),
            "userName" to "User"
        )

        // Save to Firestore under user's personal collection for better isolation
        firestore.collection("users")
            .document(uid)
            .collection("activity_logs")
            .add(logData)
            .addOnSuccessListener {
                // Fire and forget - don't wait for global log
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Get all activity logs for the current user
     * Always fetches from Firestore to ensure latest data per user
     */
    fun getActivityLogs(
        onSuccess: (List<ActivityLogItem>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid
        
        if (uid == null) {
            onSuccess(emptyList())
            return
        }

        // Always fetch from Firestore - user's personal collection for better isolation
        firestore.collection("users")
            .document(uid)
            .collection("activity_logs")
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
                
                // Also save to local cache for offline access
                saveLocalLogs(logs, uid)
                onSuccess(logs)
            }
            .addOnFailureListener { exception ->
                // Try to get from local cache if Firestore fails
                val localLogs = getLocalLogs(uid)
                if (localLogs.isNotEmpty()) {
                    onSuccess(localLogs)
                } else {
                    onFailure(exception)
                }
            }
    }

    /**
     * Get activity logs from global collection (fallback)
     * Only returns logs for the current user
     */
    fun getActivityLogsFromGlobal(
        onSuccess: (List<ActivityLogItem>) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid
        
        if (uid == null) {
            onSuccess(emptyList())
            return
        }

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
                onSuccess(logs)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    /**
     * Save logs to local SharedPreferences for offline access
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
     * Only returns logs for the specified user
     */
    private fun getLocalLogs(uid: String): List<ActivityLogItem> {
        val savedUid = sharedPrefs?.getString("logs_uid", "") ?: ""
        
        // Only return logs if they belong to the current user
        if (savedUid != uid) {
            return emptyList()
        }
        
        val logsJson = sharedPrefs?.getString("cached_logs", null) ?: return emptyList()

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
     * This ensures no data leaks between users
     */
    fun clearLocalLogs() {
        sharedPrefs?.edit()?.apply {
            remove("cached_logs")
            remove("logs_uid")
            apply()
        }
    }

    /**
     * Clear all logs for a specific user (from both local and Firestore)
     */
    fun clearUserLogs(uid: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit = {}) {
        // Clear local cache
        clearLocalLogs()
        
        // Clear from Firestore user's collection
        firestore.collection("users")
            .document(uid)
            .collection("activity_logs")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

}
