package com.example.myfirstapp.passenger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.location.Location
import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myfirstapp.BusRoute
import com.example.myfirstapp.BusStop
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.LocationSuggestion
import com.example.myfirstapp.admin.AdminGreen
import com.example.myfirstapp.fetchLiveOpenStreetMapSuggestions

data class RecentSearchItem(
    val title: String,
    val address: String,
    val status: String = ""
)

// Javascript bridge for Leaflet location picker
class LeafletBridge(
    private val onPick: (String, String, Double, Double) -> Unit
) {
    @JavascriptInterface
    fun onLocationPicked(name: String, address: String, lat: Double, lng: Double) {
        onPick(name, address, lat, lng)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerSearchScreen(
    onBack: () -> Unit = {},
    onDestinationSelected: (String, String) -> Unit = { _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var schedules by remember { mutableStateOf<List<com.example.myfirstapp.BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val todayFullDateStr = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date())
    }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { fetchedSchedules ->
                schedules = fetchedSchedules.filter { sched ->
                    // 1. Only today's schedule
                    (sched.scheduleDate.isBlank() || sched.scheduleDate == todayFullDateStr) &&
                    // 2. Active status (not rejected, cancelled, or inactive)
                    !sched.status.equals("Rejected", ignoreCase = true) &&
                    !sched.status.equals("Cancelled", ignoreCase = true) &&
                    !sched.status.equals("Inactive", ignoreCase = true) &&
                    // 3. Must have available bus & assigned driver
                    sched.bus.busNumber.isNotBlank() &&
                    sched.bus.assignedDriver.isNotBlank() &&
                    !sched.bus.assignedDriver.equals("Unassigned", ignoreCase = true)
                }
                isLoading = false
            },
            onError = { isLoading = false }
        )
    }

    val filteredSchedules = remember(schedules, searchQuery) {
        if (searchQuery.isBlank()) {
            schedules
        } else {
            val q = searchQuery.trim().lowercase()
            schedules.filter { sched ->
                sched.route.routeName.lowercase().contains(q) ||
                sched.route.startPoint.lowercase().contains(q) ||
                sched.route.destination.lowercase().contains(q) ||
                sched.bus.busNumber.lowercase().contains(q) ||
                sched.bus.assignedDriver.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AdminGreen
                )
            }
            Text("Search Buses & Routes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Single Clean Type Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search bus, route, or destination...", color = Color.Gray, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = AdminGreen) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AdminGreen,
                unfocusedBorderColor = Color(0xFFD0D0D0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = if (searchQuery.isBlank()) "Today's Active Trips (${filteredSchedules.size})" else "Search Results (${filteredSchedules.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = "Showing available buses & assigned drivers",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Text(
                    text = todayFullDateStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdminGreen,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AdminGreen)
            }
        } else if (filteredSchedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No matching buses or routes found", fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Try searching for a different location or bus number", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSchedules) { schedule ->
                    val cleanStart = schedule.route.startPoint.ifBlank { "Tuy" }
                    val cleanDest = schedule.route.destination.ifBlank { "Pasay" }
                    val driverText = if (schedule.bus.assignedDriver.isNotBlank() && schedule.bus.assignedDriver != "Unassigned") schedule.bus.assignedDriver else "Assigned Driver"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDestinationSelected(cleanStart, cleanDest)
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = AdminGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = schedule.route.routeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )
                                }

                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = schedule.status.ifBlank { "Active" },
                                        fontSize = 11.sp,
                                        color = AdminGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = "Driver", tint = AdminGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Driver: $driverText", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bus: ${schedule.bus.busNumber} • Departure: ${schedule.departureTime} - Arrival: ${schedule.arrivalTime}",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Leaflet Map Location Picker Component.
 * Dynamically loads bus stops and terminals from Supabase.
 */
