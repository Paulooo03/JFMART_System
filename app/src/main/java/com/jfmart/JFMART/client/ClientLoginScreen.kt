package com.jfmart.JFMART.client
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ClientLoginScreen(
    onNavigateToDashboard: () -> Unit,
    onSwitchToCourier: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    // Temporary password for testing
    val temporaryPassword = "1234"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome", style = MaterialTheme.typography.headlineLarge)
        Text("(Client Name)!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("JF Mart client login", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (isError) isError = false // Clear error indicator while typing
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

        TextButton(onClick = onSwitchToCourier) {
            Text("Switch to Courier Login")
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ClientLoginScreenPreview() {
    MaterialTheme {
        ClientLoginScreen(
            onNavigateToDashboard = {},
            onSwitchToCourier = {}
        )
    }
}