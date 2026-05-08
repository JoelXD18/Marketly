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
import com.ramos.marketly.adapter.MyProductAdapter
import com.ramos.marketly.model.MyProductItem
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MyProductsActivity : AppCompatActivity() {

    private lateinit var rvProducts: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var productAdapter: MyProductAdapter

    private var currentUserIdStr: String? = null
    private var productsList: List<ProductData>? = null
    private var itemsList: List<MyProductItem>? = null

    @Serializable
    data class ProductData(
        val id: String,
        val title: String,
        val product_type: String,
        val price: Double,
        val image_urls: List<String>? = null,
        val status: String = "activo"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_products)

        rvProducts = findViewById(R.id.rvProducts)
        llEmpty = findViewById(R.id.llEmpty)
        btnBack = findViewById(R.id.btnBack)

        productAdapter = MyProductAdapter(mutableListOf()) {
            if (productAdapter.itemCount == 0) {
                llEmpty.visibility = View.VISIBLE
                rvProducts.visibility = View.GONE
            }
        }
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = productAdapter

        btnBack.setOnClickListener { finish() }

        lifecycleScope.launch {
            loadProducts()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            loadProducts()
        }
    }

    private suspend fun loadProducts() {
        try {
            currentUserIdStr = SessionManager.getUser()?.id ?: return

            productsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("id, title, product_type, price, image_urls, status")) {
                    filter {
                        eq("seller_id", currentUserIdStr!!)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ProductData>()

            itemsList = productsList!!.map { product ->
                MyProductItem(
                    id = product.id,
                    title = product.title,
                    category = product.product_type,
                    price = product.price,
                    imageUrl = product.image_urls?.firstOrNull(),
                    status = product.status
                )
            }

            runOnUiThread {
                if (itemsList!!.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rvProducts.visibility = View.GONE
                } else {
                    llEmpty.visibility = View.GONE
                    rvProducts.visibility = View.VISIBLE
                    productAdapter.setProducts(itemsList!!)
                }
            }

        } catch (e: Exception) {
            Log.e("MyProducts", " Error: ${e.message}")
            e.printStackTrace()
        }
    }
}