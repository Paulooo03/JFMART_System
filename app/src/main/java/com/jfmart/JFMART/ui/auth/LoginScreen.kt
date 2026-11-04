package com.jfmart.JFMART.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.ui.components.FormField

/** One sign-in screen for everyone; the server's answer decides whether you land in the client, courier or admin area. */
@Composable
fun LoginScreen(viewModel: AuthViewModel, onSignUp: () -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text("JF MiniMart", style = MaterialTheme.typography.headlineLarge)
        Text("Sign in to continue", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        FormField("Username", username, { username = it })
        Spacer(Modifier.height(12.dp))
        FormField("Password", password, { password = it }, password = true)

        viewModel.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.login(username, password) },
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Sign in")
            }
        }

        TextButton(onClick = { viewModel.resetForm(); onSignUp() }) {
            Text("New hospital or client? Create an account")
        }
    }
}
