package com.ramos.marketly.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ramos.marketly.R
import com.ramos.marketly.controller.IncidenceDetailActivity
import com.ramos.marketly.model.IncidencePreview

class IncidenceAdapter(private val incidences: MutableList<IncidencePreview>) :
    RecyclerView.Adapter<IncidenceAdapter.IncidenceViewHolder>() {

    private var view: View? = null
    private var incidence: IncidencePreview? = null
    private var intent: Intent? = null
    private var parts: List<String>? = null

    class IncidenceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        val tvBuyer: TextView = itemView.findViewById(R.id.tvBuyer)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidenceViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_incidence, parent, false)
        return IncidenceViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: IncidenceViewHolder, position: Int) {
        incidence = incidences[position]

        holder.tvProductTitle.text = incidence!!.productTitle
        holder.tvBuyer.text = "Comprador: ${incidence!!.buyerUsername}"
        holder.tvDescription.text = incidence!!.description
        holder.tvDate.text = formatDate(incidence!!.createdAt)

        if (incidence!!.status == "en_disputa") {
            holder.tvStatus.text = "en disputa"
            holder.tvStatus.setBackgroundColor(0xFFD97706.toInt())
        } else {
            holder.tvStatus.text = incidence!!.status
            holder.tvStatus.setBackgroundColor(0xFFEF4444.toInt())
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, IncidenceDetailActivity::class.java)
            intent!!.putExtra("incidence_id", incidence!!.id)
            intent!!.putExtra("order_id", incidence!!.orderId)
            intent!!.putExtra("product_id", incidence!!.productId)
            intent!!.putExtra("product_title", incidence!!.productTitle)
            intent!!.putExtra("product_image", incidence!!.productImageUrl)
            intent!!.putExtra("buyer_username", incidence!!.buyerUsername)
            intent!!.putExtra("seller_username", incidence!!.sellerUsername)
            intent!!.putExtra("buyer_id", incidence!!.buyerId)
            intent!!.putExtra("seller_id", incidence!!.sellerId)
            intent!!.putExtra("description", incidence!!.description)
            intent!!.putExtra("status", incidence!!.status)
            intent!!.putExtra("amount", incidence!!.amount)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = incidences.size

    fun setIncidences(newIncidences: List<IncidencePreview>) {
        incidences.clear()
        incidences.addAll(newIncidences)
        notifyDataSetChanged()
    }

    private fun formatDate(createdAt: String?): String {
        if (createdAt == null) return ""
        return try {
            parts = createdAt.split("T")
            parts!![0]
        } catch (e: Exception) {
            ""
        }
    }
}