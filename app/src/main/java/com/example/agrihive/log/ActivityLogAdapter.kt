package com.example.agrihive.log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
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
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvTag: TextView = itemView.findViewById(R.id.tvTag)
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivLogIcon)
        private val flIconBg: View = itemView.findViewById(R.id.flIconBg)

        fun bind(item: ActivityLogItem) {
            tvDescription.text = item.description
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            tvTimestamp.text = "${getDateLabel(item.timestamp)} ${timeFormat.format(item.timestamp)}"
            
            // Set tag and icon based on type
            tvTag.text = item.type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
            
            val context = itemView.context
            when (item.type) {
                LogType.HIVE_SENSOR -> {
                    ivIcon.setImageResource(R.drawable.ic_temperature)
                    tvTag.setTextColor(ContextCompat.getColor(context, R.color.status_error))
                }
                LogType.USER_ACCOUNT -> {
                    ivIcon.setImageResource(R.drawable.ic_profile)
                    tvTag.setTextColor(ContextCompat.getColor(context, R.color.login_text_primary))
                }
                LogType.SUBSCRIPTION -> {
                    ivIcon.setImageResource(R.drawable.ic_saved)
                    tvTag.setTextColor(ContextCompat.getColor(context, R.color.status_warning))
                }
                else -> {
                    ivIcon.setImageResource(R.drawable.ic_settings)
                    tvTag.setTextColor(ContextCompat.getColor(context, R.color.login_text_secondary))
                }
            }
        }

        private fun getDateLabel(date: Date): String {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply { time = date }

            val isSameDay = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

            if (isSameDay) return "Today,"

            now.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterday = now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

            if (isYesterday) return "Yesterday,"

            val diffInDays = TimeUnit.MILLISECONDS.toDays(
                Calendar.getInstance().timeInMillis - date.time
            )
            if (diffInDays < 7) {
                return SimpleDateFormat("EEEE,", Locale.getDefault()).format(date)
            }

            return SimpleDateFormat("MMM d,", Locale.getDefault()).format(date)
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<ActivityLogItem>() {
        override fun areItemsTheSame(oldItem: ActivityLogItem, newItem: ActivityLogItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ActivityLogItem, newItem: ActivityLogItem): Boolean = oldItem == newItem
    }
}
