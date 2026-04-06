package com.example.agrihive.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying notifications in a RecyclerView
 */
class NotificationAdapter(
    private var notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit,
    private val onDeleteClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification_v2, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<NotificationItem>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.ivIcon)
        private val titleView: TextView = itemView.findViewById(R.id.tvTitle)
        private val messageView: TextView = itemView.findViewById(R.id.tvContent)
        private val timeView: TextView = itemView.findViewById(R.id.tvTime)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadDot)

        fun bind(notification: NotificationItem) {
            titleView.text = notification.title
            messageView.text = notification.message
            timeView.text = formatTime(notification.timestamp)
            
            // Show/hide unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

            // Set icon based on notification type
            val iconRes = when (notification.type) {
                NotificationType.RAIN_ALERT -> R.drawable.ic_cloud
                NotificationType.FEEDING_ALERT -> R.drawable.ic_bee
                NotificationType.TEMPERATURE_ALERT -> R.drawable.ic_temperature
                NotificationType.SYSTEM -> R.drawable.ic_bell
                NotificationType.ADMIN_REPLY -> R.drawable.ic_summary
            }
            iconView.setImageResource(iconRes)

            // Click listeners
            itemView.setOnClickListener { onItemClick(notification) }
            itemView.setOnLongClickListener {
                onDeleteClick(notification)
                true
            }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60 * 1000 -> "Just now"
                diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min ago"
                diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
                diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
                else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
