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
    val context = LocalContext.current

    var origin by remember { mutableStateOf("Current Location") }
    var originFocused by remember { mutableStateOf(false) }
    var liveOriginSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }

    var destination by remember { mutableStateOf("") }
    var destinationFocused by remember { mutableStateOf(false) }
    var liveDestSuggestions by remember { mutableStateOf<List<LocationSuggestion>>(emptyList()) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        fetchPrecisePhilippineAddress(context, location.latitude, location.longitude) { resolved ->
                            origin = resolved
                            Toast.makeText(context, "Location resolved accurately!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Unable to get GPS location. Ensure GPS is enabled.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: SecurityException) {
                Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(origin) {
        if (origin.trim().length >= 2 && !origin.startsWith("GPS") && !origin.startsWith("Brgy.")) {
            delay(300)
            val fetched = fetchLiveOpenStreetMapSuggestions(origin)
            liveOriginSuggestions = fetched.distinctBy { it.title }
        } else {
            liveOriginSuggestions = emptyList()
        }
    }

    LaunchedEffect(destination) {
        if (destination.trim().length >= 2) {
            delay(300)
            val fetched = fetchLiveOpenStreetMapSuggestions(destination)
            liveDestSuggestions = fetched.distinctBy { it.title }
        } else {
            liveDestSuggestions = emptyList()
        }
    }
    
    var routes by remember { mutableStateOf<List<BusRoute>>(emptyList()) }
    var busStops by remember { mutableStateOf<List<BusStop>>(emptyList()) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToRoutes(
            onUpdate = { fetchedRoutes -> routes = fetchedRoutes },
            onError = {}
        )
        DatabaseManager.listenToBusStops(
            onUpdate = { fetchedStops -> busStops = fetchedStops },
            onError = {}
        )
    }

    val dynamicSearches = remember(routes, busStops) {
        val list = mutableListOf<RecentSearchItem>()
        busStops.forEach { stop ->
            list.add(RecentSearchItem(stop.stopName, stop.location, stop.status))
        }
        routes.forEach { route ->
            list.add(RecentSearchItem(route.routeName, "${route.startPoint} ➔ ${route.destination}", route.status))
        }
        if (list.isEmpty()) {
            listOf(
                RecentSearchItem("Central Terminal - Manila", "Central Manila, Metro Manila", "Open 24 Hours"),
                RecentSearchItem("Cubao Bus Station - Quezon City", "EDSA Corner Aurora Blvd, Cubao", "Open • Active Transit")
            )
        } else {
            list
        }
    }
    
    // Track which field is opening the Leaflet map location picker: "Origin" or "Destination" or null
    var pickerTarget by remember { mutableStateOf<String?>(null) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLabelColor = AdminGreen,
        unfocusedLabelColor = Color.DarkGray,
        focusedBorderColor = AdminGreen,
        unfocusedBorderColor = Color.LightGray
    )

    // Leaflet Map Location Picker Fullscreen Overlay
    if (pickerTarget != null) {
        val target = pickerTarget!!
        val currentVal = if (target == "Origin") origin else destination
        LeafletLocationPickerModal(
            targetType = target,
            initialLocation = currentVal,
            busStops = busStops,
            onLocationConfirmed = { selectedName, _, _ ->
                if (target == "Origin") {
                    origin = selectedName
                    Toast.makeText(context, "Origin set to: $selectedName", Toast.LENGTH_SHORT).show()
                } else {
                    destination = selectedName
                    Toast.makeText(context, "Destination set to: $selectedName", Toast.LENGTH_SHORT).show()
                }
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
        return
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
            Text("Where to go?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google Maps style Origin & Destination Input Card with Leaflet Map Picker integrations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Location Dots / Pin Indicator
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.size(12.dp)
                        ) {}
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("┊", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Destination Pin",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Input Text Fields Column
                    Column(modifier = Modifier.weight(1f)) {
                        // Origin Input with Live Search Autocomplete
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                OutlinedTextField(
                                    value = origin,
                                    onValueChange = {
                                        origin = it
                                        originFocused = true
                                    },
                                    label = { Text("Your location (Origin)", color = Color.DarkGray, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = textFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (originFocused && liveOriginSuggestions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            liveOriginSuggestions.forEach { suggestion ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            origin = suggestion.title
                                                            originFocused = false
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFE3F2FD),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.LocationOn,
                                                                contentDescription = null,
                                                                tint = Color(0xFF1976D2),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                                        Text(suggestion.subtitle, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }
                                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Destination Input with Live Search Autocomplete
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                OutlinedTextField(
                                    value = destination,
                                    onValueChange = {
                                        destination = it
                                        destinationFocused = true
                                    },
                                    label = { Text("Choose final destination", color = Color.DarkGray, fontSize = 12.sp) },
                                    placeholder = { Text("Search city, station, or terminal", color = Color.Gray) },
                                    singleLine = true,
                                    colors = textFieldColors,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (destinationFocused && liveDestSuggestions.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 220.dp)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            liveDestSuggestions.forEach { suggestion ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            destination = suggestion.title
                                                            destinationFocused = false
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        color = Color(0xFFFFEBEE),
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.DirectionsBus,
                                                                contentDescription = null,
                                                                tint = Color(0xFFD32F2F),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                                        Text(suggestion.subtitle, fontSize = 11.sp, color = Color.Gray)
                                                    }
                                                }
                                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Swap Origin ⇄ Destination Button
                    IconButton(onClick = {
                        val temp = origin
                        origin = destination.ifEmpty { "Current Location" }
                        destination = temp
                    }) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Swap Locations",
                            tint = AdminGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto GPS Button
                OutlinedButton(
                    onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AdminGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AdminGreen)
                ) {
                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto GPS (Detect Current Location)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Routes & Terminals List
        Text("Active Routes & Terminals", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(dynamicSearches) { searchItem ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            destination = searchItem.title
                            onDestinationSelected(origin, searchItem.title)
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF0F4F1),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Route/Terminal",
                                    tint = AdminGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(searchItem.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Text(searchItem.address, fontSize = 12.sp, color = Color.Gray)
                            if (searchItem.status.isNotEmpty()) {
                                Text(searchItem.status, fontSize = 11.sp, color = AdminGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Buses Button
        Button(
            onClick = {
                if (destination.isBlank()) {
                    Toast.makeText(context, "Please enter a destination or pick one on the Leaflet map", Toast.LENGTH_SHORT).show()
                } else {
                    onDestinationSelected(origin, destination)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = AdminGreen),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Search Buses & View Route Map", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
