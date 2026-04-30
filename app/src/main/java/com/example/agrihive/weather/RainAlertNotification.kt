package com.example.agrihive.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.notification.NotificationItem
import com.example.agrihive.notification.NotificationRepository
import com.example.agrihive.notification.NotificationType

/**
 * Notification helper for rain alerts
 * Notifies beekeepers when rain is expected so they can prepare white sugar water
 */
object RainAlertNotification {

    private const val CHANNEL_ID = "rain_alerts"
    private const val CHANNEL_NAME = "Rain Alerts"
    private const val CHANNEL_DESCRIPTION = "Notifications when rain is expected in your area"
    
    private const val NOTIFICATION_ID_RAIN = 1001
    private const val NOTIFICATION_ID_MODERATE_RAIN = 1002
    private const val NOTIFICATION_ID_HEAVY_RAIN = 1003
    private const val NOTIFICATION_ID_SEVERE = 1004
    private const val NOTIFICATION_ID_ADMIN_REPLY = 2001

    /**
     * Create notification channel (required for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = CHANNEL_NAME
            val descriptionText = CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show rain notification based on warning level
     */
    fun showRainNotification(context: Context, warning: RainfallWarning) {
        createNotificationChannel(context)

        val (title, message, notificationId) = when (warning) {
            RainfallWarning.NONE -> Triple(
                "All Clear - No Rain Expected",
                "Weather conditions are good for your bees today.",
                NOTIFICATION_ID_RAIN
            )
            RainfallWarning.LOW -> Triple(
                "Light Rain Expected - Prepare White Sugar Water",
                "Light rain may occur. Consider preparing white sugar water (1:1 ratio) for your bees just in case.",
                NOTIFICATION_ID_RAIN
            )
            RainfallWarning.MODERATE -> Triple(
                "⚠️ MODERATE RAIN ALERT - Prepare White Sugar Water Now!",
                "Moderate rain expected. Your bees may not be able to forage. URGENT: Mix 1 cup white sugar with 1 cup water and prepare for feeding!",
                NOTIFICATION_ID_MODERATE_RAIN
            )
            RainfallWarning.HIGH -> Triple(
                "🔴 HEAVY RAIN ALERT - White Sugar Water Needed!",
                "Heavy rain is expected! Your bees will stay in the hive and need food. IMMEDIATELY prepare white sugar water (1:1 ratio) for emergency feeding!",
                NOTIFICATION_ID_HEAVY_RAIN
            )
            RainfallWarning.SEVERE -> Triple(
                "🚨 SEVERE WEATHER ALERT - Take Immediate Action!",
                "Severe weather conditions! Prepare white sugar water immediately and protect your hives from damage!",
                NOTIFICATION_ID_SEVERE
            )
        }

        // Save notification to repository
        val repository = NotificationRepository(context)
        repository.addNotification(title, message, NotificationType.RAIN_ALERT)

        sendNotification(context, title, message, notificationId)
    }

    /**
     * Show notification for admin reply
     */
    fun showAdminReplyNotification(context: Context, reply: String) {
        createNotificationChannel(context)

        val title = "Admin Replied to Your Report"
        val message = reply

        // Save notification to repository
        val repository = NotificationRepository(context)
        repository.addNotification(title, message, NotificationType.ADMIN_REPLY)

        sendNotification(context, title, message, NOTIFICATION_ID_ADMIN_REPLY)
    }

    /**
     * Send notification for upcoming rain (from forecast)
     */
    fun showRainForecastNotification(context: Context, rainProbability: Int, date: String) {
        createNotificationChannel(context)

        val (title, message, notificationId) = when {
            rainProbability >= 70 -> Triple(
                "🌧️ High Rain Probability Tomorrow - $date",
                "There's a $rainProbability% chance of rain tomorrow in Cebu. Consider preparing white sugar water for your bees!",
                NOTIFICATION_ID_MODERATE_RAIN
            )
            rainProbability >= 50 -> Triple(
                "🌦️ Rain Possible Tomorrow - $date",
                "There's a $rainProbability% chance of rain tomorrow. You may want to prepare white sugar water just in case.",
                NOTIFICATION_ID_RAIN
            )
            else -> return // Don't notify for low probability
        }

        // Save notification to repository
        val repository = NotificationRepository(context)
        repository.addNotification(title, message, NotificationType.RAIN_ALERT)

        sendNotification(context, title, message, notificationId)
    }

    /**
     * Send the notification
     */
    private fun sendNotification(context: Context, title: String, message: String, notificationId: Int) {
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Use a unique ID so each notification appears separately instead of
        // replacing the previous one of the same type.
        val uniqueId = (notificationId * 100_000 + (System.currentTimeMillis() % 100_000)).toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))

        try {
            NotificationManagerCompat.from(context).notify(uniqueId, builder.build())
        } catch (e: SecurityException) {
            // Handle case where notification permission is not granted
            e.printStackTrace()
        }
    }

    /**
     * Clear all rain notifications
     */
    fun clearNotifications(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_RAIN)
        notificationManager.cancel(NOTIFICATION_ID_MODERATE_RAIN)
        notificationManager.cancel(NOTIFICATION_ID_HEAVY_RAIN)
        notificationManager.cancel(NOTIFICATION_ID_SEVERE)
    }
}
