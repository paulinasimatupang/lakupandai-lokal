package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Mutation

class MutationAdapter(private val mutations: List<Mutation>) :
    RecyclerView.Adapter<MutationAdapter.MutationViewHolder>() {

    private val groupedMutations = mutations.groupBy { it.date }.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MutationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.mutation_item, parent, false)
        return MutationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MutationViewHolder, position: Int) {
        val (date, transactions) = groupedMutations[position]
        holder.bind(date, transactions)
    }

    override fun getItemCount(): Int = groupedMutations.size

    class MutationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        private val transactionContainer: LinearLayout = itemView.findViewById(R.id.transaction_container)

        fun bind(date: String, transactions: List<Mutation>) {
            dateTextView.text = date
            transactionContainer.removeAllViews() // Menghapus transaksi sebelumnya

            transactions.forEach { mutation ->
                val transactionView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.transaction_item, transactionContainer, false)

                val descriptionTextView: TextView = transactionView.findViewById(R.id.tv_description)
                val amountTextView: TextView = transactionView.findViewById(R.id.tv_amount)
                val timeTextView: TextView = transactionView.findViewById(R.id.tv_time)

                descriptionTextView.text = mutation.description
                amountTextView.text = mutation.amount
                timeTextView.text = mutation.time

                // Atur warna teks berdasarkan jumlah transaksi
                val amount = mutation.amount.replace(",", "").replace("Rp ", "").toDoubleOrNull()
                if (amount != null && amount < 0) {
                    amountTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                } else {
                    amountTextView.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                }

                transactionContainer.addView(transactionView)
            }
        }
    }
}
