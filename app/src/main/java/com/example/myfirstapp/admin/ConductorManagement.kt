package com.example.myfirstapp.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.Bus
import com.example.myfirstapp.ConductorRecord
import com.example.myfirstapp.DatabaseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorManagementScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var conductors by remember { mutableStateOf<List<ConductorRecord>>(emptyList()) }
    val busList = remember { mutableStateListOf<Bus>() }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConductor by remember { mutableStateOf<ConductorRecord?>(null) }
    var conductorToDelete by remember { mutableStateOf<ConductorRecord?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToConductors(
            onUpdate = { list ->
                conductors = list
                isLoading = false
            },
            onError = { _ ->
                isLoading = false
            }
        )
        DatabaseManager.listenToBuses(
            onUpdate = { fetchedBuses ->
                busList.clear()
                busList.addAll(fetchedBuses)
            }
        )
    }

    val filteredList = conductors.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
        it.employeeId.contains(searchQuery, ignoreCase = true) ||
        it.email.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conductor Management", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminGreen)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingConductor = null; showAddDialog = true },
                containerColor = AdminGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Conductor")
            }
        },
        containerColor = AdminBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, employee ID, or email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AdminGreen) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdminGreen,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminGreen)
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No conductors found.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList) { conductor ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFE8F5E9),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Badge, contentDescription = null, tint = AdminGreen)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(conductor.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                            Text("ID: ${conductor.employeeId}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (conductor.status == "On Duty") Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    ) {
                                        Text(
                                            text = conductor.status,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Phone: ${conductor.phoneNumber}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Email: ${conductor.email}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Password: ${conductor.password}", fontSize = 13.sp, color = Color.Gray)
                                Text("Assigned Bus ID: ${if (conductor.assignedBusId.isNotBlank()) conductor.assignedBusId else "Unassigned"}", fontSize = 13.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { editingConductor = conductor; showAddDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = AdminGreen)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit", fontSize = 12.sp, color = AdminGreen)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = { conductorToDelete = conductor },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Conductor Dialog
    if (showAddDialog) {
        val target = editingConductor
        val conductorId = remember { target?.id ?: java.util.UUID.randomUUID().toString() }
        var fullName by remember { mutableStateOf(target?.fullName ?: "") }
        var employeeId by remember { mutableStateOf(target?.employeeId ?: ("EMP-" + (1000..9999).random())) }
        var phoneNumber by remember { mutableStateOf(target?.phoneNumber ?: "") }
        var email by remember { mutableStateOf(target?.email ?: "") }
        var username by remember { mutableStateOf(target?.username ?: "") }
        var password by remember { mutableStateOf(target?.password ?: "password123") }
        var passwordVisible by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf(target?.status ?: "On Duty") }
        var assignedBusId by remember { mutableStateOf(target?.assignedBusId ?: "") }
        var busDropdownExpanded by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (target != null) "Edit Conductor" else "Add New Conductor") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (dialogError.isNotEmpty()) {
                        Text(dialogError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = employeeId,
                        onValueChange = { employeeId = it },
                        label = { Text("Employee ID (Unique)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Login Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "Hide" else "Show", color = AdminGreen, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Status Selection
                    Text("Employment Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("On Duty", "Off Duty", "On Leave").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    // Assigned Bus Dropdown
                    ExposedDropdownMenuBox(
                        expanded = busDropdownExpanded,
                        onExpandedChange = { busDropdownExpanded = !busDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (assignedBusId.isBlank()) "Unassigned" else assignedBusId,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Assigned Bus ID / Number") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = busDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = busDropdownExpanded,
                            onDismissRequest = { busDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned") },
                                onClick = {
                                    assignedBusId = ""
                                    busDropdownExpanded = false
                                }
                            )
                            busList.forEach { bus ->
                                DropdownMenuItem(
                                    text = { Text("${bus.busNumber} (${bus.busId})") },
                                    onClick = {
                                        assignedBusId = bus.busId
                                        busDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullName.isBlank() || employeeId.isBlank()) {
                            dialogError = "Full Name and Employee ID are required"
                            return@Button
                        }
                        val record = ConductorRecord(
                            id = conductorId,
                            fullName = fullName.trim(),
                            employeeId = employeeId.trim(),
                            phoneNumber = phoneNumber.trim(),
                            email = email.trim(),
                            username = username.trim(),
                            role = "Conductor",
                            assignedBusId = assignedBusId.trim(),
                            status = status,
                            password = password.ifBlank { "password123" }
                        )
                        DatabaseManager.saveConductor(
                            conductor = record,
                            onSuccess = {
                                Toast.makeText(context, "Conductor saved successfully!", Toast.LENGTH_SHORT).show()
                                showAddDialog = false
                                editingConductor = null
                            },
                            onFailure = { e ->
                                dialogError = e.localizedMessage ?: "Failed to save conductor"
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGreen)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (conductorToDelete != null) {
        val target = conductorToDelete!!
        AlertDialog(
            onDismissRequest = { conductorToDelete = null },
            title = { Text("Delete Conductor") },
            text = { Text("Are you sure you want to delete ${target.fullName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        DatabaseManager.deleteConductor(
                            id = target.id,
                            onSuccess = {
                                Toast.makeText(context, "Conductor deleted.", Toast.LENGTH_SHORT).show()
                                conductorToDelete = null
                            },
                            onFailure = { e ->
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                conductorToDelete = null
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { conductorToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
