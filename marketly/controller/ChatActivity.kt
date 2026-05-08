package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.adapter.MessageAdapter
import com.ramos.marketly.model.Message
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order as SupabaseOrder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import com.ramos.marketly.model.WalletTransaction

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnViewProduct: ImageButton
    private lateinit var tvChatTitle: TextView
    private lateinit var tvChatSubtitle: TextView
    private lateinit var tvChatPrice: TextView
    private lateinit var ivProductImage: ImageView
    private lateinit var actionBar: LinearLayout
    private lateinit var btnAllGood: MaterialButton
    private lateinit var btnOpenIncidence: MaterialButton
    private lateinit var messageAdapter: MessageAdapter

    private var productId: String? = null
    private var orderId: String? = null
    private var sellerId: String? = null
    private var currentUserId: String = ""
    private var activeOrderId: String? = null
    private var chatId: String? = null

    private val userRoles = mutableMapOf<String, String>()
    private val usernames = mutableMapOf<String, String>()

    private var intentChatId: String? = null
    private var otherUserId: String? = null
    private var productTitle: String? = null
    private var productSeller: String? = null
    private var productPrice: String? = null
    private var productImage: String? = null
    private var ids: List<String>? = null
    private var isBuyer: Boolean? = null
    private var buyerIdForAdapter: String? = null
    private var sellerIdForAdapter: String? = null
    private var users: List<UserInfo>? = null
    private var orders: List<OrderResult>? = null
    private var localOrderId: String? = null
    private var currentUser: com.ramos.marketly.model.User? = null
    private var prod: String? = null
    private var order: OrderResult? = null
    private var amount: Double? = null
    private var products: List<SellerIdResult>? = null
    private var sellerIdResult: String? = null
    private var sellerUser: List<BalanceResult>? = null
    private var sellerBalance: Double? = null
    private var messagesList: List<Message>? = null
    private var messageId: String? = null
    private var newMessage: Message? = null
    private var cid: String? = null
    private var channelName: String? = null
    private var channel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var senderId: String? = null

    @Serializable
    data class OrderResult(
        val id: String,
        val status: String,
        val amount: Double,
        val buyer_id: String,
        val product_id: String? = null,
        val delivery_deadline: String? = null,
        val created_at: String? = null,
        val completed_at: String? = null
    )

    @Serializable
    data class UserInfo(
        val id: String,
        val username: String,
        val role: String = "cliente"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnViewProduct = findViewById(R.id.btnViewProduct)
        tvChatTitle = findViewById(R.id.tvChatTitle)
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle)
        tvChatPrice = findViewById(R.id.tvChatPrice)
        ivProductImage = findViewById(R.id.ivProductImage)
        actionBar = findViewById(R.id.actionBar)
        btnAllGood = findViewById(R.id.btnAllGood)
        btnOpenIncidence = findViewById(R.id.btnOpenIncidence)

        productId = intent.getStringExtra("product_id")
        orderId = intent.getStringExtra("order_id")
        sellerId = intent.getStringExtra("seller_id")
        intentChatId = intent.getStringExtra("chat_id")
        otherUserId = intent.getStringExtra("other_user_id")

        productTitle = intent.getStringExtra("product_title")
        productSeller = intent.getStringExtra("product_seller")
        productPrice = intent.getStringExtra("product_price")
        productImage = intent.getStringExtra("product_image")

        if (!productTitle.isNullOrEmpty()) tvChatTitle.text = productTitle
        if (!productSeller.isNullOrEmpty()) tvChatSubtitle.text = productSeller
        if (!productPrice.isNullOrEmpty() && productPrice != "null") {
            tvChatPrice.text = "$productPrice "
        }
        if (!productImage.isNullOrEmpty() && productImage != "null") {
            Glide.with(this)
                .load(productImage)
                .centerCrop()
                .into(ivProductImage)
        }

        currentUserId = SessionManager.getUser()?.id ?: ""

        // Si viene chat_id del intent usarlo directamente
        // Si no, generarlo usando otherUserId o sellerId
        if (!intentChatId.isNullOrEmpty()) {
            chatId = intentChatId
        } else if (productId != null && !otherUserId.isNullOrEmpty()) {
            ids = listOf(currentUserId, otherUserId!!).sorted()
            chatId = "${productId}_${ids!![0]}_${ids!![1]}"
        } else if (productId != null && !sellerId.isNullOrEmpty()) {
            ids = listOf(currentUserId, sellerId!!).sorted()
            chatId = "${productId}_${ids!![0]}_${ids!![1]}"
        } else {
            chatId = null
        }

        isBuyer = currentUserId != sellerId
        buyerIdForAdapter = if (isBuyer!!) currentUserId else ""
        sellerIdForAdapter = sellerId ?: ""

        messageAdapter = MessageAdapter(
            mutableListOf(),
            currentUserId,
            userRoles,
            usernames,
            sellerIdForAdapter!!,
            buyerIdForAdapter!!
        )
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = messageAdapter

        btnBack.setOnClickListener {
            finish()
        }

        btnViewProduct.setOnClickListener {
            if (productId != null) {
                val intent = Intent(this, ProductDetailActivity::class.java)
                intent.putExtra("product_id", productId)
                startActivity(intent)
            }
        }

        btnAllGood.setOnClickListener {
            lifecycleScope.launch {
                confirmOrder()
            }
        }

        btnOpenIncidence.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@ChatActivity, OpenIncidenceActivity::class.java)
                intent.putExtra("order_id", activeOrderId)
                intent.putExtra("product_id", productId)
                intent.putExtra("chat_id", chatId)
                intent.putExtra("seller_id", sellerId)
                startActivityForResult(intent, 1001)
            }
        })

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                lifecycleScope.launch {
                    sendMessage(text)
                }
            }
        }

        lifecycleScope.launch {
            loadUserInfo()
            checkActiveOrder()
            loadMessages()
            subscribeToMessages()
        }
    }

    private suspend fun loadUserInfo() {
        try {
            users = SupabaseClient.client
                .from("users")
                .select(Columns.raw("id, username, role"))
                .decodeList<UserInfo>()

            for (user in users!!) {
                userRoles[user.id] = user.role
                usernames[user.id] = user.username
            }

        } catch (e: Exception) {
            Log.e("Chat", " Error cargando usuarios: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun checkActiveOrder() {
        try {
            if (productId == null) return

            orders = SupabaseClient.client
                .from("orders")
                .select(Columns.ALL) {
                    filter {
                        eq("product_id", productId!!)
                        eq("buyer_id", currentUserId)
                        eq("status", "en_espera")
                    }
                }
                .decodeList<OrderResult>()

            if (orders!!.isNotEmpty()) {
                activeOrderId = orders!!.first().id
                runOnUiThread {
                    actionBar.visibility = View.VISIBLE
                }
            }

        } catch (e: Exception) {
            Log.e("Chat", " Error comprobando orden: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun confirmOrder() {
        try {
            localOrderId = activeOrderId ?: return
            currentUser = SessionManager.getUser() ?: return
            prod = productId ?: return

            orders = SupabaseClient.client
                .from("orders")
                .select(Columns.ALL) {
                    filter {
                        eq("id", localOrderId!!)
                    }
                }
                .decodeList<OrderResult>()

            order = orders!!.firstOrNull() ?: return
            amount = order!!.amount

            products = SupabaseClient.client
                .from("products")
                .select(Columns.raw("seller_id, title")) {
                    filter {
                        eq("id", prod!!)
                    }
                }
                .decodeList<SellerIdResult>()

            sellerIdResult = products!!.firstOrNull()?.seller_id ?: return
            productTitle = products!!.firstOrNull()?.title ?: "Producto"

            SupabaseClient.client.from("orders").update(
                mapOf("status" to "completada")
            ) {
                filter {
                    eq("id", localOrderId!!)
                }
            }

            sellerUser = SupabaseClient.client
                .from("users")
                .select(Columns.raw("balance")) {
                    filter {
                        eq("id", sellerIdResult!!)
                    }
                }
                .decodeList<BalanceResult>()

            sellerBalance = sellerUser!!.firstOrNull()?.balance ?: 0.0

            SupabaseClient.client.from("users").update(
                mapOf("balance" to (sellerBalance!! + amount!!))
            ) {
                filter {
                    eq("id", sellerIdResult!!)
                }
            }

            // Registrar transacción de venta para el vendedor
            SupabaseClient.client.from("wallet_transactions").insert(
                WalletTransaction(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = sellerIdResult!!,
                    type = "venta",
                    amount = amount!!,
                    description = "Venta: $productTitle"
                )
            )

            // Mensaje especial de orden completada en el chat
            SupabaseClient.client.from("messages").insert(
                Message(
                    id = UUID.randomUUID().toString(),
                    productId = prod,
                    orderId = localOrderId,
                    chatId = chatId,
                    senderId = currentUserId,
                    message = "Pedido completado",
                    messageType = "order_completed"
                )
            )

            runOnUiThread {
                actionBar.visibility = View.GONE
                Toast.makeText(this, "¡Transacción completada!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("Chat", " Error confirmando orden: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun loadMessages() {
        try {
            if (chatId != null) {
                messagesList = SupabaseClient.client
                    .from("messages")
                    .select(Columns.ALL) {
                        filter {
                            eq("chat_id", chatId!!)
                        }
                        order("created_at", SupabaseOrder.ASCENDING)
                    }
                    .decodeList<Message>()
            } else if (orderId != null) {
                messagesList = SupabaseClient.client
                    .from("messages")
                    .select(Columns.ALL) {
                        filter {
                            eq("order_id", orderId!!)
                        }
                        order("created_at", SupabaseOrder.ASCENDING)
                    }
                    .decodeList<Message>()
            } else {
                messagesList = emptyList()
            }

            runOnUiThread {
                messageAdapter.setMessages(messagesList!!)
                rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
            }

        } catch (e: Exception) {
            Log.e("Chat", " Error cargando mensajes: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun sendMessage(text: String) {
        try {
            messageId = UUID.randomUUID().toString()
            newMessage = Message(
                id = messageId!!,
                productId = productId,
                orderId = orderId,
                chatId = chatId,
                senderId = currentUserId,
                message = text
            )

            SupabaseClient.client.from("messages").insert(newMessage!!)

            runOnUiThread {
                etMessage.text?.clear()
                messageAdapter.addMessage(newMessage!!)
                rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
            }

        } catch (e: Exception) {
            Log.e("Chat", " Error enviando mensaje: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun subscribeToMessages() {
        try {
            cid = chatId
            if (cid == null) return
            channelName = "chat_$cid"
            channel = SupabaseClient.client.realtime.channel(channelName!!)

            channel!!.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter = "chat_id=eq.$cid"
            }.onEach { action ->
                senderId = action.record["sender_id"]?.jsonPrimitive?.content ?: ""

                if (senderId != currentUserId) {
                    newMessage = Message(
                        id = action.record["id"]?.jsonPrimitive?.content ?: "",
                        productId = action.record["product_id"]?.jsonPrimitive?.content,
                        orderId = action.record["order_id"]?.jsonPrimitive?.content,
                        chatId = action.record["chat_id"]?.jsonPrimitive?.content,
                        senderId = senderId!!,
                        message = action.record["message"]?.jsonPrimitive?.content ?: "",
                        messageType = action.record["message_type"]?.jsonPrimitive?.content ?: "normal",
                        createdAt = action.record["created_at"]?.jsonPrimitive?.content
                    )

                    runOnUiThread {
                        messageAdapter.addMessage(newMessage!!)
                        rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
                    }
                }
            }.launchIn(lifecycleScope)

            channel!!.subscribe()

        } catch (e: Exception) {
            Log.e("Chat", " Error en Realtime: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            runOnUiThread {
                actionBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            try {
                SupabaseClient.client.realtime.removeAllChannels()
            } catch (e: Exception) {
                Log.e("Chat", "Error cerrando canal: ${e.message}")
            }
        }
    }

    @Serializable
    data class SellerIdResult(
        val seller_id: String,
        val title: String = ""
    )

    @Serializable
    data class BalanceResult(val balance: Double)
}