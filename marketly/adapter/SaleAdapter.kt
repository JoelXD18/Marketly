package com.ramos.marketly.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ramos.marketly.R
import com.ramos.marketly.controller.ChatActivity
import com.ramos.marketly.model.SaleItem

class SaleAdapter(private val sales: MutableList<SaleItem>) :
    RecyclerView.Adapter<SaleAdapter.SaleViewHolder>() {

    private var view: View? = null
    private var sale: SaleItem? = null
    private var intent: Intent? = null

    class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        val tvBuyer: TextView = itemView.findViewById(R.id.tvBuyer)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        sale = sales[position]

        holder.tvProductTitle.text = sale!!.productTitle
        holder.tvBuyer.text = "Comprador: ${sale!!.buyerUsername}"
        holder.tvAmount.text = "${sale!!.amount} "

        if (sale!!.status == "en_espera") {
            holder.tvStatus.text = "En espera"
            holder.tvStatus.setBackgroundColor(0xFFD97706.toInt())
        } else if (sale!!.status == "completada") {
            holder.tvStatus.text = "Completada"
            holder.tvStatus.setBackgroundColor(0xFF10B981.toInt())
        } else if (sale!!.status == "en_disputa") {
            holder.tvStatus.text = "En disputa"
            holder.tvStatus.setBackgroundColor(0xFFEF4444.toInt())
        } else if (sale!!.status == "reembolsada") {
            holder.tvStatus.text = "Reembolsada"
            holder.tvStatus.setBackgroundColor(0xFF6B7280.toInt())
        } else {
            holder.tvStatus.setBackgroundColor(0xFF9CA3AF.toInt())
        }

        if (!sale!!.productImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(sale!!.productImageUrl)
                .centerCrop()
                .into(holder.ivProductImage)
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent!!.putExtra("product_id", sale!!.productId)
            intent!!.putExtra("seller_id", sale!!.buyerId)
            intent!!.putExtra("product_title", sale!!.productTitle)
            intent!!.putExtra("product_image", sale!!.productImageUrl)
            intent!!.putExtra("chat_id", sale!!.chatId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = sales.size

    fun setSales(newSales: List<SaleItem>) {
        sales.clear()
        sales.addAll(newSales)
        notifyDataSetChanged()
    }
}