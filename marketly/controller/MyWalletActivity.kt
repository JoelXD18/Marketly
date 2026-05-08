package com.ramos.marketly.controller

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.ramos.marketly.R
import com.ramos.marketly.adapter.TransactionAdapter
import com.ramos.marketly.model.WalletTransaction
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import java.util.UUID

class MyWalletActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvBalance: TextView
    private lateinit var btnAddBalance: MaterialButton
    private lateinit var rvTransactions: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var transactionAdapter: TransactionAdapter

    private var user: com.ramos.marketly.model.User? = null
    private var usersList: List<BalanceResult>? = null
    private var balance: Double? = null
    private var transactionsList: List<WalletTransaction>? = null
    private var dialogView: View? = null
    private var input: TextInputEditText? = null
    private var container: TextInputLayout? = null
    private var amountStr: String? = null
    private var amount: Double? = null
    private var currentBalance: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_wallet)

        btnBack = findViewById(R.id.btnBack)
        tvBalance = findViewById(R.id.tvBalance)
        btnAddBalance = findViewById(R.id.btnAddBalance)
        rvTransactions = findViewById(R.id.rvTransactions)
        llEmpty = findViewById(R.id.llEmpty)

        transactionAdapter = TransactionAdapter(mutableListOf())
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = transactionAdapter

        btnBack.setOnClickListener { finish() }

        btnAddBalance.setOnClickListener {
            showAddBalanceDialog()
        }

        lifecycleScope.launch {
            loadWallet()
        }
    }

    private suspend fun loadWallet() {
        try {
            user = SessionManager.getUser() ?: return

            // Cargar saldo actualizado
            usersList = SupabaseClient.client
                .from("users")
                .select(Columns.raw("balance")) {
                    filter {
                        eq("id", user!!.id)
                    }
                }
                .decodeList<BalanceResult>()

            balance = usersList!!.firstOrNull()?.balance ?: 0.0

            // Cargar historial de transacciones
            transactionsList = SupabaseClient.client
                .from("wallet_transactions")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", user!!.id)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<WalletTransaction>()

            runOnUiThread {
                tvBalance.text = "$balance "

                if (transactionsList!!.isEmpty()) {
                    llEmpty.visibility = View.VISIBLE
                    rvTransactions.visibility = View.GONE
                } else {
                    llEmpty.visibility = View.GONE
                    rvTransactions.visibility = View.VISIBLE
                    transactionAdapter.setTransactions(transactionsList!!)
                }
            }

        } catch (e: Exception) {
            Log.e("MyWallet", " Error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun showAddBalanceDialog() {
        dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        input = TextInputEditText(this)
        input!!.hint = "Cantidad a añadir ()"
        input!!.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        container = TextInputLayout(this)
        container!!.addView(input)
        container!!.setPadding(48, 16, 48, 0)

        AlertDialog.Builder(this)
            .setTitle("Añadir saldo")
            .setMessage("Introduce la cantidad que quieres añadir")
            .setView(container)
            .setPositiveButton("Añadir", object : android.content.DialogInterface.OnClickListener {
                override fun onClick(dialog: android.content.DialogInterface?, which: Int) {
                    amountStr = input!!.text.toString().trim()
                    if (amountStr!!.isEmpty()) {
                        Toast.makeText(this@MyWalletActivity, "Introduce una cantidad", Toast.LENGTH_SHORT).show()
                        return
                    }
                    amount = amountStr!!.toDoubleOrNull()
                    if (amount == null || amount!! <= 0) {
                        Toast.makeText(this@MyWalletActivity, "Cantidad no válida", Toast.LENGTH_SHORT).show()
                        return
                    }
                    lifecycleScope.launch {
                        addBalance(amount!!)
                    }
                }
            })
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun addBalance(amountToAdd: Double) {
        try {
            user = SessionManager.getUser() ?: return

            // Obtener saldo actual
            usersList = SupabaseClient.client
                .from("users")
                .select(Columns.raw("balance")) {
                    filter {
                        eq("id", user!!.id)
                    }
                }
                .decodeList<BalanceResult>()

            currentBalance = usersList!!.firstOrNull()?.balance ?: 0.0

            // Actualizar saldo
            SupabaseClient.client.from("users").update(
                mapOf("balance" to (currentBalance!! + amountToAdd))
            ) {
                filter {
                    eq("id", user!!.id)
                }
            }

            // Registrar transacción
            SupabaseClient.client.from("wallet_transactions").insert(
                WalletTransaction(
                    id = UUID.randomUUID().toString(),
                    userId = user!!.id,
                    type = "recarga",
                    amount = amountToAdd,
                    description = "Recarga de saldo"
                )
            )

            Log.d("MyWallet", " Saldo añadido: $amount ")
            runOnUiThread {
                Toast.makeText(this, "¡Saldo añadido correctamente!", Toast.LENGTH_SHORT).show()
            }

            loadWallet()

        } catch (e: Exception) {
            Log.e("MyWallet", " Error: ${e.message}")
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error al añadir saldo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class BalanceResult(val balance: Double)
}