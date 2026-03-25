package com.example.agrihive.hivestreams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedTreatmentsAdapter : ListAdapter<SavedTreatment, SavedTreatmentsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_saved_treatment_v2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

        fun bind(item: SavedTreatment) {
            tvTitle.text = item.diseaseName
            val date = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.timestamp))
            tvSubtitle.text = "${item.hiveName} · $date"
            tvDescription.text = item.description
            tvScore.text = item.healthScore.toString()
            
            // Set color based on score
            if (item.healthScore < 50) {
                tvScore.setTextColor(itemView.context.getColor(android.R.color.holo_red_light))
            } else {
                tvScore.setTextColor(itemView.context.getColor(android.R.color.holo_green_light))
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<SavedTreatment>() {
        override fun areItemsTheSame(oldItem: SavedTreatment, newItem: SavedTreatment) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SavedTreatment, newItem: SavedTreatment) = oldItem == newItem
    }
}
