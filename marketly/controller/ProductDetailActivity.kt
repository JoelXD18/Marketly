package com.ramos.marketly.controller

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.ramos.marketly.R
import com.ramos.marketly.model.Message
import com.ramos.marketly.model.Order
import com.ramos.marketly.model.Product
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import com.ramos.marketly.model.WalletTransaction

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var viewPagerImages: ViewPager2
    private lateinit var llIndicators: LinearLayout
    private lateinit var tvCategory: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvSellerInitial: TextView
    private lateinit var tvSellerName: TextView
    private lateinit var tvSellerRating: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnChat: MaterialButton
    private lateinit var btnBuy: MaterialButton
    private lateinit var btnEdit: MaterialButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var ivSellerAdminBadge: ImageView
    private lateinit var ivSellerModBadge: ImageView
    private lateinit var tvSoldOut: TextView

    private var product: Product? = null

    private var productIdStr: String? = null
    private var intentObj: Intent? = null
    private var sellerIdStr: String? = null
    private var ratingsList: List<RatingResult>? = null
    private var avgRating: Double? = null
    private var rounded: String? = null
    private var sellerUsername: String? = null
    private var sellerRole: String? = null
    private var imagesList: List<String>? = null
    private var currentUserId: String? = null
    private var prod: Product? = null
    private var dot: View? = null
    private var params: LinearLayout.LayoutParams? = null
    private var currentUser: com.ramos.marketly.model.User? = null
    private var orderIdStr: String? = null
    private var deadlineStr: String? = null
    private var idsList: List<String>? = null
    private var chatIdStr: String? = null

    @Serializable
    data class RatingResult(val rating: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        viewPagerImages = findViewById(R.id.viewPagerImages)
        llIndicators = findViewById(R.id.llIndicators)
        tvCategory = findViewById(R.id.tvCategory)
        tvTitle = findViewById(R.id.tvTitle)
        tvPrice = findViewById(R.id.tvPrice)
        tvSellerInitial = findViewById(R.id.tvSellerInitial)
        tvSellerName = findViewById(R.id.tvSellerName)
        tvSellerRating = findViewById(R.id.tvSellerRating)
        tvDescription = findViewById(R.id.tvDescription)
        btnBack = findViewById(R.id.btnBack)
        btnChat = findViewById(R.id.btnChat)
        btnBuy = findViewById(R.id.btnBuy)
        btnEdit = findViewById(R.id.btnEdit)
        btnDelete = findViewById(R.id.btnDelete)
        ivSellerAdminBadge = findViewById(R.id.ivSellerAdminBadge)
        ivSellerModBadge = findViewById(R.id.ivSellerModBadge)
        tvSoldOut = findViewById(R.id.tvSoldOut)

        productIdStr = intent.getStringExtra("product_id")
            ?: run { finish(); return }

        btnBack.setOnClickListener { finish() }

        btnChat.setOnClickListener {
            intentObj = Intent(this, ChatActivity::class.java)
            intentObj!!.putExtra("product_id", product?.id)
            intentObj!!.putExtra("seller_id", product?.sellerId)
            intentObj!!.putExtra("product_title", product?.title)
            intentObj!!.putExtra("product_seller", product?.seller?.username)
            intentObj!!.putExtra("product_price", product?.price.toString())
            intentObj!!.putExtra("product_image", product?.imageUrls?.firstOrNull())
            startActivity(intentObj)
        }

        btnBuy.setOnClickListener {
            lifecycleScope.launch {
                buyProduct()
            }
        }

        btnEdit.setOnClickListener {
            intentObj = Intent(this, EditProductActivity::class.java)
            intentObj!!.putExtra("product_id", product?.id)
            startActivity(intentObj)
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Estás seguro de que quieres eliminar este producto? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        lifecycleScope.launch {
                            deleteProduct()
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show()
        }

        lifecycleScope.launch {
            loadProduct(productIdStr!!)
        }
    }

    private suspend fun loadProduct(productId: String) {
        try {
            product = SupabaseClient.client
                .from("products")
                .select(Columns.raw("*, users!seller_id(username, role)")) {
                    filter {
                        eq("id", productId)
                    }
                }
                .decodeSingle<Product>()

            // Cargar valoraciones del vendedor
            sellerIdStr = product?.sellerId ?: ""
            ratingsList = SupabaseClient.client
                .from("ratings")
                .select(Columns.raw("rating")) {
                    filter {
                        eq("reviewed_id", sellerIdStr!!)
                    }
                }
                .decodeList<RatingResult>()

            avgRating = if (ratingsList!!.isEmpty()) null
            else ratingsList!!.map { it.rating }.average()

            runOnUiThread {
                product?.let { displayProduct(it) }
                if (avgRating != null) {
                    rounded = String.format("%.1f", avgRating)
                    tvSellerRating.text = " $rounded (${ratingsList!!.size} valoraciones)"
                } else {
                    tvSellerRating.text = " Sin valoraciones"
                }
            }

        } catch (e: Exception) {
            Log.e("ProductDetail", " Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun displayProduct(product: Product) {
        tvCategory.text = product.productType
        tvTitle.text = product.title
        tvPrice.text = "${product.price} "
        tvDescription.text = product.description

        sellerUsername = product.seller?.username ?: "Desconocido"
        tvSellerName.text = sellerUsername
        tvSellerName.setOnClickListener(object : android.view.View.OnClickListener {
            override fun onClick(v: android.view.View?) {
                intentObj = Intent(this@ProductDetailActivity, UserProfileActivity::class.java)
                intentObj!!.putExtra("user_id", product.sellerId)
                startActivity(intentObj)
            }
        })
        tvSellerInitial.text = sellerUsername!!.firstOrNull()?.uppercase() ?: "?"

        sellerRole = product.seller?.role ?: "cliente"
        if (sellerRole == "administrador") {
            ivSellerAdminBadge.visibility = View.VISIBLE
            ivSellerModBadge.visibility = View.GONE
        } else if (sellerRole == "moderador") {
            ivSellerModBadge.visibility = View.VISIBLE
            ivSellerAdminBadge.visibility = View.GONE
        } else {
            ivSellerAdminBadge.visibility = View.GONE
            ivSellerModBadge.visibility = View.GONE
        }

        imagesList = product.imageUrls ?: emptyList()
        viewPagerImages.adapter = ImageCarouselAdapter(imagesList!!)

        setupIndicators(imagesList!!.size)
        viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
            }
        })

        currentUserId = SessionManager.getUser()?.id
        if (product.sellerId == currentUserId) {
            btnBuy.visibility = View.GONE
            btnChat.visibility = View.GONE
            tvSoldOut.visibility = View.GONE
            if (product.status == "vendido") {
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE
                tvSoldOut.visibility = View.VISIBLE
                tvSoldOut.text = " Vendido"
            } else {
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE
            }
        } else {
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
            if (product.status == "vendido") {
                btnBuy.visibility = View.GONE
                btnChat.visibility = View.GONE
                tvSoldOut.visibility = View.VISIBLE
            } else {
                btnBuy.visibility = View.VISIBLE
                btnChat.visibility = View.VISIBLE
                tvSoldOut.visibility = View.GONE
            }
        }
    }

    private suspend fun deleteProduct() {
        try {
            prod = product ?: return
            SupabaseClient.client.from("products").delete {
                filter {
                    eq("id", prod!!.id)
                }
            }
            Log.d("ProductDetail", " Producto eliminado")
            runOnUiThread {
                Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Log.e("ProductDetail", " Error eliminando: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupIndicators(count: Int) {
        llIndicators.removeAllViews()
        if (count <= 1) return
        for (i in 0 until count) {
            dot = View(this)
            params = LinearLayout.LayoutParams(8.dpToPx(), 8.dpToPx())
            params!!.marginEnd = 4.dpToPx()
            dot!!.layoutParams = params
            dot!!.setBackgroundResource(if (i == 0) R.drawable.indicator_active else R.drawable.indicator_inactive)
            llIndicators.addView(dot)
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until llIndicators.childCount) {
            llIndicators.getChildAt(i).setBackgroundResource(
                if (i == position) R.drawable.indicator_active else R.drawable.indicator_inactive
            )
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private suspend fun buyProduct() {
        currentUser = SessionManager.getUser()
            ?: throw Exception("No hay usuario autenticado")
        prod = product
            ?: throw Exception("Producto no cargado")
        orderIdStr = UUID.randomUUID().toString()
        deadlineStr = Instant.now().plus(48, ChronoUnit.HOURS).toString()
        idsList = listOf(currentUser!!.id, prod!!.sellerId).sorted()
        chatIdStr = "${prod!!.id}_${idsList!![0]}_${idsList!![1]}"

        try {
            if (currentUser!!.balance < prod!!.price) {
                runOnUiThread {
                    Toast.makeText(this, "Saldo insuficiente", Toast.LENGTH_SHORT).show()
                }
                return
            }

            SupabaseClient.client.from("orders").insert(
                Order(
                    id = orderIdStr!!,
                    productId = prod!!.id,
                    buyerId = currentUser!!.id,
                    status = "en_espera",
                    amount = prod!!.price,
                    deliveryDeadline = deadlineStr!!
                )
            )

            SupabaseClient.client.from("users").update(
                mapOf("balance" to (currentUser!!.balance - prod!!.price))
            ) {
                filter {
                    eq("id", currentUser!!.id)
                }
            }

            SupabaseClient.client.from("wallet_transactions").insert(
                WalletTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser!!.id,
                    type = "compra",
                    amount = prod!!.price,
                    description = "Compra: ${prod!!.title}"
                )
            )

            SupabaseClient.client.from("products").update(
                mapOf("status" to "vendido")
            ) {
                filter {
                    eq("id", prod!!.id)
                }
            }

            // Mensaje automatico de compra
            SupabaseClient.client.from("messages").insert(
                Message(
                    id = UUID.randomUUID().toString(),
                    productId = prod!!.id,
                    orderId = orderIdStr!!,
                    chatId = chatIdStr!!,
                    senderId = currentUser!!.id,
                    message = "He comprado tu producto! Mi pedido es #${orderIdStr!!.take(8)}"
                )
            )

            // Mensaje recordatorio con el plazo de 48h para confirmar o abrir incidencia
            SupabaseClient.client.from("messages").insert(
                Message(
                    id = UUID.randomUUID().toString(),
                    productId = prod!!.id,
                    orderId = orderIdStr!!,
                    chatId = chatIdStr!!,
                    senderId = currentUser!!.id,
                    message = "Recordatorio de plazo",
                    messageType = "order_reminder",
                    extraData = deadlineStr
                )
            )

            // Si el producto tiene archivo digital, enviarlo automaticamente al chat
            if (!prod!!.fileUrl.isNullOrEmpty()) {
                SupabaseClient.client.from("messages").insert(
                    Message(
                        id = UUID.randomUUID().toString(),
                        productId = prod!!.id,
                        orderId = orderIdStr!!,
                        chatId = chatIdStr!!,
                        senderId = prod!!.sellerId,
                        message = prod!!.fileUrl!!,
                        messageType = "file"
                    )
                )
            }

            Log.d("ProductDetail", "Compra realizada - Order ID: $orderIdStr")
            runOnUiThread {
                Toast.makeText(this, "Compra realizada!", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("ProductDetail", "Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class ImageCarouselAdapter(private val images: List<String>) :
        RecyclerView.Adapter<ImageCarouselAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.ivCarouselImage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carousel_image, parent, false)
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            Glide.with(holder.itemView.context)
                .load(images[position])
                .centerCrop()
                .into(holder.imageView)
        }

        override fun getItemCount(): Int = images.size
    }
}