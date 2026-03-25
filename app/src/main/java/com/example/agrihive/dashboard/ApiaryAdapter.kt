package com.example.agrihive.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.addapiary.Apiary
import com.example.agrihive.databinding.ItemApiaryCardBinding

class ApiaryAdapter(
    private val onApiaryClick: (Apiary) -> Unit
) : ListAdapter<Apiary, ApiaryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemApiaryCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onApiaryClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemApiaryCardBinding,
        private val onApiaryClick: (Apiary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(apiary: Apiary) {
            binding.tvApiaryName.text = apiary.name
            binding.ivStatus.setBackgroundResource(
                if (apiary.isConnected) R.drawable.bg_green_circle else R.drawable.bg_red_circle
            )
            binding.tvTemp.text = "%.1f°C".format(apiary.temperature)
            binding.tvHumidity.text = "%.0f%%".format(apiary.humidity)
            binding.tvWeight.text = "%.1f kg".format(apiary.weight)
            binding.root.setOnClickListener { onApiaryClick(apiary) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Apiary>() {
        override fun areItemsTheSame(a: Apiary, b: Apiary) = a.id == b.id
        override fun areContentsTheSame(a: Apiary, b: Apiary) = a == b
    }
}