@Composable
fun LeafletLocationPickerModal(
    targetType: String,
    initialLocation: String,
    busStops: List<BusStop>,
    onLocationConfirmed: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedName by remember { mutableStateOf(if (initialLocation.isNotBlank() && initialLocation != "Current Location") initialLocation else "Central Terminal - Manila") }
    var selectedAddress by remember { mutableStateOf("Selected Location on Leaflet Map") }
    var selectedLat by remember { mutableDoubleStateOf(14.5995) }
    var selectedLng by remember { mutableDoubleStateOf(120.9842) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    val stopsJson = if (busStops.isEmpty()) {
        """
        [
            { name: "Central Terminal - Manila", address: "Central Manila, Metro Manila", lat: 14.5995, lng: 120.9842 },
            { name: "PITX Terminal - Parañaque", address: "Kennedy Rd, Tambo, Parañaque", lat: 14.5092, lng: 120.9917 }
        ]
        """
    } else {
        val sb = StringBuilder("[")
        busStops.forEachIndexed { index, stop ->
            if (index > 0) sb.append(",")
            sb.append("{ name: \"${stop.stopName}\", address: \"${stop.location}\", lat: ${stop.latitude}, lng: ${stop.longitude} }")
        }
        sb.append("]").toString()
    }

    val leafletPickerHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                body, html, #map { height: 100vh; width: 100vw; margin: 0; padding: 0; background-color: #f5f8f6; }
                .leaflet-popup-content { font-size: 13px; font-weight: bold; color: #116530; }
                .avail-badge { background-color: #116530; color: #ffffff; padding: 2px 6px; border-radius: 4px; font-size: 10px; font-weight: bold; }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    try {
                        var map = L.map('map').setView([14.5995, 120.9842], 11);
                        
                        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            attribution: '© OpenStreetMap'
                        }).addTo(map);

                        var activeMarker = null;

                        var availableLocations = $stopsJson;

                        var greenIcon = L.icon({
                            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-green.png',
                            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34],
                            shadowSize: [41, 41]
                        });

                        var redIcon = L.icon({
                            iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
                            shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                            iconSize: [25, 41],
                            iconAnchor: [12, 41],
                            popupAnchor: [1, -34],
                            shadowSize: [41, 41]
                        });

                        availableLocations.forEach(function(loc) {
                            var m = L.marker([loc.lat, loc.lng], { icon: greenIcon }).addTo(map);
                            m.bindPopup("<b>" + loc.name + "</b><br><span class='avail-badge'>Supabase Terminal</span><br>" + loc.address);
                            m.on('click', function() {
                                markSelected(loc.name, loc.address, loc.lat, loc.lng);
                            });
                        });

                        function markSelected(name, address, lat, lng) {
                            if (activeMarker) {
                                map.removeLayer(activeMarker);
                            }
                            activeMarker = L.marker([lat, lng], { icon: redIcon }).addTo(map);
                            activeMarker.bindPopup("<b>Selected $targetType</b><br>" + name).openPopup();
                            if (window.AndroidBridge) {
                                window.AndroidBridge.onLocationPicked(name, address, lat, lng);
                            }
                        }

                        markSelected("$selectedName", "Selected on Leaflet OpenStreetMap", 14.5995, 120.9842);

                        map.on('click', function(e) {
                            var lat = e.latlng.lat.toFixed(4);
                            var lng = e.latlng.lng.toFixed(4);
                            var name = "Picked Location (" + lat + ", " + lng + ")";
                            var addr = "Custom Location on Leaflet Map";
                            markSelected(name, addr, parseFloat(lat), parseFloat(lng));
                        });

                    } catch(e) {
                        console.log(e);
                    }
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F6))
    ) {
        Surface(
            color = AdminGreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Choose $targetType on Leaflet Map",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Tap any available terminal marker or tap anywhere on the map",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isMapLoaded = true
                            }
                        }
                        addJavascriptInterface(
                            LeafletBridge { name, address, lat, lng ->
                                handler.post {
                                    selectedName = name
                                    selectedAddress = address
                                    selectedLat = lat
                                    selectedLng = lng
                                }
                            },
                            "AndroidBridge"
                        )
                        loadDataWithBaseURL("https://leafletjs.com", leafletPickerHtml, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isMapLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AdminGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading Leaflet OpenStreetMap Location Picker...", fontSize = 13.sp, color = AdminGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (targetType == "Origin") Color(0xFFE3F2FD) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Selecting $targetType",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (targetType == "Origin") Color(0xFF1976D2) else Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "Lat: %.4f, Lng: %.4f".format(selectedLat, selectedLng),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (targetType == "Origin") Icons.Default.MyLocation else Icons.Default.Place,
                        contentDescription = null,
                        tint = if (targetType == "Origin") Color(0xFF1976D2) else Color(0xFFD32F2F),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(selectedName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                        Text(selectedAddress, fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedName = "Current Location (GPS)"
                            selectedAddress = "Your Current GPS Location"
                            onLocationConfirmed("Current Location", selectedLat, selectedLng)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Use GPS", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onLocationConfirmed(selectedName, selectedLat, selectedLng)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGreen),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set as $targetType", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

fun fetchPrecisePhilippineAddress(context: android.content.Context, lat: Double, lng: Double, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = java.net.URL("https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=18&addressdetails=1")
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "TransitGoBusApp/1.0")
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (connection.responseCode in 200..299) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val addressObj = json.optJSONObject("address")
                if (addressObj != null) {
                    val village = addressObj.optString("village", "")
                    val suburb = addressObj.optString("suburb", "")
                    val barangay = if (village.isNotBlank()) village else suburb
                    val town = addressObj.optString("town", "")
                    val municipality = addressObj.optString("municipality", "")
                    val city = addressObj.optString("city", "")
                    val townOrCity = if (town.isNotBlank()) town else if (municipality.isNotBlank()) municipality else city
                    val province = addressObj.optString("state", addressObj.optString("province", ""))

                    val parts = mutableListOf<String>()
                    if (barangay.isNotBlank()) parts.add("Brgy. $barangay")
                    if (townOrCity.isNotBlank()) parts.add(townOrCity)
                    if (province.isNotBlank()) parts.add(province)

                    val formatted = if (parts.isNotEmpty()) parts.joinToString(", ") else json.optString("display_name", "")
                    if (formatted.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            onResult(formatted.replace(", Philippines", "").trim())
                        }
                        return@launch
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val barangay = address.subLocality ?: address.featureName ?: ""
                        val municipality = address.locality ?: address.subAdminArea ?: ""
                        val province = address.adminArea ?: ""
                        val parts = listOf("Brgy. $barangay", municipality, province).filter { it.isNotBlank() && !it.matches(Regex("^[0-9]+$")) }.distinct()
                        val res = if (parts.isNotEmpty()) parts.joinToString(", ") else "GPS Location (%.4f, %.4f)".format(lat, lng)
                        CoroutineScope(Dispatchers.Main).launch {
                            onResult(res)
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            onResult("GPS Location (%.4f, %.4f)".format(lat, lng))
                        }
                    }
                }
                return@launch
            }
        } catch (_: Exception) {}

        withContext(Dispatchers.Main) {
            onResult("GPS Location (%.4f, %.4f)".format(lat, lng))
        }
    }
}
