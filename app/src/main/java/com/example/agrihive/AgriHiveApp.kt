package com.example.agrihive

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.agrihive.log.ActivityLogRepository
import com.google.firebase.FirebaseApp

class AgriHiveApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize ActivityLogRepository with context (needed for SharedPreferences cache)
        ActivityLogRepository.init(this)

        // Create all notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Hive sensor alerts (temperature, moisture, weight)
            manager.createNotificationChannel(
                NotificationChannel(
                    "HIVE_ALERTS",
                    "Hive Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts for hive temperature, moisture, and weight thresholds"
                }
            )

            // Admin reply notifications
            manager.createNotificationChannel(
                NotificationChannel(
                    "ADMIN_REPLIES",
                    "Admin Replies",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications when the admin replies to your reports"
                }
            )

            // Weather / rain alerts
            manager.createNotificationChannel(
                NotificationChannel(
                    "WEATHER_ALERTS",
                    "Weather Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Rain and weather forecast alerts for your farm area"
                }
            )

            // General system notifications
            manager.createNotificationChannel(
                NotificationChannel(
                    "SYSTEM",
                    "System Notifications",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "General app notifications"
                }
            )
        }
    }
}
