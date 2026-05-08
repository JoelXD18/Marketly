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
import com.ramos.marketly.controller.ProductDetailActivity
import com.ramos.marketly.model.Product

class ProductAdapter(private val products: MutableList<Product>) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var view: View? = null
    private var product: Product? = null
    private var role: String? = null
    private var firstImage: String? = null
    private var intent: Intent? = null

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvType: TextView = itemView.findViewById(R.id.tvProductType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvSeller: TextView = itemView.findViewById(R.id.tvProductSeller)
        val ivAdminBadge: ImageView = itemView.findViewById(R.id.ivAdminBadge)
        val ivModBadge: ImageView = itemView.findViewById(R.id.ivModBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        product = products[position]

        holder.tvType.text = product!!.productType
        holder.tvTitle.text = product!!.title
        holder.tvPrice.text = "${product!!.price} "
        holder.tvSeller.text = product!!.seller?.username ?: "Desconocido"

        // Badges de rol
        role = product!!.seller?.role ?: "cliente"
        if (role == "administrador") {
            holder.ivAdminBadge.visibility = View.VISIBLE
            holder.ivModBadge.visibility = View.GONE
        } else if (role == "moderador") {
            holder.ivModBadge.visibility = View.VISIBLE
            holder.ivAdminBadge.visibility = View.GONE
        } else {
            holder.ivAdminBadge.visibility = View.GONE
            holder.ivModBadge.visibility = View.GONE
        }

        firstImage = product!!.imageUrls?.firstOrNull()
        if (firstImage != null) {
            Glide.with(holder.itemView.context)
                .load(firstImage)
                .centerCrop()
                .into(holder.ivImage)
        } else {
            holder.ivImage.setImageResource(R.drawable.marketly)
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, ProductDetailActivity::class.java)
            intent!!.putExtra("product_id", product!!.id)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    fun appendProducts(newProducts: List<Product>) {
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}