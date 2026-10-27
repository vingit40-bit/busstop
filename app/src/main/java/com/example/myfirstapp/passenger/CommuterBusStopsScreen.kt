package com.example.myfirstapp.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BusStop
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.TransitGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterBusStopsScreen() {
    var busStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToBusStops(
            onUpdate = { list ->
                busStops = list
                isLoading = false
            },
            onError = { _ -> isLoading = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bus Stops & Terminal Directory", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Text("Registered Bus Stops & Terminals", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TransitGreen)
                }
            } else if (busStops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No bus stops available.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(busStops) { stop ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = TransitGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stop.stopName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Address: ${stop.location}", fontSize = 13.sp, color = Color.DarkGray)
                                Text("Operating Hours: ${stop.operatingHours}", fontSize = 12.sp, color = Color.Gray)
                                Text("Active Bays: ${stop.activeBays} / ${stop.baysCount}", fontSize = 12.sp, color = TransitGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
