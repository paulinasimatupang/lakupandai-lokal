package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Mutation

class MutationAdapter(private val mutations: List<Mutation>) :
    RecyclerView.Adapter<MutationAdapter.MutationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MutationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mutation_item, parent, false)  // Menggunakan layout mutation_item
        return MutationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MutationViewHolder, position: Int) {
        holder.bind(mutations[position])  // Menghubungkan data ke ViewHolder
    }

    override fun getItemCount(): Int = mutations.size  // Mengembalikan jumlah item mutasi

    class MutationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mutationDate: TextView = itemView.findViewById(R.id.mutation_date)
        private val mutationTime: TextView = itemView.findViewById(R.id.mutation_time)
        private val mutationDescription: TextView = itemView.findViewById(R.id.mutation_description)
        private val mutationAmount: TextView = itemView.findViewById(R.id.mutation_amount)

        fun bind(mutation: Mutation) {
            mutationDate.text = mutation.date  // Tampilkan tanggal
            mutationTime.text = mutation.time  // Tampilkan waktu
            mutationDescription.text = mutation.description  // Tampilkan deskripsi
            mutationAmount.text = mutation.amount  // Tampilkan jumlah transaksi

            // Penyesuaian warna berdasarkan nilai positif/negatif
            val amount = mutation.amount.replace(",", "").toDoubleOrNull()
            if (amount != null && amount < 0) {
                mutationAmount.setTextColor(itemView.context.getColor(R.color.yellow))
            } else {
                mutationAmount.setTextColor(itemView.context.getColor(R.color.green))
            }
        }
    }
}

