package com.ramos.marketly.controller

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.model.Incidence
import com.ramos.marketly.model.Message
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

class OpenIncidenceActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSubmit: MaterialButton
    private lateinit var etDescription: TextInputEditText

    private var orderId: String? = null
    private var productId: String? = null
    private var chatId: String? = null
    private var sellerId: String? = null

    private var descriptionStr: String? = null
    private var currentUserId: String? = null
    private var oid: String? = null

    @Serializable
    data class OrderInfo(
        val buyer_id: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_incidence)

        btnBack = findViewById(R.id.btnBack)
        btnCancel = findViewById(R.id.btnCancel)
        btnSubmit = findViewById(R.id.btnSubmit)
        etDescription = findViewById(R.id.etDescription)

        orderId = intent.getStringExtra("order_id")
        productId = intent.getStringExtra("product_id")
        chatId = intent.getStringExtra("chat_id")
        sellerId = intent.getStringExtra("seller_id")

        btnBack.setOnClickListener(object : android.view.View.OnClickListener {
            override fun onClick(v: android.view.View?) {
                finish()
            }
        })

        btnCancel.setOnClickListener(object : android.view.View.OnClickListener {
            override fun onClick(v: android.view.View?) {
                finish()
            }
        })

        btnSubmit.setOnClickListener(object : android.view.View.OnClickListener {
            override fun onClick(v: android.view.View?) {
                descriptionStr = etDescription.text.toString().trim()
                if (descriptionStr!!.isEmpty()) {
                    Toast.makeText(this@OpenIncidenceActivity, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
                    return
                }
                lifecycleScope.launch {
                    submitIncidence(descriptionStr!!)
                }
            }
        })
    }

    private suspend fun submitIncidence(description: String) {
        try {
            btnSubmit.isEnabled = false

            currentUserId = SessionManager.getUser()?.id
                ?: throw Exception("No hay usuario autenticado")

            oid = orderId ?: throw Exception("No hay orden asociada")

            // 1. Crear incidencia
            SupabaseClient.client.from("incidences").insert(
                Incidence(
                    id = UUID.randomUUID().toString(),
                    orderId = oid!!,
                    openedBy = currentUserId!!,
                    description = description,
                    status = "abierta"
                )
            )

            // 2. Cambiar status de la orden a en_disputa
            SupabaseClient.client.from("orders").update(
                mapOf("status" to "en_disputa")
            ) {
                filter {
                    eq("id", oid!!)
                }
            }

            // 3. Enviar mensaje especial al chat
            SupabaseClient.client.from("messages").insert(
                Message(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    orderId = oid,
                    chatId = chatId,
                    senderId = currentUserId!!,
                    message = "Incidencia abierta",
                    messageType = "incidence_opened",
                    extraData = description
                )
            )

            Log.d("OpenIncidence", " Incidencia creada")
            runOnUiThread {
                Toast.makeText(this, "Incidencia abierta correctamente", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

        } catch (e: Exception) {
            Log.e("OpenIncidence", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                btnSubmit.isEnabled = true
            }
        }
    }
}