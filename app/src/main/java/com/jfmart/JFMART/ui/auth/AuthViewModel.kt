package com.jfmart.JFMART.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.LoginRequest
import com.jfmart.JFMART.data.remote.SignUpRequest
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.data.session.Session
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var signUpDone by mutableStateOf(false)
        private set

    /** On success the session is saved and navigation moves on by itself. */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            error = "Enter your username and password."
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = apiCall { AppGraph.api.login(LoginRequest(username.trim(), password)) }) {
                is ApiResult.Ok -> AppGraph.session.save(
                    Session(
                        token = result.value.token,
                        role = result.value.role,
                        userId = result.value.userId,
                        clientId = result.value.clientId,
                        username = username.trim()
                    )
                )
                is ApiResult.Err -> error = result.message
            }
            loading = false
        }
    }

    fun signUp(request: SignUpRequest) {
        viewModelScope.launch {
            loading = true
            error = null
            when (val result = apiCall { AppGraph.api.signUp(request) }) {
                is ApiResult.Ok -> signUpDone = true
                is ApiResult.Err -> error = result.message
            }
            loading = false
        }
    }

    fun showError(message: String) {
        error = message
    }

    fun resetForm() {
        error = null
        signUpDone = false
    }

    fun logout() = AppGraph.session.clear()
}
