package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R

class ProdukAdapter(
    private val items: List<Pair<String?, String?>>,
    private val onItemSelected: (Pair<String?, String?>) -> Unit
) : RecyclerView.Adapter<ProdukAdapter.ViewHolder>() {
    private var selectedItemPosition: Int? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.product_name)
        val layout: RelativeLayout = itemView.findViewById(R.id.product_layout)
        fun bind(item: Pair<String?, String?>, isSelected: Boolean) {
            textView.text = item.first

            // Ubah background jika item dipilih
            if (isSelected) {
                layout.background = ContextCompat.getDrawable(itemView.context, R.drawable.semi_circle_white)
            } else {
                layout.background = ContextCompat.getDrawable(itemView.context, R.drawable.card_background)
            }

            itemView.setOnClickListener {
                onItemSelected(item)
                selectedItemPosition = adapterPosition
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_combo_box_produk, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == selectedItemPosition)
    }

    override fun getItemCount(): Int = items.size
}
