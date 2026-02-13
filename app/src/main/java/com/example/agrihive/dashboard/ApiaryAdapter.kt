import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.addapiary.Apiary

class ApiaryAdapter(
    private val onItemClick: (Apiary) -> Unit
) : ListAdapter<Apiary, ApiaryAdapter.ApiaryViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Apiary>() {
            override fun areItemsTheSame(oldItem: Apiary, newItem: Apiary) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Apiary, newItem: Apiary) =
                oldItem == newItem
        }
    }

    inner class ApiaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvApiaryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApiaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_apiary, parent, false)

        return ApiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApiaryViewHolder, position: Int) {
        val apiary = getItem(position)
        holder.name.text = apiary.name

        // ✅ CLICK LISTENER
        holder.itemView.setOnClickListener {
            onItemClick(apiary)
        }
    }
}
