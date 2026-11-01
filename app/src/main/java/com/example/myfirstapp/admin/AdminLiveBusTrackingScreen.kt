package com.example.myfirstapp.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.Bus
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.LiveBusLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveBusTrackingScreen(
    onBack: () -> Unit = {}
) {
    var registeredBuses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var liveLocations by remember { mutableStateOf<List<LiveBusLocation>>(emptyList()) }
    var selectedBusNumber by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToBuses(
            onUpdate = { buses ->
                registeredBuses = buses
            }
        )
        DatabaseManager.listenToLiveBusLocations(
            onUpdate = { locs ->
                liveLocations = locs
                isLoading = false
            },
            onError = { isLoading = false }
        )
    }

    val selectedBusLoc = remember(liveLocations, selectedBusNumber) {
        if (selectedBusNumber != null) {
            liveLocations.find { it.busNumber.contains(selectedBusNumber!!, ignoreCase = true) || it.busId == selectedBusNumber }
        } else liveLocations.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Fleet Live Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Real-Time GPS Monitoring for All Registered Buses", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isLoading = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminGreen)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background Map Engine Container
            LeafletMapViewScreen(
                leafletId = selectedBusLoc?.busId ?: "ALL",
                routeName = selectedBusLoc?.routeName ?: "System Active Routes",
                startLat = selectedBusLoc?.latitude,
                startLng = selectedBusLoc?.longitude,
                showAllRoutes = true,
                onBack = onBack
            )

            // Top Floating Bus Selection Drawer / Ribbon Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Registered Buses (${registeredBuses.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AdminGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "${liveLocations.count { !it.isOffline() }} Online",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (registeredBuses.isEmpty()) {
                            Text("No registered buses in database.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(registeredBuses) { bus ->
                                    val loc = liveLocations.find { it.busNumber.contains(bus.busNumber, ignoreCase = true) || it.busId == bus.busId }
                                    val statusText = loc?.computedStatus() ?: "Offline"
                                    val isSel = selectedBusNumber == bus.busNumber

                                    Card(
                                        modifier = Modifier
                                            .width(135.dp)
                                            .clickable { selectedBusNumber = bus.busNumber },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isSel) AdminGreen else Color(0xFFF0F4F1)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSel) 4.dp else 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = bus.busNumber,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isSel) Color.White else Color.Black
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = when (statusText) {
                                                        "Moving" -> Color(0xFF4CAF50)
                                                        "Stopped" -> Color(0xFFFF9800)
                                                        else -> Color.Gray
                                                    }
                                                ) {
                                                    Text(
                                                        text = statusText,
                                                        color = Color.White,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Driver: ${bus.assignedDriver.ifBlank { "Unassigned" }}",
                                                fontSize = 10.sp,
                                                color = if (isSel) Color.White.copy(alpha = 0.85f) else Color.DarkGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (loc != null && !loc.isOffline()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Speed, contentDescription = null, tint = if (isSel) Color.White else AdminGreen, modifier = Modifier.size(11.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("${loc.speedKmh.toInt()} km/h", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else AdminGreen)
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
        }
    }
}
