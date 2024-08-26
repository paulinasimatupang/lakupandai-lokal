package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Mutation
import java.text.NumberFormat
import java.util.Locale

class MutationAdapter(private val mutations: List<Mutation>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val groupedMutations = mutations.groupBy { it.date }.toList()
    private val archiveNumber = mutations.lastOrNull()?.archiveNumber // Assuming archive number is from the last mutation

    override fun getItemViewType(position: Int): Int {
        return if (position < itemCount - 1) R.layout.mutation_item else R.layout.archive_number_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == R.layout.mutation_item) {
            val view = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)
            MutationViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)
            ArchiveNumberViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MutationViewHolder) {
            val index = position
            val (date, transactions) = groupedMutations[index]
            holder.bind(date, transactions)
        } else if (holder is ArchiveNumberViewHolder) {
            holder.bind(archiveNumber)
        }
    }

    override fun getItemCount(): Int {
        return groupedMutations.size + 0 // Adding one for the archive number item
    }

    inner class MutationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        private val transactionContainer: LinearLayout =
            itemView.findViewById(R.id.transaction_container)

        fun bind(date: String, transactions: List<Mutation>) {
            dateTextView.text = date
            transactionContainer.removeAllViews()

            transactions.forEach { mutation ->
                val transactionView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.transaction_item, transactionContainer, false)

                val descriptionTextView: TextView =
                    transactionView.findViewById(R.id.tv_description)
                val amountTextView: TextView = transactionView.findViewById(R.id.tv_amount)
                val transactionTypeTextView: TextView = transactionView.findViewById(R.id.tv_transaction_type)
                val timeTextView: TextView = transactionView.findViewById(R.id.tv_time)

                descriptionTextView.text = mutation.description
                amountTextView.text = formatRupiah(mutation.amount)
                timeTextView.text = mutation.time

                // Set transaction type text (DB or CR)
                transactionTypeTextView.text = if (mutation.transactionType == "DEBIT") "DB" else "CR"

                // Set text color based on transaction type
                if (mutation.transactionType == "DEBIT") {
                    amountTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                } else {
                    amountTextView.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                }

                transactionContainer.addView(transactionView)
            }
        }

        private fun formatRupiah(amount: String): String {
            val amountValue = amount.replace(",", "").replace("Rp", "").toDoubleOrNull() ?: 0.0
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            format.maximumFractionDigits = 0
            return format.format(amountValue)
        }
    }

    inner class ArchiveNumberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val archiveNumberTextView: TextView = itemView.findViewById(R.id.tv_archive_number)

        fun bind(archiveNumber: String?) {
            if (archiveNumber == null) {
                archiveNumberTextView.visibility = View.VISIBLE
            } else {
                archiveNumberTextView.visibility = View.GONE
            }
        }
    }
}
