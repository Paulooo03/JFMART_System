package com.jfmart.JFMART.data.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Session(
    val token: String,
    val role: String,
    val userId: Long,
    val clientId: Long?,
    val username: String
)

const val ROLE_CLIENT = "ROLE_CLIENT"
const val ROLE_DELIVERY = "ROLE_DELIVERY"
const val ROLE_ADMIN = "ROLE_ADMIN"

/** Keeps the signed-in user across app restarts. A null session means signed out. */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("jfmart_session", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())

    val state: StateFlow<Session?> = _state.asStateFlow()
    val current: Session? get() = _state.value

    fun save(session: Session) {
        prefs.edit()
            .putString("token", session.token)
            .putString("role", session.role)
            .putLong("userId", session.userId)
            .putLong("clientId", session.clientId ?: -1L)
            .putString("username", session.username)
            .apply()
        _state.value = session
    }

    fun clear() {
        prefs.edit().clear().apply()
        _state.value = null
    }

    private fun load(): Session? {
        val token = prefs.getString("token", null) ?: return null
        val role = prefs.getString("role", null) ?: return null
        val clientId = prefs.getLong("clientId", -1L).takeIf { it >= 0 }
        return Session(token, role, prefs.getLong("userId", 0L), clientId, prefs.getString("username", "") ?: "")
    }
}
