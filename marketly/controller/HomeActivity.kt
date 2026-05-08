package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ramos.marketly.R
import com.ramos.marketly.adapter.ProductAdapter
import com.ramos.marketly.model.Message
import com.ramos.marketly.model.Product
import com.ramos.marketly.model.WalletTransaction
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

class HomeActivity : AppCompatActivity() {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var rvProducts: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var bottomNav: BottomNavigationView
    private var currentOffset = 0
    private val pageSize = 20
    private var isLoading = false
    private var isSearching = false
    private var searchJob: Job? = null

    private var currentUserIdStr: String? = null
    private var nowStr: String? = null
    private var expiredOrders: List<ExpiredOrder>? = null
    private var sellerId: String? = null
    private var productTitle: String? = null
    private var ids: List<String>? = null
    private var chatId: String? = null
    private var sellerData: List<UserBalance>? = null
    private var sellerBalance: Double? = null
    private var productsList: List<Product>? = null
    private var queryStr: String? = null

    @Serializable
    data class ExpiredOrder(
        val id: String,
        val buyer_id: String,
        val product_id: String,
        val amount: Double,
        val delivery_deadline: String,
        val products: ExpiredOrderProduct?
    )

    @Serializable
    data class ExpiredOrderProduct(
        val seller_id: String,
        val title: String
    )

    @Serializable
    data class UserBalance(
        val balance: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rvProducts = findViewById(R.id.rvProducts)
        searchView = findViewById(R.id.searchView)
        bottomNav = findViewById(R.id.bottomNavigationView)

        productAdapter = ProductAdapter(mutableListOf())
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = productAdapter

        rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isSearching && !isLoading && !recyclerView.canScrollVertically(1)) {
                    lifecycleScope.launch {
                        loadProducts(append = true)
                    }
                }
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300)
                    queryStr = newText?.trim() ?: ""
                    if (queryStr!!.isEmpty()) {
                        isSearching = false
                        currentOffset = 0
                        productAdapter.updateProducts(mutableListOf())
                        loadProducts(append = false)
                    } else {
                        isSearching = true
                        searchProducts(queryStr!!)
                    }
                }
                return true
            }
        })

        bottomNav.setOnItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
                if (item.itemId == R.id.nav_home) {
                    return true
                } else if (item.itemId == R.id.nav_vender) {
                    startActivity(Intent(this@HomeActivity, PublishProductActivity::class.java))
                    return true
                } else if (item.itemId == R.id.nav_chats) {
                    startActivity(Intent(this@HomeActivity, ChatsActivity::class.java))
                    return true
                } else if (item.itemId == R.id.nav_perfil) {
                    startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
                    return true
                }
                return false
            }
        })

        lifecycleScope.launch {
            SessionManager.loadUser()
            checkExpiredOrders()
            loadProducts(append = false)
        }
    }

    // Comprueba las ordenes en espera cuyo plazo de 48h ha expirado,
    // transfiere el saldo al vendedor y las marca como completadas
    private suspend fun checkExpiredOrders() {
        try {
            currentUserIdStr = SessionManager.getUser()?.id
            if (currentUserIdStr == null) return
            nowStr = Instant.now().toString()

            expiredOrders = SupabaseClient.client
                .from("orders")
                .select(Columns.raw("id, buyer_id, product_id, amount, delivery_deadline, products!product_id(seller_id, title)")) {
                    filter {
                        eq("buyer_id", currentUserIdStr!!)
                        eq("status", "en_espera")
                        lt("delivery_deadline", nowStr!!)
                    }
                }
                .decodeList<ExpiredOrder>()

            for (order in expiredOrders!!) {
                sellerId = order.products?.seller_id
                if (sellerId == null) continue
                productTitle = order.products!!.title
                ids = listOf(order.buyer_id, sellerId!!).sorted()
                chatId = "${order.product_id}_${ids!![0]}_${ids!![1]}"

                // Obtener saldo actual del vendedor
                sellerData = SupabaseClient.client
                    .from("users")
                    .select(Columns.raw("balance")) {
                        filter {
                            eq("id", sellerId!!)
                        }
                    }
                    .decodeList<UserBalance>()

                sellerBalance = sellerData!!.firstOrNull()?.balance ?: 0.0

                // Transferir saldo al vendedor
                SupabaseClient.client.from("users").update(
                    mapOf("balance" to (sellerBalance!! + order.amount))
                ) {
                    filter {
                        eq("id", sellerId!!)
                    }
                }

                // Marcar orden como completada
                SupabaseClient.client.from("orders").update(
                    mapOf("status" to "completada")
                ) {
                    filter {
                        eq("id", order.id)
                    }
                }

                // Registrar transaccion de venta para el vendedor
                SupabaseClient.client.from("wallet_transactions").insert(
                    WalletTransaction(
                        id = UUID.randomUUID().toString(),
                        userId = sellerId!!,
                        type = "venta",
                        amount = order.amount,
                        description = "Venta: $productTitle"
                    )
                )

                // Enviar mensaje especial al chat indicando cierre automatico
                SupabaseClient.client.from("messages").insert(
                    Message(
                        id = UUID.randomUUID().toString(),
                        productId = order.product_id,
                        orderId = order.id,
                        chatId = chatId,
                        senderId = currentUserIdStr!!,
                        message = "Pedido completado automaticamente",
                        messageType = "order_completed"
                    )
                )

                Log.d("HomeActivity", "Orden cerrada automaticamente: ${order.id}")
            }

        } catch (e: Exception) {
            Log.e("HomeActivity", "Error cerrando ordenes expiradas: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun loadProducts(append: Boolean) {
        if (isLoading) return
        isLoading = true

        try {
            if (!append) currentOffset = 0

            productsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("*, users!seller_id(username, role)")) {
                    filter {
                        eq("status", "activo")
                    }
                    order("created_at", Order.DESCENDING)
                    range(currentOffset.toLong(), (currentOffset + pageSize - 1).toLong())
                }
                .decodeList<Product>()

            Log.d("HomeActivity", "Productos cargados: ${productsList!!.size} offset: $currentOffset")

            currentOffset += productsList!!.size

            runOnUiThread {
                if (append) {
                    productAdapter.appendProducts(productsList!!)
                } else {
                    productAdapter.updateProducts(productsList!!)
                }
            }

        } catch (e: Exception) {
            Log.e("HomeActivity", "Error: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    private suspend fun searchProducts(query: String) {
        try {
            productsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("*, users!seller_id(username, role)")) {
                    filter {
                        eq("status", "activo")
                        ilike("title", "%$query%")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Product>()

            Log.d("HomeActivity", "Resultados busqueda: ${productsList!!.size}")

            runOnUiThread {
                productAdapter.updateProducts(productsList!!)
            }

        } catch (e: Exception) {
            Log.e("HomeActivity", "Error busqueda: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_home
    }
}