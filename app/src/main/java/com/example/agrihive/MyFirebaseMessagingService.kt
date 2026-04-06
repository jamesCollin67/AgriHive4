package com.example.agrihive

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.notification.NotificationActivity
import com.example.agrihive.notification.NotificationRepository
import com.example.agrihive.notification.NotificationType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle both notification-type messages and data-only messages
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "AgriHive Alert"
            
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: ""

        Log.d("FCM", "Received message: $title - $body")

        // Save to NotificationRepository so it appears in the Alert UI
        saveToRepository(title, body, remoteMessage.data["type"])

        sendNotification(title, body)
    }

    private fun saveToRepository(title: String, message: String, typeStr: String?) {
        val repository = NotificationRepository(this)
        val type = when (typeStr) {
            "ADMIN_REPLY" -> NotificationType.ADMIN_REPLY
            "RAIN_ALERT" -> NotificationType.RAIN_ALERT
            "FEEDING_ALERT" -> NotificationType.FEEDING_ALERT
            "TEMPERATURE_ALERT" -> NotificationType.TEMPERATURE_ALERT
            else -> NotificationType.SYSTEM
        }
        repository.addNotification(title, message, type)
    }

    private fun sendNotification(title: String, messageBody: String) {
        // Change intent to NotificationActivity so user goes directly to the Alert UI
        val intent = Intent(this, NotificationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "HIVE_ALERTS")
            .setSmallIcon(R.drawable.ic_logo_honeycomb)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "New token: $token")
        saveTokenToFirestore(token)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users")
            .document(uid)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d("FCM", "Token updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FCM", "Error updating token", e)
            }
    }
}
