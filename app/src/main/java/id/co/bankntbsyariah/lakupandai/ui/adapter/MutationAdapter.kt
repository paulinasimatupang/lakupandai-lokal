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
            .inflate(R.layout.mutation_item, parent, false)
        return MutationViewHolder(view)
    }

    override fun onBindViewHolder(holder: MutationViewHolder, position: Int) {
        holder.bind(mutations[position])
    }

    override fun getItemCount(): Int = mutations.size

    class MutationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mutationDate: TextView = itemView.findViewById(R.id.mutation_date)
        private val mutationDescription: TextView = itemView.findViewById(R.id.mutation_description)
        private val mutationAmount: TextView = itemView.findViewById(R.id.mutation_amount)
        private val mutationTime: TextView = itemView.findViewById(R.id.mutation_time)

        fun bind(mutation: Mutation) {
            mutationDate.text = mutation.date
            mutationDescription.text = mutation.description
            mutationAmount.text = mutation.amount
            mutationTime.text = mutation.time

        }
    }
}
