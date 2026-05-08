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
import com.ramos.marketly.model.PurchaseItem

class PurchaseAdapter(private val purchases: MutableList<PurchaseItem>) :
    RecyclerView.Adapter<PurchaseAdapter.PurchaseViewHolder>() {

    private var view: View? = null
    private var purchase: PurchaseItem? = null
    private var intent: Intent? = null

    class PurchaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_purchase, parent, false)
        return PurchaseViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: PurchaseViewHolder, position: Int) {
        purchase = purchases[position]

        holder.tvProductTitle.text = purchase!!.productTitle
        holder.tvAmount.text = "${purchase!!.amount} "
        holder.tvStatus.text = purchase!!.status

        if (purchase!!.status == "en_espera") {
            holder.tvStatus.text = "En espera"
            holder.tvStatus.setBackgroundColor(0xFFD97706.toInt())
        } else if (purchase!!.status == "completada") {
            holder.tvStatus.text = "Completada"
            holder.tvStatus.setBackgroundColor(0xFF10B981.toInt())
        } else if (purchase!!.status == "en_disputa") {
            holder.tvStatus.text = "En disputa"
            holder.tvStatus.setBackgroundColor(0xFFEF4444.toInt())
        } else if (purchase!!.status == "reembolsada") {
            holder.tvStatus.text = "Reembolsada"
            holder.tvStatus.setBackgroundColor(0xFF6B7280.toInt())
        } else {
            holder.tvStatus.setBackgroundColor(0xFF9CA3AF.toInt())
        }

        if (!purchase!!.productImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(purchase!!.productImageUrl)
                .centerCrop()
                .into(holder.ivProductImage)
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, ChatActivity::class.java)
            intent!!.putExtra("product_id", purchase!!.productId)
            intent!!.putExtra("seller_id", purchase!!.sellerId)
            intent!!.putExtra("product_title", purchase!!.productTitle)
            intent!!.putExtra("product_image", purchase!!.productImageUrl)
            intent!!.putExtra("chat_id", purchase!!.chatId)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = purchases.size

    fun setPurchases(newPurchases: List<PurchaseItem>) {
        purchases.clear()
        purchases.addAll(newPurchases)
        notifyDataSetChanged()
    }
}