package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.ramos.marketly.model.WalletTransaction


class IncidenceDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var ivProductImage: ImageView
    private lateinit var tvProductTitle: TextView
    private lateinit var tvBuyer: TextView
    private lateinit var tvSeller: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var etResolution: TextInputEditText
    private lateinit var btnFavorBuyer: MaterialButton
    private lateinit var btnFavorSeller: MaterialButton
    private lateinit var btnViewProduct: MaterialButton
    private lateinit var btnViewChat: MaterialButton

    private var orderId: String? = null
    private var incidenceId: String? = null
    private var productId: String? = null
    private var sellerUsername: String? = null
    private var buyerId: String? = null
    private var sellerId: String? = null
    private var productImage: String? = null
    private var amount: Double = 0.0

    private var productTitleStr: String? = null
    private var buyerUsernameStr: String? = null
    private var descriptionStr: String? = null
    private var pid: String? = null
    private var bid: String? = null
    private var sid: String? = null
    private var ids: List<String>? = null
    private var chatIdStr: String? = null
    private var intentObj: Intent? = null
    private var resolutionStr: String? = null
    private var moderatorId: String? = null
    private var oid: String? = null
    private var orders: List<OrderDetail>? = null
    private var order: OrderDetail? = null
    private var buyerIdFromOrder: String? = null
    private var sellerIdFromOrder: String? = null
    private var productTitleFromOrder: String? = null
    private var productIdFromOrder: String? = null
    private var buyerData: List<UserBalance>? = null
    private var buyerBalance: Double? = null
    private var sellerData: List<UserBalance>? = null
    private var sellerBalance: Double? = null
    private var winner: String? = null
    private var extraData: String? = null

    @Serializable
    data class OrderDetail(
        val id: String,
        val buyer_id: String,
        val product_id: String,
        val amount: Double,
        val products: ProductDetail?
    )

    @Serializable
    data class ProductDetail(
        val seller_id: String,
        val title: String = ""
    )

    @Serializable
    data class UserBalance(
        val balance: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incidence_detail)

        btnBack = findViewById(R.id.btnBack)
        ivProductImage = findViewById(R.id.ivProductImage)
        tvProductTitle = findViewById(R.id.tvProductTitle)
        tvBuyer = findViewById(R.id.tvBuyer)
        tvSeller = findViewById(R.id.tvSeller)
        tvAmount = findViewById(R.id.tvAmount)
        tvDescription = findViewById(R.id.tvDescription)
        etResolution = findViewById(R.id.etResolution)
        btnFavorBuyer = findViewById(R.id.btnFavorBuyer)
        btnFavorSeller = findViewById(R.id.btnFavorSeller)
        btnViewProduct = findViewById(R.id.btnViewProduct)
        btnViewChat = findViewById(R.id.btnViewChat)

        incidenceId = intent.getStringExtra("incidence_id")
        orderId = intent.getStringExtra("order_id")
        productId = intent.getStringExtra("product_id")
        amount = intent.getDoubleExtra("amount", 0.0)
        buyerId = intent.getStringExtra("buyer_id")
        sellerId = intent.getStringExtra("seller_id")
        sellerUsername = intent.getStringExtra("seller_username")
        productImage = intent.getStringExtra("product_image")

        productTitleStr = intent.getStringExtra("product_title") ?: ""
        buyerUsernameStr = intent.getStringExtra("buyer_username") ?: ""
        descriptionStr = intent.getStringExtra("description") ?: ""

        tvProductTitle.text = productTitleStr
        tvBuyer.text = buyerUsernameStr
        tvSeller.text = sellerUsername ?: ""
        tvDescription.text = descriptionStr
        tvAmount.text = "$amount "

        if (!productImage.isNullOrEmpty()) {
            Glide.with(this)
                .load(productImage)
                .centerCrop()
                .into(ivProductImage)
        }

        btnBack.setOnClickListener { finish() }

        btnViewProduct.setOnClickListener {
            if (productId != null) {
                val intent = Intent(this, ProductDetailActivity::class.java)
                intent.putExtra("product_id", productId)
                startActivity(intent)
            }
        }

        btnViewChat.setOnClickListener {
            pid = productId ?: return@setOnClickListener
            bid = buyerId ?: return@setOnClickListener
            sid = sellerId ?: return@setOnClickListener

            ids = listOf(bid!!, sid!!).sorted()
            chatIdStr = "${pid}_${ids!![0]}_${ids!![1]}"

            intentObj = Intent(this, ChatActivity::class.java)
            intentObj!!.putExtra("product_id", pid)
            intentObj!!.putExtra("product_title", productTitleStr)
            intentObj!!.putExtra("product_image", productImage)
            intentObj!!.putExtra("chat_id", chatIdStr)
            intentObj!!.putExtra("seller_id", sid)
            startActivity(intentObj)

            Log.d("IncidenceDetail", "chat_id generado: $chatIdStr")
            Log.d("IncidenceDetail", "buyerId: $bid")
            Log.d("IncidenceDetail", "sellerId: $sid")
            Log.d("IncidenceDetail", "productId: $pid")
        }

        btnFavorBuyer.setOnClickListener {
            resolutionStr = etResolution.text.toString().trim()
            if (resolutionStr!!.isEmpty()) {
                Toast.makeText(this, "Escribe la resolución antes de confirmar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Dar la razón al comprador")
                .setMessage("Se reembolsará $amount  al comprador. ¿Confirmas?")
                .setPositiveButton("Confirmar") { _, _ ->
                    lifecycleScope.launch {
                        resolveIncidence(favorBuyer = true, resolution = resolutionStr!!)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnFavorSeller.setOnClickListener {
            resolutionStr = etResolution.text.toString().trim()
            if (resolutionStr!!.isEmpty()) {
                Toast.makeText(this, "Escribe la resolución antes de confirmar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            AlertDialog.Builder(this)
                .setTitle("Dar la razón al vendedor")
                .setMessage("Se transferirán $amount  al vendedor. ¿Confirmas?")
                .setPositiveButton("Confirmar") { _, _ ->
                    lifecycleScope.launch {
                        resolveIncidence(favorBuyer = false, resolution = resolutionStr!!)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private suspend fun resolveIncidence(favorBuyer: Boolean, resolution: String) {
        try {
            btnFavorBuyer.isEnabled = false
            btnFavorSeller.isEnabled = false

            moderatorId = SessionManager.getUser()?.id ?: throw Exception("No hay usuario autenticado")
            oid = orderId ?: throw Exception("No hay orden")

            orders = SupabaseClient.client
                .from("orders")
                .select(Columns.raw("id, buyer_id, product_id, amount, products!product_id(seller_id, title)")) {
                    filter {
                        eq("id", oid!!)
                    }
                }
                .decodeList<OrderDetail>()

            order = orders!!.firstOrNull() ?: throw Exception("Orden no encontrada")
            buyerIdFromOrder = order!!.buyer_id
            sellerIdFromOrder = order!!.products?.seller_id ?: throw Exception("Vendedor no encontrado")
            productTitleFromOrder = order!!.products?.title ?: "Producto"
            productIdFromOrder = order!!.product_id

            if (favorBuyer) {
                buyerData = SupabaseClient.client
                    .from("users")
                    .select(Columns.raw("balance")) {
                        filter { eq("id", buyerIdFromOrder!!) }
                    }
                    .decodeList<UserBalance>()

                buyerBalance = buyerData!!.firstOrNull()?.balance ?: 0.0
                SupabaseClient.client.from("users").update(
                    mapOf("balance" to (buyerBalance!! + amount))
                ) {
                    filter { eq("id", buyerIdFromOrder!!) }
                }

                SupabaseClient.client.from("wallet_transactions").insert(
                    WalletTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = buyerIdFromOrder!!,
                        type = "reembolso",
                        amount = amount,
                        description = "Reembolso: $productTitleFromOrder"
                    )
                )

                SupabaseClient.client.from("orders").update(
                    mapOf("status" to "reembolsada")
                ) {
                    filter { eq("id", oid!!) }
                }

            } else {
                sellerData = SupabaseClient.client
                    .from("users")
                    .select(Columns.raw("balance")) {
                        filter { eq("id", sellerIdFromOrder!!) }
                    }
                    .decodeList<UserBalance>()

                sellerBalance = sellerData!!.firstOrNull()?.balance ?: 0.0
                SupabaseClient.client.from("users").update(
                    mapOf("balance" to (sellerBalance!! + amount))
                ) {
                    filter { eq("id", sellerIdFromOrder!!) }
                }

                SupabaseClient.client.from("wallet_transactions").insert(
                    WalletTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        userId = sellerIdFromOrder!!,
                        type = "venta",
                        amount = amount,
                        description = "Venta: $productTitleFromOrder"
                    )
                )

                SupabaseClient.client.from("orders").update(
                    mapOf("status" to "completada")
                ) {
                    filter { eq("id", oid!!) }
                }
            }

            SupabaseClient.client.from("incidences").update(
                mapOf(
                    "status" to "resuelta",
                    "resolved_by" to moderatorId!!,
                    "resolution" to resolution
                )
            ) {
                filter { eq("id", incidenceId!!) }
            }

            // Enviar mensaje especial al chat
            winner = if (favorBuyer) "comprador" else "vendedor"
            extraData = "Resolución: $resolution\nGanador: $winner"
            ids = listOf(buyerIdFromOrder!!, sellerIdFromOrder!!).sorted()
            chatIdStr = "${productIdFromOrder}_${ids!![0]}_${ids!![1]}"

            SupabaseClient.client.from("messages").insert(
                com.ramos.marketly.model.Message(
                    id = java.util.UUID.randomUUID().toString(),
                    productId = productIdFromOrder,
                    orderId = oid,
                    chatId = chatIdStr,
                    senderId = moderatorId!!,
                    message = "Incidencia resuelta",
                    messageType = "incidence_resolved",
                    extraData = extraData
                )
            )

            Log.d("IncidenceDetail", " Incidencia resuelta")
            runOnUiThread {
                Toast.makeText(this, "Incidencia resuelta correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }

        } catch (e: Exception) {
            Log.e("IncidenceDetail", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnFavorBuyer.isEnabled = true
                btnFavorSeller.isEnabled = true
            }
        }
    }
}