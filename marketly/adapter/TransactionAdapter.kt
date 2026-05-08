package com.ramos.marketly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ramos.marketly.R
import com.ramos.marketly.model.WalletTransaction

class TransactionAdapter(private val transactions: MutableList<WalletTransaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var view: View? = null
    private var transaction: WalletTransaction? = null
    private var parts: List<String>? = null

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTypeIcon: TextView = itemView.findViewById(R.id.tvTypeIcon)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        transaction = transactions[position]

        holder.tvDescription.text = transaction!!.description
        holder.tvDate.text = formatDate(transaction!!.createdAt)

        if (transaction!!.type == "compra") {
            holder.tvTypeIcon.text = ""
            holder.tvAmount.text = "-${transaction!!.amount} "
            holder.tvAmount.setTextColor(0xFFEF4444.toInt())
        } else if (transaction!!.type == "venta") {
            holder.tvTypeIcon.text = ""
            holder.tvAmount.text = "+${transaction!!.amount} "
            holder.tvAmount.setTextColor(0xFF10B981.toInt())
        } else if (transaction!!.type == "reembolso") {
            holder.tvTypeIcon.text = "️"
            holder.tvAmount.text = "+${transaction!!.amount} "
            holder.tvAmount.setTextColor(0xFF10B981.toInt())
        } else if (transaction!!.type == "recarga") {
            holder.tvTypeIcon.text = ""
            holder.tvAmount.text = "+${transaction!!.amount} "
            holder.tvAmount.setTextColor(0xFF10B981.toInt())
        } else {
            holder.tvTypeIcon.text = ""
            holder.tvAmount.text = "${transaction!!.amount} "
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun setTransactions(newTransactions: List<WalletTransaction>) {
        transactions.clear()
        transactions.addAll(newTransactions)
        notifyDataSetChanged()
    }

    private fun formatDate(createdAt: String?): String {
        if (createdAt == null) return ""
        return try {
            parts = createdAt.split("T")
            if (parts!!.size >= 2) {
                "${parts!![0]} ${parts!![1].substring(0, 5)}"
            } else parts!![0]
        } catch (e: Exception) {
            ""
        }
    }
}