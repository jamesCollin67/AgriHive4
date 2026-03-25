package com.example.agrihive.hivestreams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R

data class ConfidenceResult(val name: String, val confidence: Int, val color: Int)

class ConfidenceAdapter(private val items: List<ConfidenceResult>) :
    RecyclerView.Adapter<ConfidenceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDiseaseName)
        val pbConfidence: ProgressBar = view.findViewById(R.id.pbConfidence)
        val tvPercent: TextView = view.findViewById(R.id.tvPercent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_confidence_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.pbConfidence.progress = item.confidence
        holder.tvPercent.text = "${item.confidence}%"
        
        // In a real app, you'd set the progress tint programmatically if needed
        // holder.pbConfidence.progressTintList = ColorStateList.valueOf(item.color)
    }

    override fun getItemCount() = items.size
}
