package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ramos.marketly.R
import com.ramos.marketly.utils.PermissionManager
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUserInitial: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvSalesCount: TextView
    private lateinit var tvPurchasesCount: TextView
    private lateinit var llMyProducts: LinearLayout
    private lateinit var llMySales: LinearLayout
    private lateinit var llMyPurchases: LinearLayout
    private lateinit var llMyWallet: LinearLayout
    private lateinit var llModeration: LinearLayout
    private lateinit var cardModeration: CardView
    private lateinit var llLogout: LinearLayout
    private lateinit var ivAdminBadge: ImageView
    private lateinit var ivModBadge: ImageView

    private var bottomNav: BottomNavigationView? = null
    private var userObj: com.ramos.marketly.model.User? = null
    private var purchasesList: List<kotlinx.serialization.json.JsonObject>? = null
    private var myProductsList: List<kotlinx.serialization.json.JsonObject>? = null
    private var salesCountInt: Int? = null
    private var productIdsList: List<String>? = null
    private var salesList: List<kotlinx.serialization.json.JsonObject>? = null
    private var intentObj: Intent? = null

    @Serializable
    data class OrderCount(val count: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        tvUserInitial = findViewById(R.id.tvUserInitial)
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        tvBalance = findViewById(R.id.tvBalance)
        tvSalesCount = findViewById(R.id.tvSalesCount)
        tvPurchasesCount = findViewById(R.id.tvPurchasesCount)
        llMyProducts = findViewById(R.id.llMyProducts)
        llMySales = findViewById(R.id.llMySales)
        llMyPurchases = findViewById(R.id.llMyPurchases)
        llMyWallet = findViewById(R.id.llMyWallet)
        llModeration = findViewById(R.id.llModeration)
        cardModeration = findViewById(R.id.cardModeration)
        llLogout = findViewById(R.id.llLogout)
        ivAdminBadge = findViewById(R.id.ivAdminBadge)
        ivModBadge = findViewById(R.id.ivModBadge)

        bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav!!.selectedItemId = R.id.nav_perfil
        bottomNav!!.setOnItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
                if (item.itemId == R.id.nav_home) {
                    startActivity(Intent(this@ProfileActivity, HomeActivity::class.java))
                    finish()
                    return true
                } else if (item.itemId == R.id.nav_vender) {
                    startActivity(Intent(this@ProfileActivity, PublishProductActivity::class.java))
                    return true
                } else if (item.itemId == R.id.nav_chats) {
                    startActivity(Intent(this@ProfileActivity, ChatsActivity::class.java))
                    finish()
                    return true
                } else if (item.itemId == R.id.nav_perfil) {
                    return true
                }
                return false
            }
        })

        // Cargar datos del usuario
        userObj = SessionManager.getUser()
        if (userObj != null) {
            tvUserInitial.text = userObj!!.username.firstOrNull()?.uppercase() ?: "?"
            tvUsername.text = userObj!!.username
            tvEmail.text = userObj!!.email
            tvBalance.text = "${userObj!!.balance}  disponibles"

            // Mostrar badge según rol
            if (userObj!!.role == "administrador") {
                ivAdminBadge.visibility = View.VISIBLE
                ivModBadge.visibility = View.GONE
            } else if (userObj!!.role == "moderador") {
                ivModBadge.visibility = View.VISIBLE
                ivAdminBadge.visibility = View.GONE
            } else {
                ivAdminBadge.visibility = View.GONE
                ivModBadge.visibility = View.GONE
            }
        }

        // Mostrar panel moderación si es moderador o admin
        if (PermissionManager.canAccessModeratorPanel()) {
            cardModeration.visibility = View.VISIBLE
        }

        llMyProducts.setOnClickListener {
            startActivity(Intent(this, MyProductsActivity::class.java))
        }

        llMySales.setOnClickListener {
            startActivity(Intent(this, MySalesActivity::class.java))
        }

        llMyPurchases.setOnClickListener {
            startActivity(Intent(this, MyPurchasesActivity::class.java))
        }

        llMyWallet.setOnClickListener {
            startActivity(Intent(this, MyWalletActivity::class.java))
        }

        llModeration.setOnClickListener {
            startActivity(Intent(this, ModerationActivity::class.java))
        }

        llLogout.setOnClickListener {
            lifecycleScope.launch {
                logout()
            }
        }

        lifecycleScope.launch {
            loadCounts()
        }
    }

    private suspend fun loadCounts() {
        try {
            userObj = SessionManager.getUser() ?: return

            // Compras
            purchasesList = SupabaseClient.client
                .from("orders")
                .select(Columns.ALL) {
                    filter {
                        eq("buyer_id", userObj!!.id)
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            // Ventas  primero obtener productos del vendedor
            myProductsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("id")) {
                    filter {
                        eq("seller_id", userObj!!.id)
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            salesCountInt = if (myProductsList!!.isEmpty()) {
                0
            } else {
                productIdsList = myProductsList!!.mapNotNull {
                    it["id"]?.toString()?.trim('"')
                }
                salesList = SupabaseClient.client
                    .from("orders")
                    .select(Columns.ALL) {
                        filter {
                            isIn("product_id", productIdsList!!)
                        }
                    }
                    .decodeList<kotlinx.serialization.json.JsonObject>()
                salesList!!.size
            }

            runOnUiThread {
                tvPurchasesCount.text = "${purchasesList!!.size} pedidos"
                tvSalesCount.text = "$salesCountInt pedidos"
            }

        } catch (e: Exception) {
            Log.e("Profile", " Error cargando contadores: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun logout() {
        try {
            SupabaseClient.client.auth.signOut()
            SessionManager.clear()
            runOnUiThread {
                intentObj = Intent(this, MainActivity::class.java)
                intentObj!!.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intentObj)
            }
        } catch (e: Exception) {
            Log.e("Profile", " Error cerrando sesión: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}