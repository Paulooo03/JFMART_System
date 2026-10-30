package com.jfmart.JFMART.client
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.components.WireframeListBox // ✅ Correct package
@Composable
fun ClientOrderDetailsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Order status", style = MaterialTheme.typography.titleMedium)
        Text("(Order number) Awaiting for scan", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(12.dp))

        WireframeListBox(modifier = Modifier.weight(1f)) {
            Text("Item One", modifier = Modifier.padding(vertical = 4.dp))
            Text("Item Two", modifier = Modifier.padding(vertical = 4.dp))
            Text("Item Three", modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total:", style = MaterialTheme.typography.titleLarge)
            Text("(Price)", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Scan QR")
        }
    }
}