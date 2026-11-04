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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jfmart.JFMART.data.remote.SignUpRequest
import com.jfmart.JFMART.ui.components.FormField
import com.jfmart.JFMART.ui.components.ScreenHeader

@Composable
fun SignUpScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    if (viewModel.signUpDone) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Spacer(Modifier.height(64.dp))
            Text("Account created", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("JF MiniMart needs to approve your account before you can sign in. We will let your contact person know.")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.resetForm(); onBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back to sign in")
            }
        }
        return
    }

    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var hospital by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ScreenHeader("Create an account", onBack = { viewModel.resetForm(); onBack() })

        FormField("Client / hospital name", hospital, { hospital = it })
        FormField("Contact person", contact, { contact = it })
        FormField("Contact number", phone, { phone = it }, keyboardType = KeyboardType.Phone)
        FormField("Email address", email, { email = it }, keyboardType = KeyboardType.Email)
        FormField("Delivery address", address, { address = it }, singleLine = false)
        FormField("Department (if applicable)", department, { department = it })

        HorizontalDivider()
        Text("Sign-in details", style = MaterialTheme.typography.titleMedium)
        FormField("Username", username, { username = it })
        FormField("Password (at least 8 characters)", password, { password = it }, password = true)
        FormField("Confirm password", confirm, { confirm = it }, password = true)

        viewModel.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val missing = listOf(hospital, contact, phone, email, address, username).any { it.isBlank() }
                when {
                    missing -> viewModel.showError("Fill in every field except department.")
                    password.length < 8 -> viewModel.showError("The password needs at least 8 characters.")
                    password != confirm -> viewModel.showError("The two passwords do not match.")
                    else -> viewModel.signUp(
                        SignUpRequest(
                            username = username.trim(),
                            password = password,
                            hospitalName = hospital.trim(),
                            contactPerson = contact.trim(),
                            phoneNumber = phone.trim(),
                            email = email.trim(),
                            deliveryAddress = address.trim(),
                            department = department.trim().ifEmpty { null }
                        )
                    )
                }
            },
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Create account")
            }
        }
    }
}
