package com.example.myfirstapp.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.Bus
import com.example.myfirstapp.BusRoute
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.DriverRecord
import com.example.myfirstapp.LiveBusLocation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveBusTrackingScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    var liveLocations by remember { mutableStateOf<List<LiveBusLocation>>(emptyList()) }
    var busesList by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var driversList by remember { mutableStateOf<List<DriverRecord>>(emptyList()) }
    var schedulesList by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var routesList by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Moving", "Stopped", "Offline"
    var selectedBusLocation by remember { mutableStateOf<LiveBusLocation?>(null) }
    var isMapViewExpanded by remember { mutableStateOf(false) }

    // Real-Time Subscriptions to DatabaseManager
    LaunchedEffect(Unit) {
        DatabaseManager.listenToLiveBusLocations(
            onUpdate = { locations ->
                liveLocations = locations
                isLoading = false
            },
            onError = { isLoading = false }
        )
        DatabaseManager.listenToBuses(
            onUpdate = { fetchedBuses -> busesList = fetchedBuses }
        )
        DatabaseManager.listenToDrivers(
            onUpdate = { fetchedDrivers -> driversList = fetchedDrivers }
        )
        DatabaseManager.listenToSchedules(
            onUpdate = { fetchedSchedules -> schedulesList = fetchedSchedules }
        )
        DatabaseManager.listenToRoutes(
            onUpdate = { fetchedRoutes -> routesList = fetchedRoutes }
        )
    }

    // Combine all buses from Supabase (live GPS + registered fleet + scheduled buses)
    val allTrackedBuses = remember(liveLocations, busesList, schedulesList, routesList) {
        val resultList = mutableListOf<LiveBusLocation>()
        val processedBusKeys = mutableSetOf<String>()

        // 1. Add active live GPS locations from Supabase bus_locations table
        liveLocations.forEach { loc ->
            resultList.add(loc)
            if (loc.busId.isNotBlank()) processedBusKeys.add(loc.busId.trim().lowercase())
            if (loc.busNumber.isNotBlank()) processedBusKeys.add(loc.busNumber.trim().lowercase())
        }

        // 2. Add registered buses from Supabase buses table
        busesList.forEach { bus ->
            val cleanId = bus.busId.trim().lowercase()
            val cleanNum = bus.busNumber.trim().lowercase()
            if (!processedBusKeys.contains(cleanId) && !processedBusKeys.contains(cleanNum) && bus.busNumber.isNotBlank()) {
                val matchedSched = schedulesList.find { s ->
                    s.bus.busNumber.equals(bus.busNumber, ignoreCase = true) || s.bus.busId.equals(bus.busId, ignoreCase = true)
                }
                val matchedRoute = matchedSched?.route ?: routesList.find { r -> r.routeName.equals(bus.route, ignoreCase = true) }
                val startName = matchedRoute?.startPoint ?: if (bus.route.contains(" ➔ ")) bus.route.split(" ➔ ")[0] else bus.route

                val (startLat, startLng) = if (matchedRoute != null && matchedRoute.startLat != 0.0) {
                    Pair(matchedRoute.startLat, matchedRoute.startLng)
                } else {
                    resolveTerminalCoords(startName, false)
                }

                val offlineLoc = LiveBusLocation(
                    busId = bus.busId.ifBlank { bus.busNumber },
                    busNumber = bus.busNumber,
                    driverName = bus.assignedDriver.ifBlank { "Unassigned" },
                    routeName = matchedRoute?.routeName ?: bus.route.ifBlank { "Scheduled Route" },
                    latitude = startLat,
                    longitude = startLng,
                    speedKmh = 0f,
                    heading = 0f,
                    status = "Offline",
                    timestamp = System.currentTimeMillis() - 300_000L
                )
                resultList.add(offlineLoc)
                if (cleanId.isNotBlank()) processedBusKeys.add(cleanId)
                if (cleanNum.isNotBlank()) processedBusKeys.add(cleanNum)
            }
        }

        // 3. Add schedules from Supabase bus_schedules table
        schedulesList.forEach { sched ->
            val cleanId = sched.bus.busId.trim().lowercase()
            val cleanNum = sched.bus.busNumber.trim().lowercase()
            if (!processedBusKeys.contains(cleanId) && !processedBusKeys.contains(cleanNum) && sched.bus.busNumber.isNotBlank()) {
                val (startLat, startLng) = if (sched.route.startLat != 0.0) {
                    Pair(sched.route.startLat, sched.route.startLng)
                } else {
                    resolveTerminalCoords(sched.route.startPoint, false)
                }

                val schedLoc = LiveBusLocation(
                    busId = sched.bus.busId.ifBlank { sched.bus.busNumber },
                    busNumber = sched.bus.busNumber,
                    driverName = sched.bus.assignedDriver.ifBlank { "Unassigned" },
                    routeName = sched.route.routeName,
                    latitude = startLat,
                    longitude = startLng,
                    speedKmh = 0f,
                    heading = 0f,
                    status = "Offline",
                    timestamp = System.currentTimeMillis() - 300_000L
                )
                resultList.add(schedLoc)
                if (cleanId.isNotBlank()) processedBusKeys.add(cleanId)
                if (cleanNum.isNotBlank()) processedBusKeys.add(cleanNum)
            }
        }

        resultList
    }

    // Filter live bus locations based on search and status filter
    val filteredLiveLocations = remember(allTrackedBuses, searchQuery, selectedFilter) {
        allTrackedBuses.filter { loc ->
            val status = loc.computedStatus()
            val matchesFilter = when (selectedFilter) {
                "Moving" -> status == "Moving"
                "Stopped" -> status == "Stopped"
                "Offline" -> status == "Offline"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    loc.busNumber.contains(searchQuery, ignoreCase = true) ||
                    loc.busId.contains(searchQuery, ignoreCase = true) ||
                    loc.driverName.contains(searchQuery, ignoreCase = true) ||
                    loc.routeName.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    // Summary counts
    val movingCount = remember(allTrackedBuses) { allTrackedBuses.count { it.computedStatus() == "Moving" } }
    val stoppedCount = remember(allTrackedBuses) { allTrackedBuses.count { it.computedStatus() == "Stopped" } }
    val offlineCount = remember(allTrackedBuses) { allTrackedBuses.count { it.computedStatus() == "Offline" } }

    fun callDriverPhone(phone: String) {
        if (phone.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun smsDriverPhone(phone: String) {
        if (phone.isNotBlank()) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phone")
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    // Determine target bus & route for LeafletMapViewScreen (matching DriverLiveNavigationScreen.kt reference)
    // Keep selectedBus synced with latest live location update for that bus ID / number
    val currentSelectedBus = remember(selectedBusLocation, filteredLiveLocations) {
        selectedBusLocation?.let { sel ->
            filteredLiveLocations.find { 
                (it.busId.isNotBlank() && it.busId.equals(sel.busId, ignoreCase = true)) ||
                (it.busNumber.isNotBlank() && it.busNumber.equals(sel.busNumber, ignoreCase = true))
            } ?: sel
        }
    }

    val selectedRoute = remember(routesList, currentSelectedBus) {
        routesList.find { r ->
            currentSelectedBus != null && (r.routeName.equals(currentSelectedBus.routeName, ignoreCase = true) ||
                    currentSelectedBus.routeName.contains(r.startPoint, ignoreCase = true))
        }
    }

    val startPointName = remember(selectedRoute, currentSelectedBus) {
        selectedRoute?.startPoint ?: currentSelectedBus?.routeName?.let {
            if (it.contains(" ➔ ")) it.split(" ➔ ")[0] else if (it.contains(" - ")) it.split(" - ")[0] else "Tuy"
        } ?: "Tuy"
    }

    val destPointName = remember(selectedRoute, currentSelectedBus) {
        selectedRoute?.destination ?: currentSelectedBus?.routeName?.let {
            if (it.contains(" ➔ ")) it.split(" ➔ ")[1] else if (it.contains(" - ")) it.split(" - ")[1] else "Pasay"
        } ?: "Pasay"
    }

    val displayRouteName = remember(currentSelectedBus) {
        currentSelectedBus?.routeName?.ifBlank { "Tuy ➔ Pasay" } ?: "Tuy ➔ Pasay"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Live Bus Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Real-time GPS Tracking • ${allTrackedBuses.size} Buses Tracked",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            DatabaseManager.listenToLiveBusLocations(
                                onUpdate = { locations ->
                                    liveLocations = locations
                                    isLoading = false
                                },
                                onError = { isLoading = false }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminGreen)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(AdminBg)
        ) {
            // Stats Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    title = "Total Fleet",
                    count = allTrackedBuses.size,
                    color = AdminGreen,
                    icon = Icons.Default.DirectionsBus,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    title = "Moving",
                    count = movingCount,
                    color = Color(0xFF2E7D32),
                    icon = Icons.Default.Navigation,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    title = "Stopped",
                    count = stoppedCount,
                    color = Color(0xFFE65100),
                    icon = Icons.Default.PauseCircle,
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    title = "Offline",
                    count = offlineCount,
                    color = Color.Gray,
                    icon = Icons.Default.SignalCellularOff,
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar & Filter Chips
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Bus #, Driver, or Route...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AdminGreen) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminGreen,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf("All", "Moving", "Stopped", "Offline")
                        items(filters) { f ->
                            FilterChip(
                                selected = selectedFilter == f,
                                onClick = { selectedFilter = f },
                                label = { Text(f, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdminGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Live Interactive Map View Container using LeafletMapViewScreen (Same Reference Component as DriverLiveNavigationScreen.kt)
            val mapHeightModifier = if (isMapViewExpanded) Modifier.weight(1f) else Modifier.height(280.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(mapHeightModifier)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                LeafletMapViewScreen(
                    leafletId = currentSelectedBus?.busNumber ?: "AdminTrackerMap",
                    routeName = displayRouteName,
                    startPoint = startPointName,
                    destination = destPointName,
                    startLat = selectedRoute?.startLat?.takeIf { it != 0.0 },
                    startLng = selectedRoute?.startLng?.takeIf { it != 0.0 },
                    destLat = selectedRoute?.destLat?.takeIf { it != 0.0 },
                    destLng = selectedRoute?.destLng?.takeIf { it != 0.0 },
                    liveBusLat = currentSelectedBus?.latitude?.takeIf { it != 0.0 },
                    liveBusLng = currentSelectedBus?.longitude?.takeIf { it != 0.0 },
                    liveBusHeading = currentSelectedBus?.heading ?: 0f,
                    liveBusLabel = currentSelectedBus?.let { "Bus ${it.busNumber} (${it.computedStatus()})" } ?: "",
                    allLiveBuses = filteredLiveLocations,
                    showAllRoutes = selectedBusLocation == null,
                    customRoutes = routesList,
                    showTopBar = false,
                    showBottomCard = false,
                    onBack = onBack
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = AdminGreen
                    )
                }

                // Selected Bus Focus Banner / Reset Overview Button
                if (selectedBusLocation != null) {
                    Surface(
                        color = AdminGreen,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clickable { selectedBusLocation = null }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Focused: Bus ${currentSelectedBus?.busNumber ?: ""} • Tap to View All",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Map Expand/Collapse Toggle Floating Button
                IconButton(
                    onClick = { isMapViewExpanded = !isMapViewExpanded },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isMapViewExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Expand Map",
                        tint = AdminGreen
                    )
                }
            }

            // List of Buses Driving / Tracked
            if (!isMapViewExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Driving Buses (${filteredLiveLocations.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdminGreen
                        )
                        if (filteredLiveLocations.isNotEmpty()) {
                            Text(
                                text = "Auto-updating live GPS",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (filteredLiveLocations.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank() || selectedFilter != "All") "No buses matching filter criteria" else "No buses actively driving right now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When drivers start navigation in Driver Live Navigation Screen, their live GPS updates will appear here on the map.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredLiveLocations) { busLoc ->
                                val driverRecord = remember(driversList, busLoc.driverName) {
                                    driversList.find {
                                        it.driverName.equals(busLoc.driverName, ignoreCase = true) ||
                                                (it.assignedBus.isNotBlank() && it.assignedBus.equals(busLoc.busNumber, ignoreCase = true))
                                    }
                                }

                                val isThisBusSelected = selectedBusLocation?.let {
                                    (it.busId.isNotBlank() && it.busId.equals(busLoc.busId, ignoreCase = true)) ||
                                    (it.busNumber.isNotBlank() && it.busNumber.equals(busLoc.busNumber, ignoreCase = true))
                                } ?: false

                                LiveBusCard(
                                    location = busLoc,
                                    driverRecord = driverRecord,
                                    isSelected = isThisBusSelected,
                                    onSelect = {
                                        if (isThisBusSelected) {
                                            selectedBusLocation = null
                                        } else {
                                            selectedBusLocation = busLoc
                                        }
                                    },
                                    onCallDriver = { phone -> callDriverPhone(phone) },
                                    onSmsDriver = { phone -> smsDriverPhone(phone) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = count.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
                Text(
                    text = title,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LiveBusCard(
    location: LiveBusLocation,
    driverRecord: DriverRecord?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onCallDriver: (String) -> Unit,
    onSmsDriver: (String) -> Unit
) {
    val status = location.computedStatus()
    val badgeBg = when (status) {
        "Moving" -> Color(0xFFE8F5E9)
        "Stopped" -> Color(0xFFFFF3E0)
        else -> Color(0xFFEEEEEE)
    }
    val badgeFg = when (status) {
        "Moving" -> Color(0xFF116530)
        "Stopped" -> Color(0xFFE65100)
        else -> Color(0xFF616161)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = if (isSelected) BorderStroke(2.dp, AdminGreen) else BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AdminGreen.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = AdminGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bus ${location.busNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AdminGreen
                        )
                        Text(
                            text = location.routeName.ifBlank { "Active Driving Route" },
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(badgeFg, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = status,
                            color = badgeFg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Driver: ${location.driverName.ifBlank { "Unassigned" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Speed: ${String.format(java.util.Locale.getDefault(), "%.1f", location.speedKmh)} km/h • Heading: ${formatHeading(location.heading)}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRelativeTime(location.timestamp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Text(
                        text = "%.4f, %.4f".format(location.latitude, location.longitude),
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val driverPhone = driverRecord?.contactNumber ?: ""
                if (driverPhone.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onCallDriver(driverPhone) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AdminGreen)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = AdminGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 11.sp, color = AdminGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = { onSmsDriver(driverPhone) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AdminGreen)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "SMS", tint = AdminGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", fontSize = 11.sp, color = AdminGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGreen)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Focus", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus on Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun resolveTerminalCoords(locationName: String, isDestination: Boolean): Pair<Double, Double> {
    val name = locationName.lowercase().trim()
    return when {
        name.contains("san pedro") -> Pair(14.0496, 121.1371)
        name.contains("nasugbu") -> Pair(14.0722, 120.6311)
        name.contains("palico") -> Pair(14.0728, 120.6983)
        name.contains("tuy") -> Pair(14.0200, 120.7300)
        name.contains("lemery") -> Pair(13.8822, 120.9111)
        name.contains("batangas") -> Pair(13.7565, 121.0583)
        name.contains("lipa") -> Pair(13.9419, 121.1644)
        name.contains("tanauan") -> Pair(14.0861, 121.1500)
        name.contains("calamba") -> Pair(14.2117, 121.1653)
        name.contains("tagaytay") -> Pair(14.1153, 120.9621)
        name.contains("pasay") -> Pair(14.5378, 120.9992)
        name.contains("cubao") -> Pair(14.6195, 121.0511)
        name.contains("pitx") -> Pair(14.5092, 120.9917)
        name.contains("manila") || name.contains("central") -> Pair(14.5995, 120.9842)
        name.contains("airport") || name.contains("naia") -> Pair(14.5204, 121.0122)
        else -> if (isDestination) Pair(14.5378, 120.9992) else Pair(14.0200, 120.7300)
    }
}

private fun formatHeading(degrees: Float): String {
    if (degrees < 0) return "N/A"
    val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = (((degrees % 360) + 360) % 360 / 45).toInt() % 8
    return "${directions[index]} (${degrees.toInt()}°)"
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp) / 1000
    return when {
        diff < 5 -> "Just now"
        diff < 60 -> "${diff}s ago"
        diff < 3600 -> "${diff / 60}m ago"
        else -> "${diff / 3600}h ago"
    }
}
