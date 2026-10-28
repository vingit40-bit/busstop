package com.example.myfirstapp.driver

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.admin.LeafletMapViewScreen
import com.example.myfirstapp.user

val DriverGreen = Color(0xFF116530)
val DriverBg = Color(0xFFF5F8F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScheduleScreen(
    user: user,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    var selectedTab by remember { mutableStateOf("Pending") } // "Pending", "Accepted", "Rejected"
    var activeNavigationSchedule by remember { mutableStateOf<BusSchedule?>(null) }

    var scheduleToReject by remember { mutableStateOf<BusSchedule?>(null) }
    val validReasons = listOf(
        "Vehicle maintenance issue / Mechanical problem",
        "Schedule conflict / Overlapping trip",
        "Personal emergency / Medical leave",
        "Route unsafe / Weather condition",
        "Off duty / Rest day requested"
    )
    var selectedReason by remember { mutableStateOf(validReasons[0]) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list
                isLoading = false
            },
            onError = { e ->
                errorMessage = e.localizedMessage ?: "Failed to load schedules"
                isLoading = false
            }
        )
    }

    if (activeNavigationSchedule != null) {
        val sched = activeNavigationSchedule!!
        LeafletMapViewScreen(
            leafletId = sched.bus.busNumber,
            routeName = sched.route.routeName,
            onBack = { activeNavigationSchedule = null }
        )
        return
    }

    if (scheduleToReject != null) {
        AlertDialog(
            onDismissRequest = { scheduleToReject = null },
            title = { Text("Select Rejection Reason", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please choose a valid reason for rejecting schedule ${scheduleToReject!!.scheduleId}:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    validReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        DatabaseManager.updateScheduleRejection(
                            scheduleId = scheduleToReject!!.scheduleId,
                            reason = selectedReason,
                            onSuccess = {
                                Toast.makeText(context, "Schedule rejected and sent to Admin notification", Toast.LENGTH_SHORT).show()
                                scheduleToReject = null
                                selectedTab = "Rejected"
                            },
                            onFailure = { err ->
                                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Confirm Rejection", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    val filteredSchedules = remember(schedules, selectedTab) {
        when (selectedTab) {
            "Pending" -> schedules.filter { it.status.equals("Scheduled", ignoreCase = true) || it.status.isBlank() }
            "Accepted" -> schedules.filter { it.status.equals("Accepted", ignoreCase = true) || it.status.equals("In Transit", ignoreCase = true) }
            "Rejected" -> schedules.filter { it.status.equals("Rejected", ignoreCase = true) }
            else -> schedules
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Portal - ${user.fullname}", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DriverGreen)
            )
        },
        containerColor = DriverBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Driver Account", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DriverGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Name: ${user.fullname}", fontSize = 14.sp)
                    Text("Email: ${user.email}", fontSize = 14.sp, color = Color.Gray)
                    Text("Role: ${user.role}", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Filters (Pending | Accepted | Rejected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Pending", "Accepted", "Rejected").forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$selectedTab Schedules (${filteredSchedules.size})",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DriverGreen)
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
            } else if (filteredSchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No $selectedTab schedules found.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSchedules) { schedule ->
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
                                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = DriverGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Bus: ${schedule.bus.busNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (schedule.status) {
                                            "In Transit" -> Color(0xFF2196F3)
                                            "Completed" -> Color(0xFF4CAF50)
                                            "Accepted" -> Color(0xFF116530)
                                            "Rejected" -> Color(0xFFFFEBEE)
                                            else -> Color(0xFFFF9800)
                                        }
                                    ) {
                                        Text(
                                            text = schedule.status,
                                            color = if (schedule.status == "Rejected") Color(0xFFC62828) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Route: ${schedule.route.routeName}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Departure: ${schedule.departureTime} | Arrival: ${schedule.arrivalTime}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Driver: ${schedule.bus.assignedDriver}", fontSize = 13.sp, color = Color.Gray)
                                if (schedule.rejectionReason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Rejection Reason: ${schedule.rejectionReason}", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Show action buttons based on tab
                                when (selectedTab) {
                                    "Pending" -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    DatabaseManager.updateScheduleStatus(
                                                        scheduleId = schedule.scheduleId,
                                                        newStatus = "Accepted",
                                                        driverName = user.fullname
                                                    )
                                                    Toast.makeText(context, "Schedule Accepted!", Toast.LENGTH_SHORT).show()
                                                    selectedTab = "Accepted"
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    scheduleToReject = schedule
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    "Accepted" -> {
                                        Button(
                                            onClick = {
                                                DatabaseManager.updateScheduleStatus(
                                                    scheduleId = schedule.scheduleId,
                                                    newStatus = "In Transit",
                                                    driverName = user.fullname
                                                )
                                                Toast.makeText(context, "Trip Started! Launching Live Navigation...", Toast.LENGTH_SHORT).show()
                                                activeNavigationSchedule = schedule
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Start Trip (Live Navigation)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    "Rejected" -> {
                                        // No action buttons shown for rejected schedules
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
