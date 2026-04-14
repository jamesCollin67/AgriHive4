package com.example.agrihive.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.addapiary.Apiary
import com.example.agrihive.databinding.ItemApiaryCardBinding

class ApiaryAdapter(
    private val onApiaryClick: (Apiary) -> Unit,
    private val onApiaryLongClick: (Apiary) -> Unit = {}
) : ListAdapter<Apiary, ApiaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApiaryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onApiaryClick, onApiaryLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemApiaryCardBinding,
        private val onApiaryClick: (Apiary) -> Unit,
        private val onApiaryLongClick: (Apiary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apiary: Apiary) {
            binding.tvApiaryName.text = apiary.name
            binding.tvLocation.text = if (apiary.location.isBlank()) "No Location Set" else apiary.location

            binding.ivStatus.setBackgroundResource(
                if (apiary.isConnected) R.drawable.bg_green_circle else R.drawable.bg_red_circle
            )

            binding.tvTemp.text     = "%.1f°C".format(apiary.temperature)
            binding.tvHumidity.text = "%.0f%%".format(apiary.humidity)
            binding.tvMoisture.text = "%.1f%%".format(apiary.moisture)
            binding.tvWeight.text   = "%.1fkg".format(apiary.weight)

            val alerts = calculateAlerts(apiary)
            if (alerts > 0) {
                binding.tvAlertBadge.visibility = View.VISIBLE
                binding.tvAlertBadge.text = alerts.toString()
            } else {
                binding.tvAlertBadge.visibility = View.GONE
            }

            // Short tap → open HiveStreams
            binding.root.setOnClickListener { onApiaryClick(apiary) }

            // Long press → open Edit Apiary
            binding.root.setOnLongClickListener {
                onApiaryLongClick(apiary)
                true // consume the event
            }
        }

        private fun calculateAlerts(apiary: Apiary): Int {
            var count = 0
            // 1. Moisture Content Threshold (> 18% is alert)
            if (apiary.moisture > 18.0) count++
            
            // 2. Temperature Threshold (Optimal: 34°C – 36°C)
            if (apiary.temperature > 0 && (apiary.temperature < 34.0 || apiary.temperature > 36.0)) {
                count++
            }
            
            return count
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Apiary>() {
        override fun areItemsTheSame(a: Apiary, b: Apiary) = a.id == b.id
        override fun areContentsTheSame(a: Apiary, b: Apiary) = a == b
    }
}
