package com.example.myfirstapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
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
import com.example.myfirstapp.BusRoute
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.LiveBusLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveBusTrackingScreen(
    onBack: () -> Unit = {}
) {
    var registeredRoutes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var allSchedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var registeredBuses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var liveLocations by remember { mutableStateOf<List<LiveBusLocation>>(emptyList()) }

    var selectedRoute by remember { mutableStateOf<BusRoute?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToRoutes(
            onUpdate = { routes ->
                registeredRoutes = routes
            }
        )
        DatabaseManager.listenToSchedules(
            onUpdate = { fetchedSchedules ->
                allSchedules = fetchedSchedules
            }
        )
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

    // Filter ONLY routes that have an assigned bus / active schedule
    val assignedRoutes = remember(registeredRoutes, allSchedules, registeredBuses) {
        registeredRoutes.filter { route ->
            val hasSchedule = allSchedules.any { s ->
                !s.status.equals("Rejected", ignoreCase = true) &&
                (s.route.routeId == route.routeId ||
                 s.route.routeName.contains(route.routeName, ignoreCase = true) ||
                 s.route.destination.contains(route.destination, ignoreCase = true))
            }
            val hasBus = registeredBuses.any { b ->
                b.assignedDriver.isNotBlank() &&
                (b.busNumber.contains(route.routeId, ignoreCase = true) || b.busNumber.contains(route.routeName, ignoreCase = true))
            }
            hasSchedule || hasBus
        }
    }

    // Find latest upcoming schedule for the selected route
    val latestSchedule = remember(allSchedules, selectedRoute) {
        if (selectedRoute == null) null
        else {
            val matching = allSchedules.filter { s ->
                s.route.routeName.contains(selectedRoute!!.routeName, ignoreCase = true) ||
                s.route.routeId == selectedRoute!!.routeId ||
                s.route.destination.contains(selectedRoute!!.destination, ignoreCase = true)
            }
            val activeMatching = matching.filter { !it.status.equals("Rejected", ignoreCase = true) }
            val pool = if (activeMatching.isNotEmpty()) activeMatching else matching
            pool.lastOrNull() ?: pool.firstOrNull()
        }
    }

    val assignedBus = remember(registeredBuses, latestSchedule, selectedRoute) {
        if (latestSchedule != null) latestSchedule.bus
        else if (selectedRoute != null) {
            registeredBuses.find { b ->
                b.busNumber.contains(selectedRoute!!.routeName, ignoreCase = true) ||
                b.assignedDriver.isNotBlank()
            }
        } else null
    }

    val liveBusLoc = remember(liveLocations, assignedBus, latestSchedule) {
        val searchNum = assignedBus?.busNumber ?: latestSchedule?.bus?.busNumber ?: ""
        if (searchNum.isNotBlank()) {
            liveLocations.find { it.busNumber.contains(searchNum, ignoreCase = true) || it.busId == searchNum }
        } else null
    }

    val statusText = if (liveBusLoc != null && !liveBusLoc.isOffline()) {
        liveBusLoc.computedStatus()
    } else if (latestSchedule != null) {
        latestSchedule.status
    } else if (assignedBus != null && assignedBus.status.isNotBlank()) {
        assignedBus.status
    } else {
        "Active Route"
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Admin Fleet Live Tracker", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                            Text("Assigned Routes & Buses from Route Management", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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

                // Active Assigned Routes Selection Ribbon
                if (assignedRoutes.isNotEmpty()) {
                    Surface(
                        color = Color.White,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assigned Routes (${assignedRoutes.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AdminGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "${assignedRoutes.size} Assigned",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdminGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // "All Assigned Routes" Chip (Plots all 3 assigned routes simultaneously on OpenStreetMap)
                                item {
                                    val isAllSel = selectedRoute == null
                                    Card(
                                        modifier = Modifier
                                            .width(155.dp)
                                            .clickable { selectedRoute = null },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isAllSel) AdminGreen else Color(0xFFF0F4F1)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isAllSel) 3.dp else 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Map,
                                                    contentDescription = null,
                                                    tint = if (isAllSel) Color.White else AdminGreen,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "🌐 All Assigned Routes",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (isAllSel) Color.White else Color.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Show All ${assignedRoutes.size} Routes",
                                                fontSize = 10.sp,
                                                color = if (isAllSel) Color.White.copy(alpha = 0.85f) else Color.DarkGray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Individual Assigned Route Chips
                                items(assignedRoutes) { route ->
                                    val isSel = selectedRoute?.routeId == route.routeId

                                    Card(
                                        modifier = Modifier
                                            .width(155.dp)
                                            .clickable { selectedRoute = route },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isSel) AdminGreen else Color(0xFFF0F4F1)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSel) 3.dp else 1.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.AltRoute,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color.White else AdminGreen,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = route.routeName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (isSel) Color.White else Color.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${route.startPoint.split(" ")[0]} ➔ ${route.destination.split(" ")[0]}",
                                                fontSize = 10.sp,
                                                color = if (isSel) Color.White.copy(alpha = 0.85f) else Color.DarkGray,
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
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Interactive OpenStreetMap Engine with EXACT coordinates from RouteManagementScreen
            Box(modifier = Modifier.weight(1f)) {
                LeafletMapViewScreen(
                    leafletId = selectedRoute?.routeId ?: "ALL",
                    routeName = selectedRoute?.routeName ?: "Assigned Active Routes",
                    startPoint = selectedRoute?.startPoint ?: "Origin",
                    destination = selectedRoute?.destination ?: "Destination",
                    startLat = liveBusLoc?.latitude ?: selectedRoute?.startLat,
                    startLng = liveBusLoc?.longitude ?: selectedRoute?.startLng,
                    destLat = selectedRoute?.destLat,
                    destLng = selectedRoute?.destLng,
                    showAllRoutes = selectedRoute == null,
                    customRoutes = assignedRoutes,
                    showTopBar = false,
                    showBottomCard = false,
                    onBack = onBack
                )
            }

            // Bottom Info Card Overlay
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedRoute != null) {
                    val route = selectedRoute!!
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = route.routeName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = AdminGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (statusText) {
                                    "Moving", "Active", "In Transit" -> Color(0xFF4CAF50)
                                    "Stopped", "Scheduled" -> Color(0xFFFF9800)
                                    else -> Color.Gray
                                }
                            ) {
                                Text(
                                    text = statusText,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFFF0F2F0))
                        Spacer(modifier = Modifier.height(6.dp))

                        // Assigned Bus & Driver Info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = AdminGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Bus: ${assignedBus?.busNumber ?: latestSchedule?.bus?.busNumber ?: "Assigned Bus"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = AdminGreen, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Driver: ${latestSchedule?.bus?.assignedDriver ?: assignedBus?.assignedDriver ?: "Assigned Driver"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Latest Schedule Date & Departure/Arrival Times
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Latest Schedule: ${latestSchedule?.scheduleDate?.ifBlank { "Today" } ?: "Today"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminGreen
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (latestSchedule != null) "${latestSchedule.departureTime} - ${latestSchedule.arrivalTime}" else "Scheduled Trip",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }

                        if (liveBusLoc != null && !liveBusLoc.isOffline()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = AdminGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Live GPS Speed: ${liveBusLoc.speedKmh.toInt()} km/h",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdminGreen
                                )
                            }
                        }
                    }
                } else {
                    // All Assigned Routes Overview Summary Card
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌐 All Active Assigned Routes (${assignedRoutes.size})",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = AdminGreen
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "Multi-Fleet Live View",
                                    color = AdminGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Plotting all ${assignedRoutes.size} assigned routes simultaneously on OpenStreetMap. Click any route chip above to inspect its specific bus, schedule date, and driver details.",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
