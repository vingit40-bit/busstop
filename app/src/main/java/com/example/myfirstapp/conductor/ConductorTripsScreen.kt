package com.example.myfirstapp.conductor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun ConductorTripsScreen() {
    var acceptedSchedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                acceptedSchedules = list.filter { s ->
                    s.status.equals("Accepted", ignoreCase = true) ||
                    s.status.equals("In Transit", ignoreCase = true)
                }
                isLoading = false
            },
            onError = { _ ->
                isLoading = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Active Accepted Trips", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ConductorGreen)
            }
        } else if (acceptedSchedules.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No accepted trips currently available. Waiting for driver acceptance.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(acceptedSchedules) { schedule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Bus: ${schedule.bus.busNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ConductorGreen)
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50)) {
                                    Text(schedule.status, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Route: ${schedule.route.routeName}", fontSize = 14.sp)
                            Text("Driver: ${schedule.bus.assignedDriver}", fontSize = 13.sp, color = Color.Gray)
                            Text("Departure: ${schedule.departureTime} | Arrival: ${schedule.arrivalTime}", fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
