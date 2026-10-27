package com.example.myfirstapp.driver

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.user

val DriverGreen = Color(0xFF116530)
val DriverBg = Color(0xFFF5F8F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScheduleScreen(
    user: user,
    onLogout: () -> Unit
) {
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

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
            // Driver Profile Card
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

            Text(
                text = "Assigned Bus Schedules",
                fontSize = 18.sp,
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
            } else if (schedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No schedules assigned currently.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(schedules) { schedule ->
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
                                            else -> Color(0xFFFF9800)
                                        }
                                    ) {
                                        Text(
                                            text = schedule.status,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal =.8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Route: ${schedule.route.routeName}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Departure: ${schedule.departureTime} | Arrival: ${schedule.arrivalTime}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Driver: ${schedule.bus.assignedDriver}", fontSize = 13.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            DatabaseManager.updateScheduleStatus(
                                                scheduleId = schedule.scheduleId,
                                                newStatus = "Accepted",
                                                driverName = user.fullname
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            DatabaseManager.updateScheduleStatus(
                                                scheduleId = schedule.scheduleId,
                                                newStatus = "Rejected",
                                                driverName = user.fullname
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            DatabaseManager.updateScheduleStatus(
                                                scheduleId = schedule.scheduleId,
                                                newStatus = "In Transit",
                                                driverName = user.fullname
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Start", fontSize = 11.sp)
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
