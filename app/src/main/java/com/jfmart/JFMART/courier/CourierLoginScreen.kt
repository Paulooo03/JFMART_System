package com.jfmart.JFMART.courier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CourierLoginScreen(
    onNavigateToDashboard: () -> Unit,
    onSwitchToClient: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val temporaryPassword = "1234"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineLarge)
        Text("(Courier Name)!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("JF Mart courier login", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (isError) isError = false
            },
            label = { Text("Password") },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(
                        text = "Incorrect password. (Try: 1234)",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (password == temporaryPassword) {
                    onNavigateToDashboard()
                } else {
                    isError = true
                }
            },
            modifier = Modifier.width(120.dp)
        ) {
            Text("Enter")
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = { }) {
            Text("Forgot Password?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToClient) {
            Text("Switch to Client Login")
        }
    }
}