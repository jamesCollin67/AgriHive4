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

            // Distinct icon + tint per notification type
            val (iconRes, iconTint, bgTint) = when (notification.type) {
                NotificationType.TEMPERATURE_ALERT -> Triple(
                    R.drawable.ic_temperature, "#EF5350", "#2DEF5350"
                )
                NotificationType.RAIN_ALERT -> Triple(
                    R.drawable.ic_cloud, "#2196F3", "#2D2196F3"
                )
                NotificationType.FEEDING_ALERT -> Triple(
                    R.drawable.ic_bee, "#F4B400", "#2DF4B400"
                )
                NotificationType.ADMIN_REPLY -> Triple(
                    R.drawable.ic_summary, "#66BB6A", "#2D66BB6A"
                )
                NotificationType.SYSTEM -> Triple(
                    R.drawable.ic_bell, "#9CAF9F", "#2D9CAF9F"
                )
            }

            iconView.setImageResource(iconRes)
            iconView.imageTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(iconTint)
            )
            itemView.findViewById<View>(R.id.ivIconBg).backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(bgTint)
                )

            // Admin replies get a distinct gold title color to stand out
            titleView.setTextColor(
                android.graphics.Color.parseColor(
                    if (notification.type == NotificationType.ADMIN_REPLY) "#66BB6A" else "#F4B400"
                )
            )

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
