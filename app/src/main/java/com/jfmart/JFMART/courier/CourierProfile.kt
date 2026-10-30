package com.jfmart.JFMART.courier

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.components.WireframeListBox

@Composable
fun CourierProfileScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Manage account", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Account details", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { }) {
                Text("Edit")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("(Courier name)", style = MaterialTheme.typography.bodyMedium)
        Text("(Phone number)", style = MaterialTheme.typography.bodyMedium)
        Text("(Email address)", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Delivery history", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        WireframeListBox(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("(Order number)")
                Text("(Date arrived)")
                Text("(Price)")
            }
        }
    }
}