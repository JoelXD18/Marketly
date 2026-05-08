package com.ramos.marketly.utils

import android.content.Context
import com.ramos.marketly.controller.MarketlyApp
import com.ramos.marketly.model.User
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

object SessionManager {

    private var currentUser: User? = null
    private val prefs by lazy {
        MarketlyApp.instance.getSharedPreferences("marketly_session", Context.MODE_PRIVATE)
    }
    private var authUser: io.github.jan.supabase.gotrue.user.UserInfo? = null
    private var sessionEmail: String? = null
    private var sessionPassword: String? = null

    suspend fun loadUser(): User? {
        authUser = SupabaseClient.client.auth.currentUserOrNull()
        if (authUser == null) return null

        currentUser = SupabaseClient.client
            .from("users")
            .select(Columns.ALL) {
                filter {
                    eq("id", authUser!!.id)
                }
            }
            .decodeSingle<User>()

        return currentUser
    }

    suspend fun loginAndSave(email: String, password: String) {
        SupabaseClient.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        prefs.edit().putString("saved_email", email).putString("saved_password", password).apply()
        loadUser()
    }

    suspend fun restoreSession(): Boolean {
        sessionEmail = prefs.getString("saved_email", null)
        if (sessionEmail == null) return false
        sessionPassword = prefs.getString("saved_password", null)
        if (sessionPassword == null) return false
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = sessionEmail!!
                this.password = sessionPassword!!
            }
            loadUser()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUser(): User? = currentUser

    fun getRole(): String = currentUser?.role ?: "cliente"

    fun isAdmin(): Boolean = getRole() == "administrador"

    fun isModerator(): Boolean = getRole() == "moderador" || isAdmin()

    fun isClient(): Boolean = getRole() == "cliente"

    fun clear() {
        currentUser = null
        prefs.edit().clear().apply()
    }
}