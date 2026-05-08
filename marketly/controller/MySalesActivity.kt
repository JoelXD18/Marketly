package com.ramos.marketly.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ramos.marketly.R
import com.ramos.marketly.adapter.SaleAdapter
import com.ramos.marketly.model.SaleItem
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MySalesActivity : AppCompatActivity() {

    private lateinit var rvSales: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var saleAdapter: SaleAdapter

    private var currentUserIdStr: String? = null
    private var productsList: List<ProductInfo>? = null
    private var productIdsList: List<String>? = null
    private var productMap: Map<String, ProductInfo>? = null
    private var ordersList: List<OrderWithBuyer>? = null
    private var salesList: List<SaleItem>? = null
    private var prod: ProductInfo? = null
    private var idsList: List<String>? = null
    private var chatIdStr: String? = null

    @Serializable
    data class OrderWithBuyer(
        val id: String,
        val product_id: String,
        val buyer_id: String,
        val amount: Double,
        val status: String,
        val users: BuyerInfo?
    )

    @Serializable
    data class ProductInfo(
        val id: String,
        val title: String,
        val image_urls: List<String>? = null
    )

    @Serializable
    data class BuyerInfo(
        val username: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_sales)

        rvSales = findViewById(R.id.rvSales)
        llEmpty = findViewById(R.id.llEmpty)
        btnBack = findViewById(R.id.btnBack)

        saleAdapter = SaleAdapter(mutableListOf())
        rvSales.layoutManager = LinearLayoutManager(this)
        rvSales.adapter = saleAdapter

        btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadSales()
        }
    }

    private suspend fun loadSales() {
        try {
            currentUserIdStr = SessionManager.getUser()?.id ?: return

            // 1. Obtener los product_ids del vendedor
            productsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("id, title, image_urls")) {
                    filter {
                        eq("seller_id", currentUserIdStr!!)
                    }
                }
                .decodeList<ProductInfo>()

            if (productsList!!.isEmpty()) {
                runOnUiThread {
                    llEmpty.visibility = View.VISIBLE
                    rvSales.visibility = View.GONE
                }
                return
            }

            productIdsList = productsList!!.map { it.id }
            productMap = productsList!!.associateBy { it.id }

            // 2. Obtener órdenes de esos productos
            ordersList = SupabaseClient.client
                .from("orders")
                .select(Columns.raw("id, product_id, buyer_id, amount, status, users!buyer_id(username)")) {
                    filter {
                        isIn("product_id", productIdsList!!)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<OrderWithBuyer>()

            salesList = ordersList!!.map { order ->
                prod = productMap!![order.product_id]
                idsList = listOf(order.buyer_id, currentUserIdStr!!).sorted()
                chatIdStr = "${order.product_id}_${idsList!![0]}_${idsList!![1]}"

                SaleItem(
                    orderId = order.id,
                    productId = order.product_id,
                    productTitle = prod?.title ?: "Producto",
                    productImageUrl = prod?.image_urls?.firstOrNull(),
                    amount = order.amount,
                    status = order.status,
                    buyerId = order.buyer_id,
                    buyerUsername = order.users?.username ?: "Usuario",
                    chatId = chatIdStr!!
                )
            }

            runOnUiThread {
                if (salesList!!.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rvSales.visibility = View.GONE
                } else {
                    llEmpty.visibility = View.GONE
                    rvSales.visibility = View.VISIBLE
                    saleAdapter.setSales(salesList!!)
                }
            }

            Log.d("MySales", "Productos del vendedor: ${productsList!!.size}")
            Log.d("MySales", "Product IDs: $productIdsList")
            Log.d("MySales", "Ordenes encontradas: ${ordersList!!.size}")

        } catch (e: Exception) {
            Log.e("MySales", " Error: ${e.message}")
            e.printStackTrace()
        }


    }
}