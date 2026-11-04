package com.jfmart.JFMART.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jfmart.JFMART.AppGraph
import com.jfmart.JFMART.data.remote.ApiResult
import com.jfmart.JFMART.data.remote.ClientProfile
import com.jfmart.JFMART.data.remote.UpdateProfileRequest
import com.jfmart.JFMART.data.remote.apiCall
import com.jfmart.JFMART.ui.components.*
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    var state by mutableStateOf<LoadState<ClientProfile>>(LoadState.Loading)
        private set
    var saving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch {
            if (state !is LoadState.Loaded) state = LoadState.Loading
            state = when (val result = apiCall { AppGraph.api.profile() }) {
                is ApiResult.Ok -> LoadState.Loaded(result.value)
                is ApiResult.Err -> LoadState.Error(result.message)
            }
        }
    }

    fun save(request: UpdateProfileRequest, onSaved: () -> Unit) {
        viewModelScope.launch {
            saving = true
            saveError = null
            when (val result = apiCall { AppGraph.api.updateProfile(request) }) {
                is ApiResult.Ok -> {
                    state = LoadState.Loaded(result.value)
                    onSaved()
                }
                is ApiResult.Err -> saveError = result.message
            }
            saving = false
        }
    }

    fun clearSaveError() {
        saveError = null
    }
}

@Composable
fun AccountScreen(
    onOpenStatement: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AccountViewModel = viewModel()
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Account")
        LoadStateView(viewModel.state, onRetry = { viewModel.load() }) { profile ->
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editing) {
                    EditProfile(
                        profile = profile,
                        saving = viewModel.saving,
                        error = viewModel.saveError,
                        onCancel = { viewModel.clearSaveError(); editing = false },
                        onSave = { request -> viewModel.save(request) { editing = false } }
                    )
                } else {
                    ProfileDetails(profile, onEdit = { editing = true })
                }

                if (!editing) {
                    HorizontalDivider()
                    OutlinedButton(onClick = onOpenStatement, modifier = Modifier.fillMaxWidth()) {
                        Text("Statement and payments")
                    }
                    Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
                }
            }
        }
    }
}

@Composable
private fun ProfileDetails(profile: ClientProfile, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(profile.hospitalName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onEdit) { Text("Edit") }
    }
    DetailLine("Contact person", profile.contactPerson)
    DetailLine("Contact number", profile.phoneNumber)
    DetailLine("Email", profile.email)
    DetailLine("Delivery address", profile.deliveryAddress)
    profile.department?.let { DetailLine("Department", it) }
    profile.username?.let { DetailLine("Username", it) }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EditProfile(
    profile: ClientProfile,
    saving: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onSave: (UpdateProfileRequest) -> Unit
) {
    var contact by rememberSaveable { mutableStateOf(profile.contactPerson) }
    var phone by rememberSaveable { mutableStateOf(profile.phoneNumber) }
    var email by rememberSaveable { mutableStateOf(profile.email) }
    var address by rememberSaveable { mutableStateOf(profile.deliveryAddress) }
    var department by rememberSaveable { mutableStateOf(profile.department.orEmpty()) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

    Text(profile.hospitalName, style = MaterialTheme.typography.titleLarge)
    Text("To change the hospital name, contact JF MiniMart.", style = MaterialTheme.typography.bodySmall)
    FormField("Contact person", contact, { contact = it })
    FormField("Contact number", phone, { phone = it }, keyboardType = KeyboardType.Phone)
    FormField("Email address", email, { email = it }, keyboardType = KeyboardType.Email)
    FormField("Delivery address", address, { address = it }, singleLine = false)
    FormField("Department (if applicable)", department, { department = it })

    (formError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCancel, enabled = !saving, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(
            onClick = {
                if (listOf(contact, phone, email, address).any { it.isBlank() }) {
                    formError = "Fill in every field except department."
                } else {
                    formError = null
                    onSave(UpdateProfileRequest(contact.trim(), phone.trim(), email.trim(), address.trim(), department.trim().ifEmpty { null }))
                }
            },
            enabled = !saving,
            modifier = Modifier.weight(1f)
        ) { Text(if (saving) "Saving..." else "Save") }
    }
}
