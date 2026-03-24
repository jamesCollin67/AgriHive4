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
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(item: ActivityLogItem) {
            tvTitle.text = item.title
            tvDescription.text = item.description
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvTimestamp.text = "${getDateLabel(item.timestamp)} ${timeFormat.format(item.timestamp)}"
        }

        private fun getDateLabel(date: Date): String {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { time = date }

            // Check if same day
            val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

            if (isSameDay) {
                return "Today"
            }

            // Check if yesterday
            now.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

            if (isYesterday) {
                return "Yesterday"
            }

            // Check if within this week
            val diffInDays = TimeUnit.MILLISECONDS.toDays(
                Calendar.getInstance().timeInMillis - date.time
            )
            if (diffInDays < 7) {
                // Show day name (Monday, Tuesday, etc.)
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                return dayFormat.format(date)
            }

            // Show date (Jan 15)
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            return dateFormat.format(date)
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
