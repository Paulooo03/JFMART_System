package com.jfmart.JFMART.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Placeholder until it is decided whether the admin works in the app or on a web dashboard. */
@Composable
fun AdminNoticeScreen(onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Admin account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Admin tools are not in the app yet.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onLogout) { Text("Sign out") }
    }
}
