package com.example.myfirstapp.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
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
import com.example.myfirstapp.TransitGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterSchedulesScreen() {
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list
                isLoading = false
            },
            onError = { _ -> isLoading = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus Departure Schedules & Arrivals", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        },
        containerColor = Color(0xFFF5F8F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Scheduled Departures & Arrivals", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TransitGreen)
                }
            } else if (schedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No schedules available.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(schedules) { schedule ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TransitGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Bus: ${schedule.bus.busNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Route: ${schedule.route.routeName}", fontSize = 14.sp, color = Color.DarkGray)
                                Text("Departure: ${schedule.departureTime} | Arrival: ${schedule.arrivalTime}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Status: ${schedule.status}", fontSize = 12.sp, color = TransitGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
