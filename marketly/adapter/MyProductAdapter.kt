package com.ramos.marketly.adapter

import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.ramos.marketly.R
import com.ramos.marketly.controller.EditProductActivity
import com.ramos.marketly.controller.ProductDetailActivity
import com.ramos.marketly.model.MyProductItem
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyProductAdapter(
    private val products: MutableList<MyProductItem>,
    private val onProductDeleted: Runnable
) : RecyclerView.Adapter<MyProductAdapter.MyProductViewHolder>() {

    private var view: View? = null
    private var product: MyProductItem? = null
    private var intent: Intent? = null

    class MyProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        val tvProductTitle: TextView = itemView.findViewById(R.id.tvProductTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProductViewHolder {
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_product, parent, false)
        return MyProductViewHolder(view!!)
    }

    override fun onBindViewHolder(holder: MyProductViewHolder, position: Int) {
        product = products[position]

        holder.tvProductTitle.text = product!!.title
        holder.tvCategory.text = product!!.category
        holder.tvPrice.text = "${product!!.price} "

        if (product!!.status == "activo") {
            holder.tvStatus.text = "Activo"
            holder.tvStatus.setBackgroundColor(0xFF10B981.toInt())
        } else {
            holder.tvStatus.text = "Vendido"
            holder.tvStatus.setBackgroundColor(0xFF6B7280.toInt())
        }

        if (!product!!.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(product!!.imageUrl)
                .centerCrop()
                .into(holder.ivProductImage)
        }

        holder.itemView.setOnClickListener {
            intent = Intent(holder.itemView.context, ProductDetailActivity::class.java)
            intent!!.putExtra("product_id", product!!.id)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnEdit.setOnClickListener {
            intent = Intent(holder.itemView.context, EditProductActivity::class.java)
            intent!!.putExtra("product_id", product!!.id)
            holder.itemView.context.startActivity(intent)
        }

        holder.btnDelete.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Eliminar producto")
                .setMessage("¿Estás seguro de que quieres eliminar este producto?")
                .setPositiveButton("Eliminar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface, which: Int) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                SupabaseClient.client.from("products").delete {
                                    filter {
                                        eq("id", product!!.id)
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    products.removeAt(position)
                                    notifyDataSetChanged()
                                    onProductDeleted.run()
                                    Toast.makeText(holder.itemView.context, "Producto eliminado", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(holder.itemView.context, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    override fun getItemCount(): Int = products.size

    fun setProducts(newProducts: List<MyProductItem>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}