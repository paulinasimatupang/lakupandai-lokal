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
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Define the constants for view types inside the companion object
    companion object {
        const val VIEW_TYPE_DATE = 0
        const val VIEW_TYPE_TRANSACTION = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0 || mutations[position].date != mutations[position - 1].date) {
            VIEW_TYPE_DATE
        } else {
            VIEW_TYPE_TRANSACTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_DATE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.mutation_date_item, parent, false)
            DateViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.mutation_transaction_item, parent, false)
            TransactionViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is DateViewHolder) {
            holder.bind(mutations[position])
        } else if (holder is TransactionViewHolder) {
            holder.bind(mutations[position])
        }
    }

    override fun getItemCount(): Int = mutations.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mutationDate: TextView = itemView.findViewById(R.id.mutation_date)

        fun bind(mutation: Mutation) {
            mutationDate.text = mutation.date
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mutationTime: TextView = itemView.findViewById(R.id.mutation_time)
        private val mutationDescription: TextView = itemView.findViewById(R.id.mutation_description)
        private val mutationAmount: TextView = itemView.findViewById(R.id.mutation_amount)

        fun bind(mutation: Mutation) {
            mutationTime.text = mutation.time
            mutationDescription.text = mutation.description
            mutationAmount.text = mutation.amount

            // Adjust color based on the transaction amount
            val amount = mutation.amount.replace(",", "").toDoubleOrNull()
            if (amount != null && amount < 0) {
                mutationAmount.setTextColor(itemView.context.getColor(R.color.yellow))
            } else {
                mutationAmount.setTextColor(itemView.context.getColor(R.color.green))
            }
        }
    }
}
