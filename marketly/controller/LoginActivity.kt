package com.ramos.marketly.controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.ramos.marketly.R
import com.ramos.marketly.utils.SessionManager
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvGoToRegister: TextView

    private var inputStr: String? = null
    private var passwordStr: String? = null
    private var emailToUse: String? = null
    private var result: UserEmail? = null

    @Serializable
    data class UserEmail(val email: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)

        btnLogin.setOnClickListener {
            inputStr = etEmail.text.toString().trim()
            passwordStr = etPassword.text.toString().trim()

            if (validateInputs(inputStr!!, passwordStr!!)) {
                lifecycleScope.launch {
                    loginUser(inputStr!!, passwordStr!!)
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(input: String, password: String): Boolean {
        if (input.isEmpty()) {
            Toast.makeText(this, "El email o usuario es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "La contraseña es obligatoria", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private suspend fun loginUser(input: String, password: String) {
        try {
            if (input.contains("@")) {
                emailToUse = input
            } else {
                result = SupabaseClient.client
                    .from("users")
                    .select(Columns.raw("email")) {
                        filter {
                            eq("username", input)
                        }
                    }
                    .decodeSingle<UserEmail>()
                emailToUse = result!!.email
            }

            SessionManager.loginAndSave(emailToUse!!, password)

            Log.d("Login", " Login OK")
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                finish()
            }

        } catch (e: Exception) {
            Log.e("Login", " Error: ${e.message}")
            runOnUiThread {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}