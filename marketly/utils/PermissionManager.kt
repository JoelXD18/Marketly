package com.ramos.marketly.utils

object PermissionManager {

    fun canAccessModeratorPanel(): Boolean {
        return SessionManager.isModerator() || SessionManager.isAdmin()
    }

    fun canAccessAdminPanel(): Boolean {
        return SessionManager.isAdmin()
    }

    fun canPublishProducts(): Boolean {
        return SessionManager.isClient() || SessionManager.isAdmin()
    }

    fun canBuyProducts(): Boolean {
        return SessionManager.isClient() || SessionManager.isAdmin()
    }

    fun canOpenIncidence(): Boolean {
        return SessionManager.isClient() || SessionManager.isAdmin()
    }

    fun canResolveIncidence(): Boolean {
        return SessionManager.isModerator() || SessionManager.isAdmin()
    }
}