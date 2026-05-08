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
import com.ramos.marketly.adapter.PurchaseAdapter
import com.ramos.marketly.model.PurchaseItem
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MyPurchasesActivity : AppCompatActivity() {

    private lateinit var rvPurchases: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var purchaseAdapter: PurchaseAdapter

    private var currentUserIdStr: String? = null
    private var ordersList: List<OrderWithProduct>? = null
    private var purchasesList: List<PurchaseItem>? = null
    private var sellerIdStr: String? = null
    private var idsList: List<String>? = null
    private var chatIdStr: String? = null

    @Serializable
    data class OrderWithProduct(
        val id: String,
        val product_id: String,
        val amount: Double,
        val status: String,
        val products: ProductInfo?
    )

    @Serializable
    data class ProductInfo(
        val title: String,
        val image_urls: List<String>? = null,
        val seller_id: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_purchases)

        rvPurchases = findViewById(R.id.rvPurchases)
        llEmpty = findViewById(R.id.llEmpty)
        btnBack = findViewById(R.id.btnBack)

        purchaseAdapter = PurchaseAdapter(mutableListOf())
        rvPurchases.layoutManager = LinearLayoutManager(this)
        rvPurchases.adapter = purchaseAdapter

        btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadPurchases()
        }
    }

    private suspend fun loadPurchases() {
        try {
            currentUserIdStr = SessionManager.getUser()?.id ?: return

            ordersList = SupabaseClient.client
                .from("orders")
                .select(Columns.raw("id, product_id, amount, status, products!product_id(title, image_urls, seller_id)")) {
                    filter {
                        eq("buyer_id", currentUserIdStr!!)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<OrderWithProduct>()

            purchasesList = ordersList!!.map { order ->
                sellerIdStr = order.products?.seller_id ?: ""
                idsList = listOf(currentUserIdStr!!, sellerIdStr!!).sorted()
                chatIdStr = "${order.product_id}_${idsList!![0]}_${idsList!![1]}"

                PurchaseItem(
                    orderId = order.id,
                    productId = order.product_id,
                    productTitle = order.products?.title ?: "Producto",
                    productImageUrl = order.products?.image_urls?.firstOrNull(),
                    amount = order.amount,
                    status = order.status,
                    sellerId = sellerIdStr!!,
                    chatId = chatIdStr!!
                )
            }

            runOnUiThread {
                if (purchasesList!!.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rvPurchases.visibility = View.GONE
                } else {
                    llEmpty.visibility = View.GONE
                    rvPurchases.visibility = View.VISIBLE
                    purchaseAdapter.setPurchases(purchasesList!!)
                }
            }

        } catch (e: Exception) {
            Log.e("MyPurchases", " Error: ${e.message}")
            e.printStackTrace()
        }
    }
}