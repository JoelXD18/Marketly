package com.ramos.marketly.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ramos.marketly.R
import com.ramos.marketly.adapter.IncidenceAdapter
import com.ramos.marketly.model.IncidencePreview
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ModerationActivity : AppCompatActivity() {

    private lateinit var rvIncidences: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var tvIncidenceCount: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var incidenceAdapter: IncidenceAdapter

    private var incidencesList: List<IncidenceWithData>? = null
    private var previews: List<IncidencePreview>? = null

    @Serializable
    data class IncidenceWithData(
        val id: String,
        val order_id: String,
        val description: String,
        val status: String,
        val created_at: String? = null,
        val orders: OrderData?
    )

    @Serializable
    data class OrderData(
        val amount: Double,
        val buyer_id: String,
        val product_id: String,
        val users: BuyerData?,
        val products: ProductData?
    )

    @Serializable
    data class BuyerData(
        val username: String
    )

    @Serializable
    data class ProductData(
        val title: String,
        val image_urls: List<String>? = null,
        val seller_id: String = "",
        val users: SellerData? = null
    )

    @Serializable
    data class SellerData(
        val username: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moderation)

        rvIncidences = findViewById(R.id.rvIncidences)
        llEmpty = findViewById(R.id.llEmpty)
        tvIncidenceCount = findViewById(R.id.tvIncidenceCount)
        btnBack = findViewById(R.id.btnBack)

        incidenceAdapter = IncidenceAdapter(mutableListOf())
        rvIncidences.layoutManager = LinearLayoutManager(this)
        rvIncidences.adapter = incidenceAdapter

        btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadIncidences()
        }
    }

    private suspend fun loadIncidences() {
        try {
            incidencesList = SupabaseClient.client
                .from("incidences")
                .select(Columns.raw("id, order_id, description, status, created_at, orders!order_id(amount, buyer_id, product_id, users!buyer_id(username), products!product_id(title, image_urls, seller_id, users!seller_id(username)))")) {
                    filter {
                        neq("status", "resuelta")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<IncidenceWithData>()

            previews = incidencesList!!.map { inc ->
                IncidencePreview(
                    id = inc.id,
                    orderId = inc.order_id,
                    productId = inc.orders?.product_id ?: "",
                    productTitle = inc.orders?.products?.title ?: "Producto",
                    productImageUrl = inc.orders?.products?.image_urls?.firstOrNull(),
                    buyerUsername = inc.orders?.users?.username ?: "Usuario",
                    sellerUsername = inc.orders?.products?.users?.username ?: "Vendedor",
                    buyerId = inc.orders?.buyer_id ?: "",
                    sellerId = inc.orders?.products?.seller_id ?: "",
                    description = inc.description,
                    status = inc.status,
                    createdAt = inc.created_at,
                    amount = inc.orders?.amount ?: 0.0
                )
            }

            runOnUiThread {
                if (previews!!.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rvIncidences.visibility = View.GONE
                    tvIncidenceCount.text = "0 incidencias abiertas"
                } else {
                    llEmpty.visibility = View.GONE
                    rvIncidences.visibility = View.VISIBLE
                    tvIncidenceCount.text = "${previews!!.size} incidencias abiertas"
                    incidenceAdapter.setIncidences(previews!!)
                }
            }

        } catch (e: Exception) {
            Log.e("Moderation", " Error: ${e.message}")
            e.printStackTrace()
        }
    }
}