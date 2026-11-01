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

    val cleanStart = schedule.route.startPoint.ifBlank { "Central Terminal" }
    val cleanDest = schedule.route.destination.ifBlank { "Airport Hub" }

    DisposableEffect(Unit) {
        onDispose {
            navManager.onDestroy()
            NavigationService.stopService(context)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
                ).setMinUpdateIntervalMillis(1000L).build()

                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null && loc.accuracy <= 50f) {
                            lastLat = loc.latitude
                            lastLng = loc.longitude
                            currentSpeedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else 0f
                            currentHeading = if (loc.hasBearing()) loc.bearing else 0f

                            // Update local voice navigation engine
                            navManager.onLocationUpdated(loc)

                            // Broadcast live GPS position to Supabase / Real-Time Database for Admin and Commuter tracking
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

                            if (navState.isNavigating) {
                                NavigationService.updateService(
                                    context,
                                    navState.currentInstruction,
                                    "${navState.totalEtaMinutes} min • ${navState.totalDistanceRemainingKm} km remaining"
                                )
                            }
                        }
                    }
                }, android.os.Looper.getMainLooper())
            } catch (_: SecurityException) {}
        }
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2000L
                ).setMinUpdateIntervalMillis(1000L).build()

                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null && loc.accuracy <= 50f) {
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

                            if (navState.isNavigating) {
                                NavigationService.updateService(
                                    context,
                                    navState.currentInstruction,
                                    "${navState.totalEtaMinutes} min • ${navState.totalDistanceRemainingKm} km remaining"
                                )
                            }
                        }
                    }
                }, android.os.Looper.getMainLooper())
            } catch (_: SecurityException) {}
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Driver Live Navigation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("Bus: ${schedule.bus.busNumber} • Route: ${schedule.route.routeName}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
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
                routeName = schedule.route.routeName,
                startPoint = cleanStart,
                destination = cleanDest,
                startLat = lastLat ?: schedule.route.startLat,
                startLng = lastLng ?: schedule.route.startLng,
                destLat = schedule.route.destLat,
                destLng = schedule.route.destLng,
                onBack = onBack
            )
        }
    }
}
