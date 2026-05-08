package com.ramos.marketly.controller

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.ramos.marketly.R
import com.ramos.marketly.adapter.ProductAdapter
import com.ramos.marketly.adapter.RatingAdapter
import com.ramos.marketly.model.Product
import com.ramos.marketly.model.RatingItem
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class UserProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvUserInitial: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvProductCount: TextView
    private lateinit var ivAdminBadge: ImageView
    private lateinit var ivModBadge: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var rvProducts: RecyclerView
    private lateinit var rvRatings: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var ratingAdapter: RatingAdapter

    private var userId: String? = null
    private var ratingsPage = 0
    private val ratingsPageSize = 10
    private var isLoadingRatings = false
    private var allRatingsLoaded = false

    private var layoutManager: LinearLayoutManager? = null
    private var lastVisible: Int? = null
    private var total: Int? = null
    private var uid: String? = null
    private var usersList: List<UserData>? = null
    private var userObj: UserData? = null
    private var productsList: List<Product>? = null
    private var ratingsRawList: List<RatingWithReviewer>? = null
    private var ratingItemsList: MutableList<RatingItem>? = null
    private var avgRating: Double? = null
    private var rounded: String? = null
    private var from: Long? = null
    private var to: Long? = null

    @Serializable
    data class UserData(
        val id: String,
        val username: String,
        val role: String = "cliente"
    )

    @Serializable
    data class RatingWithReviewer(
        val rating: Int,
        val comment: String? = null,
        val users: ReviewerInfo?
    )

    @Serializable
    data class ReviewerInfo(
        val username: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        btnBack = findViewById(R.id.btnBack)
        tvUserInitial = findViewById(R.id.tvUserInitial)
        tvUsername = findViewById(R.id.tvUsername)
        tvRating = findViewById(R.id.tvRating)
        tvProductCount = findViewById(R.id.tvProductCount)
        ivAdminBadge = findViewById(R.id.ivAdminBadge)
        ivModBadge = findViewById(R.id.ivModBadge)
        tabLayout = findViewById(R.id.tabLayout)
        rvProducts = findViewById(R.id.rvProducts)
        rvRatings = findViewById(R.id.rvRatings)

        userId = intent.getStringExtra("user_id")
            ?: run { finish(); return }

        productAdapter = ProductAdapter(mutableListOf())
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        rvProducts.adapter = productAdapter

        ratingAdapter = RatingAdapter(mutableListOf())
        rvRatings.layoutManager = LinearLayoutManager(this)
        rvRatings.adapter = ratingAdapter

        tabLayout.addTab(tabLayout.newTab().setText("Productos"))
        tabLayout.addTab(tabLayout.newTab().setText("Valoraciones"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    rvProducts.visibility = View.VISIBLE
                    rvRatings.visibility = View.GONE
                } else {
                    rvProducts.visibility = View.GONE
                    rvRatings.visibility = View.VISIBLE
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        rvRatings.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                layoutManager = recyclerView.layoutManager as LinearLayoutManager
                lastVisible = layoutManager!!.findLastVisibleItemPosition()
                total = layoutManager!!.itemCount
                if (!isLoadingRatings && !allRatingsLoaded && lastVisible!! >= total!! - 3) {
                    lifecycleScope.launch {
                        loadMoreRatings()
                    }
                }
            }
        })

        btnBack.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                finish()
            }
        })

        lifecycleScope.launch {
            loadProfile()
        }
    }

    private suspend fun loadProfile() {
        try {
            uid = userId ?: return

            usersList = SupabaseClient.client
                .from("users")
                .select(Columns.raw("id, username, role")) {
                    filter {
                        eq("id", uid!!)
                    }
                }
                .decodeList<UserData>()

            userObj = usersList!!.firstOrNull() ?: return

            productsList = SupabaseClient.client
                .from("products")
                .select(Columns.raw("*, users!seller_id(username, role)")) {
                    filter {
                        eq("seller_id", uid!!)
                        eq("status", "activo")
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Product>()

            ratingsRawList = SupabaseClient.client
                .from("ratings")
                .select(Columns.raw("rating, comment, users!reviewer_id(username)")) {
                    filter {
                        eq("reviewed_id", uid!!)
                    }
                    order("created_at", Order.DESCENDING)
                    range(0, (ratingsPageSize - 1).toLong())
                }
                .decodeList<RatingWithReviewer>()

            ratingItemsList = mutableListOf()
            for (r in ratingsRawList!!) {
                ratingItemsList!!.add(
                    RatingItem(
                        rating = r.rating,
                        comment = r.comment,
                        reviewerUsername = r.users?.username ?: "Usuario"
                    )
                )
            }

            if (ratingsRawList!!.size < ratingsPageSize) allRatingsLoaded = true
            ratingsPage = 1

            avgRating = null
            if (ratingItemsList!!.isNotEmpty()) {
                var sum = 0.0
                for (item in ratingItemsList!!) {
                    sum += item.rating
                }
                avgRating = sum / ratingItemsList!!.size
            }

            runOnUiThread {
                tvUserInitial.text = userObj!!.username.firstOrNull()?.uppercase() ?: "?"
                tvUsername.text = userObj!!.username
                tvProductCount.text = "${productsList!!.size} productos"

                if (avgRating != null) {
                    rounded = String.format("%.1f", avgRating)
                    tvRating.text = " $rounded (${ratingItemsList!!.size} valoraciones)"
                } else {
                    tvRating.text = " Sin valoraciones"
                }

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

                productAdapter.updateProducts(productsList!!)
                ratingAdapter.setRatings(ratingItemsList!!)
            }

        } catch (e: Exception) {
            Log.e("UserProfile", " Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun loadMoreRatings() {
        try {
            uid = userId ?: return
            isLoadingRatings = true

            from = (ratingsPage * ratingsPageSize).toLong()
            to = (ratingsPage * ratingsPageSize + ratingsPageSize - 1).toLong()

            ratingsRawList = SupabaseClient.client
                .from("ratings")
                .select(Columns.raw("rating, comment, users!reviewer_id(username)")) {
                    filter {
                        eq("reviewed_id", uid!!)
                    }
                    order("created_at", Order.DESCENDING)
                    range(from!!, to!!)
                }
                .decodeList<RatingWithReviewer>()

            ratingItemsList = mutableListOf()
            for (r in ratingsRawList!!) {
                ratingItemsList!!.add(
                    RatingItem(
                        rating = r.rating,
                        comment = r.comment,
                        reviewerUsername = r.users?.username ?: "Usuario"
                    )
                )
            }

            if (ratingsRawList!!.size < ratingsPageSize) allRatingsLoaded = true
            ratingsPage++

            runOnUiThread {
                ratingAdapter.addRatings(ratingItemsList!!)
            }

        } catch (e: Exception) {
            Log.e("UserProfile", " Error cargando más valoraciones: ${e.message}")
            e.printStackTrace()
        } finally {
            isLoadingRatings = false
        }
    }
}