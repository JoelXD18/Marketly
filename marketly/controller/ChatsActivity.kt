package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ramos.marketly.R
import com.ramos.marketly.adapter.ChatAdapter
import com.ramos.marketly.model.ChatPreview
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class ChatsActivity : AppCompatActivity() {

    private lateinit var rvChats: RecyclerView
    private lateinit var chatAdapter: ChatAdapter

    private var currentUserIdStr: String? = null
    private var allUsers: List<UserInfo>? = null
    private var userMap: Map<String, UserInfo>? = null
    private var sentMessages: List<MessageWithProduct>? = null
    private var receivedMessages: List<MessageWithProduct>? = null
    private var allMessages: List<MessageWithProduct>? = null
    private var chatMap: LinkedHashMap<String, MessageWithProduct>? = null
    private var cid: String? = null
    private var isSender: Boolean? = null
    private var isSeller: Boolean? = null
    private var chatPreviews: List<ChatPreview>? = null
    private var parts: List<String>? = null
    private var otherUserId: String? = null
    private var uid1: String? = null
    private var uid2: String? = null
    private var otherUser: UserInfo? = null
    private var otherUsername: String? = null
    private var otherUserRole: String? = null

    @Serializable
    data class MessageWithProduct(
        val chat_id: String? = null,
        val product_id: String?,
        val message: String,
        val sender_id: String,
        val created_at: String? = null,
        val products: ProductInfo?
    )

    @Serializable
    data class ProductInfo(
        val title: String,
        val image_urls: List<String>?,
        val seller_id: String,
        val users: SellerInfo?
    )

    @Serializable
    data class SellerInfo(
        val username: String,
        val role: String = "cliente"
    )

    @Serializable
    data class UserInfo(
        val id: String,
        val username: String,
        val role: String = "cliente"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        rvChats = findViewById(R.id.rvChats)
        chatAdapter = ChatAdapter(mutableListOf())
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = chatAdapter

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.nav_chats
        bottomNav.setOnItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
                if (item.itemId == R.id.nav_home) {
                    startActivity(Intent(this@ChatsActivity, HomeActivity::class.java))
                    finish()
                    return true
                } else if (item.itemId == R.id.nav_vender) {
                    startActivity(Intent(this@ChatsActivity, PublishProductActivity::class.java))
                    return true
                } else if (item.itemId == R.id.nav_chats) {
                    return true
                } else if (item.itemId == R.id.nav_perfil) {
                    startActivity(Intent(this@ChatsActivity, ProfileActivity::class.java))
                    return true
                }
                return false
            }
        })

        lifecycleScope.launch {
            loadChats()
        }
    }

    private suspend fun loadChats() {
        try {
            currentUserIdStr = SessionManager.getUser()?.id
            if (currentUserIdStr == null) return

            allUsers = SupabaseClient.client
                .from("users")
                .select(Columns.raw("id, username, role"))
                .decodeList<UserInfo>()

            userMap = allUsers!!.associateBy { it.id }

            sentMessages = SupabaseClient.client
                .from("messages")
                .select(Columns.raw("chat_id, product_id, message, sender_id, created_at, products!product_id(title, image_urls, seller_id, users!seller_id(username, role))")) {
                    filter {
                        eq("sender_id", currentUserIdStr!!)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<MessageWithProduct>()

            receivedMessages = SupabaseClient.client
                .from("messages")
                .select(Columns.raw("chat_id, product_id, message, sender_id, created_at, products!product_id(title, image_urls, seller_id, users!seller_id(username, role))")) {
                    filter {
                        eq("products.seller_id", currentUserIdStr!!)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<MessageWithProduct>()

            allMessages = (sentMessages!! + receivedMessages!!)
                .sortedByDescending { it.created_at }

            chatMap = linkedMapOf()
            for (msg in allMessages!!) {
                cid = msg.chat_id
                if (cid == null) continue
                isSender = msg.sender_id == currentUserIdStr
                isSeller = msg.products?.seller_id == currentUserIdStr
                if ((isSender!! || isSeller!!) && !chatMap!!.containsKey(cid!!)) {
                    chatMap!![cid!!] = msg
                }
            }

            chatPreviews = chatMap!!.entries.map { (chatId, msg) ->
                parts = chatId.split("_")
                otherUserId = if (parts!!.size >= 3) {
                    uid1 = parts!![parts!!.size - 2]
                    uid2 = parts!![parts!!.size - 1]
                    if (uid1 == currentUserIdStr) uid2 else uid1
                } else ""

                otherUser = userMap!![otherUserId!!]
                otherUsername = otherUser?.username ?: ""
                otherUserRole = otherUser?.role ?: "cliente"

                ChatPreview(
                    productId = msg.product_id ?: "",
                    productTitle = msg.products?.title ?: "Producto",
                    productImageUrl = msg.products?.image_urls?.firstOrNull(),
                    lastMessage = msg.message,
                    sellerId = msg.products?.seller_id ?: "",
                    sellerUsername = msg.products?.users?.username ?: "",
                    otherUsername = otherUsername!!,
                    otherUserRole = otherUserRole!!,
                    chatId = chatId,
                    otherUserId = otherUserId!!
                )
            }

            Log.d("ChatsActivity", "Chats cargados: ${chatPreviews!!.size}")

            runOnUiThread {
                chatAdapter.setChats(chatPreviews!!)
            }

        } catch (e: Exception) {
            Log.e("ChatsActivity", " Error: ${e.message}")
            e.printStackTrace()
        }
    }
}