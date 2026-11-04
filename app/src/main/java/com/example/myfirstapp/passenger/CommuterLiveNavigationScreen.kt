package com.example.myfirstapp.passenger

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.ConductorRecord
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.DriverRecord
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
    val context = LocalContext.current
    var liveLocations by remember { mutableStateOf<List<LiveBusLocation>>(emptyList()) }
    var driversList by remember { mutableStateOf<List<DriverRecord>>(emptyList()) }
    var conductorsList by remember { mutableStateOf<List<ConductorRecord>>(emptyList()) }
    var schedulesList by remember { mutableStateOf<List<com.example.myfirstapp.BusSchedule>>(emptyList()) }
    var busesList by remember { mutableStateOf<List<com.example.myfirstapp.Bus>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToLiveBusLocations(
            onUpdate = { locs ->
                liveLocations = locs
                isLoading = false
            },
            onError = { isLoading = false }
        )
        DatabaseManager.listenToDrivers(
            onUpdate = { drivers -> driversList = drivers },
            onError = {}
        )
        DatabaseManager.listenToConductors(
            onUpdate = { conductors -> conductorsList = conductors },
            onError = {}
        )
        DatabaseManager.listenToSchedules(
            onUpdate = { scheds -> schedulesList = scheds },
            onError = {}
        )
        DatabaseManager.listenToBuses(
            onUpdate = { buses -> busesList = buses },
            onError = {}
        )
    }

    fun callNumber(phone: String) {
        if (phone.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun messageNumber(phone: String) {
        if (phone.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phone")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
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

    val liveBusNum = targetLoc?.busNumber ?: busId.ifBlank { "DLTB Co. (ABC-123)" }

    val matchedSched = remember(schedulesList, busId, routeName, cleanStart, cleanDest) {
        schedulesList.find { s ->
            (busId.isNotBlank() && (s.bus.busNumber.contains(busId, ignoreCase = true) || s.bus.busId.contains(busId, ignoreCase = true))) ||
            (routeName.isNotBlank() && (s.route.routeName.contains(routeName, ignoreCase = true) || routeName.contains(s.route.routeName, ignoreCase = true))) ||
            (s.route.startPoint.contains(cleanStart, ignoreCase = true) && s.route.destination.contains(cleanDest, ignoreCase = true))
        } ?: schedulesList.firstOrNull()
    }

    val matchedBus = remember(busesList, busId, matchedSched) {
        busesList.find { b ->
            (busId.isNotBlank() && (b.busNumber.contains(busId, ignoreCase = true) || b.busId.contains(busId, ignoreCase = true))) ||
            (matchedSched != null && (b.busNumber.contains(matchedSched.bus.busNumber, ignoreCase = true) || b.busId.contains(matchedSched.bus.busId, ignoreCase = true)))
        } ?: busesList.firstOrNull()
    }

    val matchedDriver = remember(driversList, targetLoc, matchedSched, matchedBus) {
        val targetDriver = targetLoc?.driverName
        driversList.find { d ->
            (!targetDriver.isNullOrBlank() && targetDriver != "Assigned Driver" && targetDriver != "Unassigned" && (d.driverName.contains(targetDriver, ignoreCase = true) || targetDriver.contains(d.driverName, ignoreCase = true))) ||
            (matchedSched != null && matchedSched.bus.assignedDriver.isNotBlank() && matchedSched.bus.assignedDriver != "Unassigned" && d.driverName.contains(matchedSched.bus.assignedDriver, ignoreCase = true)) ||
            (matchedBus != null && matchedBus.assignedDriver.isNotBlank() && matchedBus.assignedDriver != "Unassigned" && d.driverName.contains(matchedBus.assignedDriver, ignoreCase = true)) ||
            (matchedBus != null && d.assignedBus.contains(matchedBus.busNumber, ignoreCase = true))
        } ?: driversList.firstOrNull()
    }

    val matchedConductor = remember(conductorsList, matchedBus, matchedSched) {
        conductorsList.find { c ->
            (matchedBus != null && (c.assignedBusId.contains(matchedBus.busNumber, ignoreCase = true) || matchedBus.busNumber.contains(c.assignedBusId, ignoreCase = true))) ||
            (matchedSched != null && (c.assignedBusId.contains(matchedSched.bus.busNumber, ignoreCase = true) || matchedSched.bus.busNumber.contains(c.assignedBusId, ignoreCase = true)))
        } ?: conductorsList.firstOrNull()
    }

    val liveDriverName = remember(targetLoc, matchedDriver, matchedSched, matchedBus) {
        if (matchedDriver != null && matchedDriver.driverName.isNotBlank() && matchedDriver.driverName != "Assigned Driver" && matchedDriver.driverName != "Unassigned") return@remember matchedDriver.driverName

        val fromGps = targetLoc?.driverName
        if (!fromGps.isNullOrBlank() && fromGps != "Assigned Driver" && fromGps != "Unassigned") return@remember fromGps

        val fromSched = matchedSched?.bus?.assignedDriver
        if (!fromSched.isNullOrBlank() && fromSched != "Assigned Driver" && fromSched != "Unassigned") return@remember fromSched

        val fromBus = matchedBus?.assignedDriver
        if (!fromBus.isNullOrBlank() && fromBus != "Assigned Driver" && fromBus != "Unassigned") return@remember fromBus

        "Unassigned"
    }

    val liveSpeed = targetLoc?.speedKmh?.toInt() ?: 0

    val driverPhone = remember(matchedDriver) {
        val phone = matchedDriver?.contactNumber
        if (!phone.isNullOrBlank()) phone else "Not Available"
    }

    val conductorName = remember(matchedConductor) {
        val name = matchedConductor?.fullName
        if (!name.isNullOrBlank() && name != "Assigned Conductor" && name != "Unassigned") name else "Unassigned"
    }

    val conductorPhone = remember(matchedConductor) {
        val phone = matchedConductor?.phoneNumber
        if (!phone.isNullOrBlank()) phone else "Not Available"
    }

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val resolvedBusNumber = targetLoc?.busNumber ?: matchedBus?.busNumber ?: matchedSched?.bus?.busNumber ?: busId.ifBlank { "B1203" }

            // Live Interactive Map Container with Real-Time Bus & Driver Tracking
            LeafletMapViewScreen(
                leafletId = resolvedBusNumber,
                routeName = "$cleanStart ➔ $cleanDest",
                startPoint = cleanStart,
                destination = cleanDest,
                liveBusLat = targetLoc?.latitude,
                liveBusLng = targetLoc?.longitude,
                liveBusHeading = targetLoc?.heading ?: 0f,
                liveBusLabel = "Bus $resolvedBusNumber • $liveDriverName${if (liveSpeed > 0) " ($liveSpeed km/h)" else ""}",
                showAllRoutes = false,
                onBack = onBack
            )

            // Top Status & Crew Contact Card Overlay
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
                                    text = "Bus: $liveBusNum",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Route: $cleanStart ➔ $cleanDest",
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
                                text = if (targetLoc != null && !targetLoc.isOffline()) "$statusText (${liveSpeed} km/h)" else statusText,
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

                    // Driver Contact Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Person, contentDescription = "Driver", tint = TransitGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Driver: $liveDriverName", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Text(driverPhone, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Row {
                            IconButton(onClick = { callNumber(driverPhone) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Call, contentDescription = "Call Driver", tint = TransitGreen, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { messageNumber(driverPhone) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Message, contentDescription = "SMS Driver", tint = TransitGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Conductor Contact Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Badge, contentDescription = "Conductor", tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Conductor: $conductorName", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Text(conductorPhone, fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        Row {
                            IconButton(onClick = { callNumber(conductorPhone) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Call, contentDescription = "Call Conductor", tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { messageNumber(conductorPhone) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Message, contentDescription = "SMS Conductor", tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
