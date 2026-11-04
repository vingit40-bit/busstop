package com.example.myfirstapp.driver

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.myfirstapp.LiveBusLocation
import com.example.myfirstapp.TransitGreen
import com.example.myfirstapp.admin.LeafletMapViewScreen
import com.example.myfirstapp.navigation.NavigationManager
import com.example.myfirstapp.navigation.NavigationService
import com.example.myfirstapp.user

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverLiveNavigationScreen(
    schedule: BusSchedule,
    user: user,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val navManager = remember { NavigationManager(context) }
    val navState by navManager.state.collectAsState()

    var lastLat by remember { mutableStateOf<Double?>(null) }
    var lastLng by remember { mutableStateOf<Double?>(null) }
    var currentSpeedKmh by remember { mutableStateOf(0f) }
    var currentHeading by remember { mutableStateOf(0f) }

    var isTripCompleted by remember { mutableStateOf(false) }
    var locationCallbackRef by remember { mutableStateOf<com.google.android.gms.location.LocationCallback?>(null) }
    val fusedLocationClient = remember { com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context) }

    val cleanStart = remember(schedule.route) {
        if (schedule.route.startPoint.isNotBlank() && schedule.route.startPoint != "Origin" && schedule.route.startPoint != "Central Terminal") {
            schedule.route.startPoint.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (schedule.route.routeName.contains(" ➔ ")) {
            val raw = schedule.route.routeName.split(" ➔ ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (schedule.route.routeName.contains(" - ")) {
            val raw = schedule.route.routeName.split(" - ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else {
            "Tuy"
        }
    }

    val cleanDest = remember(schedule.route) {
        if (schedule.route.destination.isNotBlank() && schedule.route.destination != "Destination" && schedule.route.destination != "Airport Hub") {
            schedule.route.destination.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (schedule.route.routeName.contains(" ➔ ")) {
            schedule.route.routeName.split(" ➔ ")[1].trim()
        } else if (schedule.route.routeName.contains(" - ")) {
            schedule.route.routeName.split(" - ")[1].trim()
        } else {
            "Pasay"
        }
    }

    val displayRouteName = remember(schedule.route.routeName) {
        val cleaned = schedule.route.routeName.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        if (cleaned.isNotBlank()) cleaned else schedule.route.routeName
    }

    // Automatically stop navigation service notification when driver exits navigation screen
    DisposableEffect(Unit) {
        onDispose {
            navManager.onDestroy()
            locationCallbackRef?.let { cb ->
                try {
                    fusedLocationClient.removeLocationUpdates(cb)
                } catch (_: Exception) {}
            }
            NavigationService.stopService(context)
        }
    }

    // Auto-detect arrival at destination terminal / bus stop (<= 150m)
    val distToDestMeters = remember(lastLat, lastLng, schedule.route) {
        if (lastLat != null && lastLng != null && schedule.route.destLat != 0.0 && schedule.route.destLng != 0.0) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(lastLat!!, lastLng!!, schedule.route.destLat, schedule.route.destLng, results)
            results[0].toInt()
        } else null
    }

    LaunchedEffect(distToDestMeters) {
        if (distToDestMeters != null && distToDestMeters <= 150 && !isTripCompleted) {
            isTripCompleted = true
            DatabaseManager.updateScheduleStatus(
                scheduleId = schedule.scheduleId,
                newStatus = "Completed",
                driverName = user.fullname,
                onSuccess = {
                    Toast.makeText(context, "Arrived at $cleanDest! Trip completed automatically.", Toast.LENGTH_LONG).show()
                    NavigationService.stopService(context)
                    navManager.stopNavigation()
                    onBack()
                },
                onFailure = {
                    NavigationService.stopService(context)
                    navManager.stopNavigation()
                    onBack()
                }
            )
        }
    }

    fun startLocationUpdates() {
        try {
            NavigationService.startService(
                context,
                "Navigating $displayRouteName",
                "Bus ${schedule.bus.busNumber} • Live GPS Navigation Active"
            )

            // Get last known location immediately
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !isTripCompleted) {
                    lastLat = loc.latitude
                    lastLng = loc.longitude
                    currentSpeedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                    currentHeading = if (loc.hasBearing()) loc.bearing else 0f

                    navManager.onLocationUpdated(loc)

                    val liveLocation = LiveBusLocation(
                        busId = schedule.bus.busId.ifBlank { schedule.bus.busNumber },
                        busNumber = schedule.bus.busNumber,
                        driverName = user.fullname.ifBlank { schedule.bus.assignedDriver },
                        routeName = schedule.route.routeName,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = currentSpeedKmh,
                        heading = currentHeading,
                        status = if (currentSpeedKmh > 3f) "Moving" else "Stopped",
                        timestamp = System.currentTimeMillis()
                    )
                    DatabaseManager.updateBusGpsLocation(liveLocation)
                }
            }

            // Continuous location updates
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
            ).setMinUpdateIntervalMillis(1000L).build()

            val callback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    if (isTripCompleted) return
                    val loc = result.lastLocation
                    if (loc != null) {
                        lastLat = loc.latitude
                        lastLng = loc.longitude
                        currentSpeedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                        currentHeading = if (loc.hasBearing()) loc.bearing else 0f

                        navManager.onLocationUpdated(loc)

                        val liveLocation = LiveBusLocation(
                            busId = schedule.bus.busId.ifBlank { schedule.bus.busNumber },
                            busNumber = schedule.bus.busNumber,
                            driverName = user.fullname.ifBlank { schedule.bus.assignedDriver },
                            routeName = schedule.route.routeName,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            speedKmh = currentSpeedKmh,
                            heading = currentHeading,
                            status = if (currentSpeedKmh > 3f) "Moving" else "Stopped",
                            timestamp = System.currentTimeMillis()
                        )
                        DatabaseManager.updateBusGpsLocation(liveLocation)

                        if (!isTripCompleted && navState.isNavigating) {
                            NavigationService.updateService(
                                context,
                                navState.currentInstruction,
                                "${navState.totalEtaMinutes} min • ${navState.totalDistanceRemainingKm} km remaining"
                            )
                        }
                    }
                }
            }

            locationCallbackRef = callback
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, android.os.Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        } else {
            Toast.makeText(context, "Location permission required for GPS navigation", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Driver Live Navigation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Bus: ${schedule.bus.busNumber} • Route: $displayRouteName", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
            // Live Interactive Map View Container
            LeafletMapViewScreen(
                leafletId = schedule.bus.busNumber,
                routeName = displayRouteName,
                startPoint = cleanStart,
                destination = cleanDest,
                startLat = if (schedule.route.startLat != 0.0) schedule.route.startLat else null,
                startLng = if (schedule.route.startLng != 0.0) schedule.route.startLng else null,
                destLat = if (schedule.route.destLat != 0.0) schedule.route.destLat else null,
                destLng = if (schedule.route.destLng != 0.0) schedule.route.destLng else null,
                isDriverNav = true,
                onBack = onBack
            )
        }
    }
}
