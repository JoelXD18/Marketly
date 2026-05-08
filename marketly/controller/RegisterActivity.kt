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
import com.ramos.marketly.model.User
import com.ramos.marketly.utils.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnRegister: MaterialButton

    private lateinit var tvGoToLogin: TextView

    private var emailStr: String? = null
    private var usernameStr: String? = null
    private var passwordStr: String? = null
    private var currentUser: io.github.jan.supabase.gotrue.user.UserInfo? = null
    private var userIdStr: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Referencias a las vistas
        etEmail = findViewById(R.id.etEmail)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)

        // Click en botón registrar
        btnRegister.setOnClickListener {
            emailStr = etEmail.text.toString().trim()
            usernameStr = etUsername.text.toString().trim()
            passwordStr = etPassword.text.toString().trim()

            if (validateInputs(emailStr!!, usernameStr!!, passwordStr!!)) {
                registerUser(emailStr!!, usernameStr!!, passwordStr!!)
            }


        }

        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInputs(email: String, username: String, password: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "El email es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.isEmpty()) {
            Toast.makeText(this, "El nombre de usuario es obligatorio", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(email: String, username: String, password: String) {
        lifecycleScope.launch {
            try {
                // 1. Registrar en Auth
                SupabaseClient.client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                // 2. Obtener el usuario actual (así evitamos el null de signUpWith)
                currentUser = SupabaseClient.client.auth.currentUserOrNull()
                    ?: throw Exception("No se pudo obtener el usuario tras el registro")

                userIdStr = currentUser!!.id
                Log.d("Register", " Auth OK - ID: $userIdStr")

                // 3. Insertar en public.users
                SupabaseClient.client.from("users").insert(
                    User(
                        id = userIdStr!!,
                        email = email,
                        username = username
                    )
                )

                Log.d("Register", " Insert en users OK")
                Toast.makeText(this@RegisterActivity, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Log.e("Register", " Error: ${e.message}")
                Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}