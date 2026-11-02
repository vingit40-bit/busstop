package com.example.myfirstapp.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.LiveBusLocation
import com.example.myfirstapp.TransitGreen
import com.example.myfirstapp.admin.LeafletMapViewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterLiveNavigationScreen(
    busId: String = "",
    routeName: String = "Tuy ➔ Pasay",
    startPoint: String = "",
    destination: String = "",
    onBack: () -> Unit = {}
) {
    var liveLocations by remember { mutableStateOf<List<LiveBusLocation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToLiveBusLocations(
            onUpdate = { locs ->
                liveLocations = locs
                isLoading = false
            },
            onError = { isLoading = false }
        )
    }

    val cleanStart = remember(startPoint, routeName) {
        if (startPoint.isNotBlank() && startPoint != "Origin" && startPoint != "Central Terminal") {
            startPoint.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" ➔ ")) {
            val raw = routeName.split(" ➔ ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" - ")) {
            val raw = routeName.split(" - ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else {
            "Tuy"
        }
    }

    val cleanDest = remember(destination, routeName) {
        if (destination.isNotBlank() && destination != "Destination" && destination != "Airport Hub") {
            destination.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" ➔ ")) {
            routeName.split(" ➔ ")[1].trim()
        } else if (routeName.contains(" - ")) {
            routeName.split(" - ")[1].trim()
        } else {
            "Pasay"
        }
    }

    // Strictly match target bus or route location without falling back to unrelated routes
    val targetLoc = remember(liveLocations, busId, routeName) {
        liveLocations.find {
            (busId.isNotBlank() && (it.busId.contains(busId, ignoreCase = true) || it.busNumber.contains(busId, ignoreCase = true))) ||
            (routeName.isNotBlank() && (it.routeName.contains(routeName, ignoreCase = true) || routeName.contains(it.routeName, ignoreCase = true)))
        } ?: liveLocations.firstOrNull()
    }

    val statusText = targetLoc?.computedStatus() ?: "Active"
    val isOnline = statusText != "Offline"

    val liveBusNum = targetLoc?.busNumber ?: busId.ifBlank { "Assigned Bus" }
    val liveDriver = targetLoc?.driverName ?: "Assigned Driver"
    val liveSpeed = targetLoc?.speedKmh?.toInt() ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Commuter Live Bus Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Route: $cleanStart ➔ $cleanDest", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Live Interactive Map Container with Real-Time Bus & Driver Tracking
            LeafletMapViewScreen(
                leafletId = busId.ifBlank { "R-TUY-PASAY" },
                routeName = "$cleanStart ➔ $cleanDest",
                startPoint = cleanStart,
                destination = cleanDest,
                liveBusLat = targetLoc?.latitude,
                liveBusLng = targetLoc?.longitude,
                liveBusHeading = targetLoc?.heading ?: 0f,
                liveBusLabel = "Bus $liveBusNum • $liveDriver ($liveSpeed km/h)",
                showAllRoutes = false,
                onBack = onBack
            )

            // Top Status Card Overlay
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
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
                                color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = if (isOnline) TransitGreen else Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Bus: ${targetLoc?.busNumber ?: busId.ifBlank { "Assigned Bus" }}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Driver: ${targetLoc?.driverName ?: "Assigned Driver"}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (statusText) {
                                "Moving", "Active" -> Color(0xFF4CAF50)
                                "Stopped" -> Color(0xFFFF9800)
                                else -> Color.Gray
                            }
                        ) {
                            Text(
                                text = statusText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF0F2F0))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TransitGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (targetLoc != null && !targetLoc.isOffline()) "${targetLoc.speedKmh.toInt()} km/h" else "On Route",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TransitGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Route: $cleanStart ➔ $cleanDest",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TransitGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
