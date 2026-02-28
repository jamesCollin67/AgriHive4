package com.example.agrihive.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ActivityLogAdapter : ListAdapter<ActivityLogItem, ActivityLogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(item: ActivityLogItem) {
            tvDescription.text = item.description

            // Format exact time
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvTime.text = timeFormat.format(item.timestamp)

            // Format relative time
            tvTimeAgo.text = getRelativeTime(item.timestamp)
        }

        private fun getRelativeTime(date: Date): String {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { time = date }

            val diffInMillis = now.timeInMillis - target.timeInMillis
            val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
            val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            val diffInWeeks = diffInDays / 7

            return when {
                diffInMinutes < 1 -> "Just now"
                diffInMinutes < 60 -> "${diffInMinutes}m ago"
                diffInHours < 24 -> "${diffInHours}h ago"
                diffInDays < 7 -> "${diffInDays}d ago"
                diffInWeeks < 4 -> "${diffInWeeks}w ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ActivityLogItem>() {
        override fun areItemsTheSame(oldItem: ActivityLogItem, newItem: ActivityLogItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityLogItem, newItem: ActivityLogItem): Boolean {
            return oldItem == newItem
        }
    }
}
