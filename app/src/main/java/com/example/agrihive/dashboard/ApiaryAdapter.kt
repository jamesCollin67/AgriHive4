package com.example.agrihive.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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

            // Show "--" when node is offline — don't show stale 0.0 values
            if (apiary.isConnected) {
                binding.tvTemp.text     = "%.1f°C".format(apiary.temperature)
                binding.tvHumidity.text = "%.0f%%".format(apiary.humidity)
                binding.tvMoisture.text = "%.1f%%".format(apiary.moisture)
                binding.tvWeight.text   = "%.1fkg".format(apiary.weight)
            } else {
                binding.tvTemp.text     = "--"
                binding.tvHumidity.text = "--"
                binding.tvMoisture.text = "--"
                binding.tvWeight.text   = "--"
            }

            val alerts = calculateAlerts(apiary)
            if (alerts > 0) {
                binding.tvAlertBadge.visibility = View.VISIBLE
                binding.tvAlertBadge.text = alerts.toString()
            } else {
                binding.tvAlertBadge.visibility = View.GONE
            }

            // Harvest ready badge
            if (isHarvestReady(apiary)) {
                binding.tvHarvestBadge.visibility = View.VISIBLE
            } else {
                binding.tvHarvestBadge.visibility = View.GONE
            }

            // ⋮ menu button → popup with Edit / Delete options
            binding.btnApiaryMenu.setOnClickListener { anchor ->
                val popup = PopupMenu(anchor.context, anchor)
                popup.menu.add(0, 1, 0, "✏️  Edit Apiary")
                popup.menu.add(0, 2, 1, "🗑️  Delete Apiary")
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        1, 2 -> { onApiaryLongClick(apiary); true }
                        else -> false
                    }
                }
                popup.show()
            }

            // Short tap → open HiveStreams
            binding.root.setOnClickListener { onApiaryClick(apiary) }

            // Long press still works as fallback
            binding.root.setOnLongClickListener {
                onApiaryLongClick(apiary)
                true
            }
        }

        private fun calculateAlerts(apiary: Apiary): Int {
            var count = 0
            if (apiary.moisture > 18.0) count++
            if (apiary.temperature > 0 && (apiary.temperature < 34.0 || apiary.temperature > 36.0)) count++
            if (apiary.weight in 0.1..4.9) count++ // low weight alert
            return count
        }

        private fun isHarvestReady(apiary: Apiary) =
            apiary.moisture in 0.1..18.0 && apiary.weight > 5.0 && apiary.isConnected
    }

    private class DiffCallback : DiffUtil.ItemCallback<Apiary>() {
        override fun areItemsTheSame(a: Apiary, b: Apiary) = a.id == b.id
        override fun areContentsTheSame(a: Apiary, b: Apiary) = a == b
    }
}
