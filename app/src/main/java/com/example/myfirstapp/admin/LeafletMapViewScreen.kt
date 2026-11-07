package com.example.myfirstapp.admin

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myfirstapp.BusRoute
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.navigation.NavigationManager
import com.example.myfirstapp.navigation.NavigationService
import java.io.File

class NavVoiceBridge(
    private val onSpeak: (String) -> Unit,
    private val onBannerUpdate: (String) -> Unit
) {
    @JavascriptInterface
    fun speakInstruction(instruction: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onSpeak(instruction)
        }
    }

    @JavascriptInterface
    fun updateBannerText(text: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onBannerUpdate(text)
        }
    }
}

@Composable
fun LeafletMapViewScreen(
    leafletId: String = "L001",
    routeName: String = "",
    startPoint: String = "",
    destination: String = "",
    startLat: Double? = null,
    startLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null,
    liveBusLat: Double? = null,
    liveBusLng: Double? = null,
    liveBusHeading: Float = 0f,
    liveBusLabel: String = "",
    allLiveBuses: List<com.example.myfirstapp.LiveBusLocation> = emptyList(),
    isSingleTerminal: Boolean = false,
    showAllRoutes: Boolean = false,
    customRoutes: List<BusRoute> = emptyList(),
    isDriverNav: Boolean = false,
    showTopBar: Boolean = false,
    showBottomCard: Boolean = false,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var isMapLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val navManager = remember { NavigationManager(context) }
    val navState by navManager.state.collectAsState()

    var lastCurrentLat by remember { mutableStateOf<Double?>(null) }
    var lastCurrentLng by remember { mutableStateOf<Double?>(null) }

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
                
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastCurrentLat = loc.latitude
                        lastCurrentLng = loc.longitude

                        navManager.onLocationUpdated(loc)

                        val bearing = if (loc.hasBearing()) loc.bearing else 0f
                        webViewRef?.evaluateJavascript("if(window.updateBusLocation && !window.isSimulationRunning) { window.updateBusLocation(${loc.latitude}, ${loc.longitude}, ${bearing}); }", null)
                    }
                }

                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2500L
                ).setMinUpdateIntervalMillis(1500L).build()

                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) {
                            lastCurrentLat = loc.latitude
                            lastCurrentLng = loc.longitude

                            navManager.onLocationUpdated(loc)

                            if (navState.isNavigating) {
                                NavigationService.updateService(
                                    context,
                                    navState.currentInstruction,
                                    "${navState.totalEtaMinutes} min • ${navState.totalDistanceRemainingKm} km remaining"
                                )
                            }

                            val bearing = if (loc.hasBearing()) loc.bearing else 0f
                            webViewRef?.evaluateJavascript("if(window.updateBusLocation && !window.isSimulationRunning) { window.updateBusLocation(${loc.latitude}, ${loc.longitude}, ${bearing}); }", null)
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

                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastCurrentLat = loc.latitude
                        lastCurrentLng = loc.longitude

                        navManager.onLocationUpdated(loc)

                        val bearing = if (loc.hasBearing()) loc.bearing else 0f
                        webViewRef?.evaluateJavascript("if(window.updateBusLocation && !window.isSimulationRunning) { window.updateBusLocation(${loc.latitude}, ${loc.longitude}, ${bearing}); }", null)
                    }
                }

                val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 2500L
                ).setMinUpdateIntervalMillis(1500L).build()

                fusedLocationClient.requestLocationUpdates(locationRequest, object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val loc = result.lastLocation
                        if (loc != null) {
                            lastCurrentLat = loc.latitude
                            lastCurrentLng = loc.longitude

                            navManager.onLocationUpdated(loc)

                            if (navState.isNavigating) {
                                NavigationService.updateService(
                                    context,
                                    navState.currentInstruction,
                                    "${navState.totalEtaMinutes} min • ${navState.totalDistanceRemainingKm} km remaining"
                                )
                            }

                            val bearing = if (loc.hasBearing()) loc.bearing else 0f
                            webViewRef?.evaluateJavascript("if(window.updateBusLocation && !window.isSimulationRunning) { window.updateBusLocation(${loc.latitude}, ${loc.longitude}, ${bearing}); }", null)
                        }
                    }
                }, android.os.Looper.getMainLooper())
            } catch (_: SecurityException) {}
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Live sync all active routes & schedules from Cloud Firestore for Real-Time Tracker mode
    val firestoreRoutes = remember { mutableStateListOf<BusRoute>() }
    val firestoreSchedules = remember { mutableStateListOf<BusSchedule>() }

    LaunchedEffect(showAllRoutes) {
        if (showAllRoutes) {
            DatabaseManager.listenToRoutes(
                onUpdate = { fetchedRoutes ->
                    firestoreRoutes.clear()
                    firestoreRoutes.addAll(fetchedRoutes)
                }
            )
            DatabaseManager.listenToSchedules(
                onUpdate = { fetchedSchedules ->
                    firestoreSchedules.clear()
                    firestoreSchedules.addAll(fetchedSchedules)
                }
            )
        }
    }

    val schedulesJsonData = remember(firestoreSchedules.toList()) {
        val sb = StringBuilder("[")
        firestoreSchedules.forEachIndexed { idx, s ->
            if (idx > 0) sb.append(",")
            val cleanBusNum = s.bus.busNumber.replace("'", "\\'")
            val cleanRoute = s.route.routeName.replace("'", "\\'")
            val cleanDriver = s.bus.assignedDriver.replace("'", "\\'")
            val cleanStatus = s.status.replace("'", "\\'")
            sb.append("{\"busNumber\":\"$cleanBusNum\",\"route\":\"$cleanRoute\",\"driver\":\"$cleanDriver\",\"status\":\"$cleanStatus\",\"sLat\":${s.route.startLat},\"sLng\":${s.route.startLng}}")
        }
        sb.append("]")
        sb.toString()
    }

    val activeRoutesList = remember(customRoutes, firestoreRoutes.toList(), showAllRoutes) {
        if (showAllRoutes) {
            if (customRoutes.isNotEmpty()) customRoutes else firestoreRoutes.toList()
        } else emptyList()
    }

    // Build JSON Array string representing all active routes for Leaflet JS engine
    val routesJsonData = remember(activeRoutesList) {
        if (showAllRoutes && activeRoutesList.isNotEmpty()) {
            val routeColors = listOf("#116530", "#1565C0", "#E65100", "#7B1FA2", "#C62828", "#00838F")
            val sb = StringBuilder("[")
            activeRoutesList.forEachIndexed { idx, r ->
                val cleanRName = r.routeName.replace("'", "\\'")
                val cleanStartName = r.startPoint.replace("'", "\\'")
                val cleanDestName = r.destination.replace("'", "\\'")
                val color = routeColors[idx % routeColors.size]

                sb.append("{\"id\":\"${r.routeId}\",\"name\":\"$cleanRName\",\"start\":\"$cleanStartName\",\"dest\":\"$cleanDestName\",")
                sb.append("\"sLat\":${r.startLat},\"sLng\":${r.startLng},\"dLat\":${r.destLat},\"dLng\":${r.destLng},\"color\":\"$color\"}")
                if (idx < activeRoutesList.size - 1) sb.append(",")
            }
            sb.append("]")
            sb.toString()
        } else "[]"
    }

    // Clean Start Point
    val cleanStart = remember(startPoint, routeName) {
        if (startPoint.isNotBlank() && startPoint != "Origin" && startPoint != "Central Terminal") {
            startPoint.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" ➔ ")) {
            val raw = routeName.split(" ➔ ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" - ")) {
            val raw = routeName.split(" - ")[0].trim()
            raw.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.isNotBlank()) {
            routeName.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else {
            "Central Terminal"
        }
    }

    // Clean Destination Point
    val cleanDest = remember(destination, routeName) {
        if (destination.isNotBlank() && destination != "Destination" && destination != "Airport Hub") {
            destination.replace(Regex("""^(Route:\s*|Route\s+[A-Za-z0-9-]+:\s*)+""", RegexOption.IGNORE_CASE), "").trim()
        } else if (routeName.contains(" ➔ ")) {
            routeName.split(" ➔ ")[1].trim()
        } else if (routeName.contains(" - ")) {
            routeName.split(" - ")[1].trim()
        } else {
            "Airport Hub"
        }
    }

    val cleanStartJS = remember(cleanStart) { cleanStart.replace("'", "\\'") }
    val cleanDestJS = remember(cleanDest) { cleanDest.replace("'", "\\'") }

    val startLatJS = if (startLat != null && startLat != 0.0) startLat.toString() else "null"
    val startLngJS = if (startLng != null && startLng != 0.0) startLng.toString() else "null"
    val destLatJS = if (destLat != null && destLat != 0.0) destLat.toString() else "null"
    val destLngJS = if (destLng != null && destLng != 0.0) destLng.toString() else "null"

    val liveBusLatJS = if (liveBusLat != null && liveBusLat != 0.0) liveBusLat.toString() else "null"
    val liveBusLngJS = if (liveBusLng != null && liveBusLng != 0.0) liveBusLng.toString() else "null"
    val liveBusLabelJS = remember(liveBusLabel) { liveBusLabel.replace("'", "\\'") }

    val allLiveBusesJsonData = remember(allLiveBuses) {
        val sb = StringBuilder("[")
        allLiveBuses.forEachIndexed { idx, bus ->
            if (idx > 0) sb.append(",")
            val cleanBusNum = bus.busNumber.replace("'", "\\'")
            val cleanBusId = bus.busId.replace("'", "\\'")
            val cleanDriver = bus.driverName.replace("'", "\\'")
            val cleanRoute = bus.routeName.replace("'", "\\'")
            val cleanStatus = bus.computedStatus().replace("'", "\\'")
            sb.append("{\"busNumber\":\"$cleanBusNum\",\"busId\":\"$cleanBusId\",\"driver\":\"$cleanDriver\",\"route\":\"$cleanRoute\",\"status\":\"$cleanStatus\",\"lat\":${bus.latitude},\"lng\":${bus.longitude},\"heading\":${bus.heading},\"speed\":${bus.speedKmh}}")
        }
        sb.append("]")
        sb.toString()
    }

    LaunchedEffect(liveBusLabel, isMapLoaded) {
        if (isMapLoaded && liveBusLabel.isNotBlank()) {
            val cleanLabel = liveBusLabel.replace("'", "\\'")
            webViewRef?.evaluateJavascript(
                "var labelElem = document.getElementById('live-marker-label'); if(labelElem) { labelElem.innerText = '$cleanLabel'; }",
                null
            )
        }
    }

    LaunchedEffect(allLiveBusesJsonData, isMapLoaded, showAllRoutes) {
        if (isMapLoaded && allLiveBusesJsonData.isNotBlank() && showAllRoutes) {
            webViewRef?.evaluateJavascript(
                "if(window.updateAllBusLocations) { window.updateAllBusLocations($allLiveBusesJsonData); }",
                null
            )
        }
    }

    LaunchedEffect(liveBusLat, liveBusLng, liveBusHeading, liveBusLabel, isMapLoaded) {
        if (isMapLoaded && liveBusLat != null && liveBusLng != null && liveBusLat != 0.0 && liveBusLng != 0.0) {
            val cleanLabel = liveBusLabel.replace("'", "\\'")
            val autoCenter = isDriverNav
            webViewRef?.evaluateJavascript(
                "if(window.updateBusLocation) { window.updateBusLocation($liveBusLat, $liveBusLng, $liveBusHeading, '$cleanLabel', $autoCenter); }",
                null
            )
        }
    }

    val leafletHtmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css" />
            <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js"></script>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                html, body {
                    height: 100%; width: 100%;
                    overflow: hidden; background-color: #f0f7f2;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                #map-wrapper {
                    position: relative;
                    width: 100%;
                    height: 100vh;
                }
                #canvas-layer {
                    position: absolute;
                    top: 0; left: 0; width: 100%; height: 100%;
                    z-index: 1;
                }
                #map-layer {
                    position: absolute;
                    top: 0; left: 0; width: 100%; height: 100%;
                    z-index: 2;
                }
                .leaflet-popup-content {
                    font-size: 13px;
                    font-weight: bold;
                    color: #116530;
                }
                .leaflet-marker-icon {
                    transition: transform 0.4s ease-out, top 0.4s ease-out, left 0.4s ease-out;
                }
            </style>
        </head>
        <body>
            <div id="map-wrapper">
                <canvas id="canvas-layer"></canvas>
                <div id="map-layer"></div>
            </div>
            <script>
                var cleanStartName = "$cleanStartJS";
                var cleanDestName = "$cleanDestJS";
                var isSingleMode = $isSingleTerminal;
                var showAllRoutesMode = $showAllRoutes;
                var isDriverNavMode = $isDriverNav;
                var routesData = $routesJsonData;
                var schedulesData = $schedulesJsonData;
                var allLiveBusesData = $allLiveBusesJsonData;

                var paramStartLat = $startLatJS;
                var paramStartLng = $startLngJS;
                var paramDestLat = $destLatJS;
                var paramDestLng = $destLngJS;

                var paramLiveBusLat = $liveBusLatJS;
                var paramLiveBusLng = $liveBusLngJS;
                var paramLiveBusHeading = $liveBusHeading;
                var paramLiveBusLabel = "$liveBusLabelJS";
                var isUserInteracting = false;

                var knownLocations = {
                    "city terminal": [14.5995, 120.9842],
                    "central terminal": [14.5995, 120.9842],
                    "central manila": [14.5995, 120.9842],
                    "manila central": [14.5995, 120.9842],
                    "manila": [14.5995, 120.9842],
                    "airport hub": [14.5204, 121.0122],
                    "naia": [14.5204, 121.0122],
                    "pasay": [14.5378, 120.9992],
                    "pasay terminal": [14.5378, 120.9992],
                    "cubao station": [14.6195, 121.0511],
                    "cubao": [14.6195, 121.0511],
                    "quezon city": [14.6760, 121.0437],
                    "pitx express": [14.5092, 120.9917],
                    "pitx": [14.5092, 120.9917],
                    "parañaque": [14.5092, 120.9917],
                    "regional routes": [14.5204, 121.0122],
                    "regional": [14.5204, 121.0122],
                    "nasugbu": [14.0722, 120.6311],
                    "philhealth nasugbu": [14.0722, 120.6311],
                    "palico grand terminal": [14.0728, 120.6983],
                    "palico terminal": [14.0728, 120.6983],
                    "palico nasugbu": [14.0728, 120.6983],
                    "palico": [14.0728, 120.6983],
                    "lemery": [13.8822, 120.9111],
                    "gmq lemery": [13.8822, 120.9111],
                    "tuy": [14.0200, 120.7300],
                    "hydromed tuy": [14.0200, 120.7300],
                    "batangas": [13.7565, 121.0583],
                    "batangas city": [13.7565, 121.0583],
                    "lipa": [13.9419, 121.1644],
                    "lipa city": [13.9419, 121.1644],
                    "tanauan": [14.0861, 121.1500],
                    "sto. tomas": [14.1083, 121.1417],
                    "santo tomas": [14.1083, 121.1417],
                    "calamba": [14.2117, 121.1653],
                    "biñan": [14.3333, 121.0833],
                    "binan": [14.3333, 121.0833],
                    "santa rosa": [14.3167, 121.1167],
                    "sta. rosa": [14.3167, 121.1167],
                    "san pedro": [14.3583, 121.0583],
                    "tagaytay": [14.1153, 120.9621],
                    "city center": [14.5800, 121.0000],
                    "makati": [14.5547, 121.0244],
                    "taguig": [14.5176, 121.0509],
                    "alabang": [14.4172, 121.0400],
                    "muntinlupa": [14.4081, 121.0415],
                    "cavite": [14.4791, 120.8970],
                    "bacoor": [14.4602, 120.9631],
                    "dasmarinas": [14.3294, 120.9367],
                    "dasmariñas": [14.3294, 120.9367],
                    "imus": [14.4297, 120.9367]
                };

                function getCoordsForText(text, isDest) {
                    if (!text || text.trim().length === 0) {
                        return isDest ? [14.5204, 121.0122] : [14.5995, 120.9842];
                    }
                    var lower = text.trim().toLowerCase().replace(/^(route:\s*|route\s+[a-z0-9-]+:\s*)+/gi, '').trim();
                    for (var key in knownLocations) {
                        if (lower.includes(key)) {
                            return knownLocations[key];
                        }
                    }
                    var hash = 0;
                    for (var i = 0; i < text.length; i++) {
                        hash = text.charCodeAt(i) + ((hash << 5) - hash);
                    }
                    var latOffset = ((Math.abs(hash) % 150) / 1000.0) * (isDest ? -1 : 1);
                    var lngOffset = (((Math.abs(hash) / 150) % 150) / 1000.0) * (isDest ? 1 : -1);
                    var baseLat = isDest ? 14.5092 : 14.5995;
                    var baseLng = isDest ? 120.9917 : 120.9842;
                    return [baseLat + latOffset, baseLng + lngOffset];
                }

                var startCoords = (paramStartLat !== null && paramStartLng !== null && paramStartLat !== 0.0 && paramStartLng !== 0.0) ? [paramStartLat, paramStartLng] : getCoordsForText(cleanStartName, false);
                var destCoords = (paramDestLat !== null && paramDestLng !== null && paramDestLat !== 0.0 && paramDestLng !== 0.0) ? [paramDestLat, paramDestLng] : getCoordsForText(cleanDestName, true);

                // --- LAYER 1: Immediate Self-Contained Vector Canvas Map Engine ---
                function drawVectorMap() {
                    var cvs = document.getElementById('canvas-layer');
                    if (!cvs) return;
                    var ctx = cvs.getContext('2d');
                    var w = cvs.width = window.innerWidth || document.documentElement.clientWidth || 360;
                    var h = cvs.height = window.innerHeight || document.documentElement.clientHeight || 600;

                    ctx.clearRect(0, 0, w, h);

                    // Background tint
                    ctx.fillStyle = '#f0f7f2';
                    ctx.fillRect(0, 0, w, h);

                    // Draw Road Grid / Map Lines
                    ctx.strokeStyle = '#d3e4d7';
                    ctx.lineWidth = 1.5;
                    for (var x = 0; x < w; x += 40) {
                        ctx.beginPath();
                        ctx.moveTo(x, 0);
                        ctx.lineTo(x, h);
                        ctx.stroke();
                    }
                    for (var y = 0; y < h; y += 40) {
                        ctx.beginPath();
                        ctx.moveTo(0, y);
                        ctx.lineTo(w, y);
                        ctx.stroke();
                    }

                    if (isSingleMode) {
                        // Single Terminal Pin Centered
                        var px = w * 0.5;
                        var py = h * 0.5;

                        ctx.fillStyle = '#116530';
                        ctx.beginPath();
                        ctx.arc(px, py, 16, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.strokeStyle = '#FFFFFF';
                        ctx.lineWidth = 3.5;
                        ctx.stroke();

                        ctx.fillStyle = '#116530';
                        ctx.font = 'bold 12px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.fillRect(px - 90, py - 42, 180, 24);
                        ctx.fillStyle = '#FFFFFF';
                        ctx.fillText('🚌 ' + cleanStartName, px, py - 25);
                    } else if (showAllRoutesMode) {
                        // All Routes Tracker Mode Canvas Grid
                        ctx.fillStyle = '#116530';
                        ctx.font = 'bold 13px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.fillText('🚌 Real-Time Bus Tracker Active (' + routesData.length + ' Routes)', w * 0.5, 30);
                    } else {
                        // 2-Point Route
                        var p1 = { x: w * 0.28, y: h * 0.35 };
                        var p2 = { x: w * 0.72, y: h * 0.65 };
                        var pMid = { x: (p1.x + p2.x) / 2 + 35, y: (p1.y + p2.y) / 2 - 45 };

                        ctx.strokeStyle = '#116530';
                        ctx.lineWidth = 6;
                        ctx.setLineDash([10, 6]);
                        ctx.beginPath();
                        ctx.moveTo(p1.x, p1.y);
                        ctx.quadraticCurveTo(pMid.x, pMid.y, p2.x, p2.y);
                        ctx.stroke();
                        ctx.setLineDash([]);

                        ctx.fillStyle = '#1976D2';
                        ctx.beginPath();
                        ctx.arc(p1.x, p1.y, 14, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.strokeStyle = '#FFFFFF';
                        ctx.lineWidth = 3;
                        ctx.stroke();

                        ctx.fillStyle = '#1976D2';
                        ctx.font = 'bold 12px sans-serif';
                        ctx.textAlign = 'center';
                        ctx.fillRect(p1.x - 65, p1.y - 38, 130, 22);
                        ctx.fillStyle = '#FFFFFF';
                        ctx.fillText('🚌 ' + cleanStartName, p1.x, p1.y - 23);

                        ctx.fillStyle = '#D32F2F';
                        ctx.beginPath();
                        ctx.arc(p2.x, p2.y, 14, 0, Math.PI * 2);
                        ctx.fill();
                        ctx.strokeStyle = '#FFFFFF';
                        ctx.lineWidth = 3;
                        ctx.stroke();

                        ctx.fillStyle = '#D32F2F';
                        ctx.fillRect(p2.x - 65, p2.y + 16, 130, 22);
                        ctx.fillStyle = '#FFFFFF';
                        ctx.fillText('🚌 ' + cleanDestName, p2.x, p2.y + 31);
                    }

                    // Compass Rose Overlay
                    ctx.fillStyle = '#116530';
                    ctx.font = 'bold 11px sans-serif';
                    ctx.fillText('🧭 N', w - 30, 30);
                    ctx.fillText('🚌 Bus Tracker GPS Active', w - 75, h - 25);
                }

                drawVectorMap();
                window.addEventListener('resize', drawVectorMap);

                // --- LAYER 2: Leaflet Tile Engine ---
                window.busFleetMarkers = window.busFleetMarkers || {};
                window.updateAllBusLocations = function(busesList) {
                    if (!busesList || !window.map) return;
                    var activeKeys = {};
                    busesList.forEach(function(bus) {
                        var key = bus.busNumber || bus.busId;
                        if (!key || bus.lat === null || bus.lng === null || bus.lat === 0.0 || bus.lng === 0.0) return;
                        activeKeys[key] = true;
                        var coords = [bus.lat, bus.lng];
                        var labelText = "🚌 Bus " + bus.busNumber + " (" + bus.status + ")";
                        var statusColor = bus.status === 'Moving' ? '#116530' : (bus.status === 'Stopped' ? '#E65100' : '#616161');

                        if (window.busFleetMarkers[key]) {
                            var marker = window.busFleetMarkers[key];
                            marker.setLatLng(coords);
                            var iconDiv = marker.getElement();
                            if (iconDiv) {
                                var labelElem = iconDiv.querySelector('.bus-fleet-label');
                                if (labelElem) labelElem.innerText = labelText;
                                var badgeElem = iconDiv.querySelector('.bus-fleet-badge');
                                if (badgeElem) badgeElem.style.background = statusColor;
                            }
                        } else {
                            var busIcon = L.divIcon({
                                className: 'live-bus-fleet-marker',
                                html: "<div class='bus-fleet-badge' style='background:" + statusColor + ";color:white;padding:6px 12px;border-radius:16px;font-weight:bold;font-size:11px;border:2px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'><span class='bus-fleet-label'>" + labelText + "</span></div>",
                                iconSize: [160, 30],
                                iconAnchor: [80, 15]
                            });
                            var marker = L.marker(coords, { icon: busIcon }).addTo(window.map)
                                .bindPopup("<b>Bus: " + bus.busNumber + "</b><br>Driver: " + bus.driver + "<br>Route: " + bus.route + "<br>Status: " + bus.status + "<br>Speed: " + bus.speed + " km/h");
                            window.busFleetMarkers[key] = marker;
                        }
                    });

                    for (var k in window.busFleetMarkers) {
                        if (!activeKeys[k]) {
                            window.map.removeLayer(window.busFleetMarkers[k]);
                            delete window.busFleetMarkers[k];
                        }
                    }
                };

                function initLeafletEngine() {
                    if (typeof L === 'undefined') {
                        setTimeout(initLeafletEngine, 150);
                        return;
                    }

                    try {
                        var mapElem = document.getElementById('map-layer');
                        if (!mapElem) return;

                        mapElem.style.zIndex = "10";

                        if (showAllRoutesMode && routesData && routesData.length > 0) {
                            // REAL-TIME MULTI-ROUTE TRACKER ENGINE
                            var map = L.map('map-layer', { zoomControl: true }).setView([14.3000, 120.9500], 10);
                            window.map = map;

                            window.map.on('movestart dragstart zoomstart', function(e) {
                                if (e && e.originalEvent) { isUserInteracting = true; }
                            });

                            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '© OpenStreetMap'
                            }).addTo(map);

                            var allBounds = [];
                            var routeColors = ["#116530", "#1565C0", "#E65100", "#7B1FA2", "#C62828", "#00838F"];

                            if (schedulesData && schedulesData.length > 0) {
                                schedulesData.forEach(function(sched, idx) {
                                    var bCoords = (sched.sLat && sched.sLng && sched.sLat !== 0.0) ? [sched.sLat, sched.sLng] : getCoordsForText(sched.route, false);
                                    var busIcon = L.divIcon({
                                        className: 'live-bus-fleet-' + idx,
                                        html: "<div style='background:#116530;color:white;padding:6px 12px;border-radius:16px;font-weight:bold;font-size:11px;border:2px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'>🚌 Bus " + sched.busNumber + " (" + sched.status + ")</div>",
                                        iconSize: [150, 30],
                                        iconAnchor: [75, 15]
                                    });
                                    L.marker(bCoords, { icon: busIcon }).addTo(map)
                                        .bindPopup("<b>Bus: " + sched.busNumber + "</b><br>Route: " + sched.route + "<br>Driver: " + sched.driver + "<br>Status: " + sched.status);
                                });
                            }

                            routesData.forEach(function(route, idx) {
                                var rColor = route.color || routeColors[idx % routeColors.size];

                                var rStartCoords = (route.sLat && route.sLng && route.sLat !== 0.0) ? [route.sLat, route.sLng] : getCoordsForText(route.start, false);
                                var rDestCoords = (route.dLat && route.dLng && route.dLat !== 0.0) ? [route.dLat, route.dLng] : getCoordsForText(route.dest, true);

                                allBounds.push(rStartCoords);
                                allBounds.push(rDestCoords);

                                var origIcon = L.divIcon({
                                    className: 'custom-multi-orig-' + idx,
                                    html: "<div style='background:" + rColor + ";color:white;padding:5px 10px;border-radius:14px;font-weight:bold;font-size:11px;border:2px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'>🚌 " + route.id + ": " + route.start.split(' ')[0] + "</div>",
                                    iconSize: [110, 28],
                                    iconAnchor: [55, 14]
                                });

                                var destIcon = L.divIcon({
                                    className: 'custom-multi-dest-' + idx,
                                    html: "<div style='background:#D32F2F;color:white;padding:5px 10px;border-radius:14px;font-weight:bold;font-size:11px;border:2px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'>🚌 " + route.id + ": " + route.dest.split(' ')[0] + "</div>",
                                    iconSize: [110, 28],
                                    iconAnchor: [55, 14]
                                });

                                L.marker(rStartCoords, { icon: origIcon }).addTo(map)
                                    .bindPopup("<b>Bus Route " + route.id + " (Origin)</b><br>" + route.name + "<br><b>Location:</b> " + route.start);

                                L.marker(rDestCoords, { icon: destIcon }).addTo(map)
                                    .bindPopup("<b>Bus Route " + route.id + " (Destination)</b><br>" + route.name + "<br><b>Location:</b> " + route.dest);

                                var osrmUrl = "https://router.project-osrm.org/route/v1/driving/" +
                                    rStartCoords[1] + "," + rStartCoords[0] + ";" +
                                    rDestCoords[1] + "," + rDestCoords[0] +
                                    "?overview=full&geometries=geojson";

                                fetch(osrmUrl)
                                    .then(function(res) { return res.json(); })
                                    .then(function(data) {
                                        if (data && data.routes && data.routes.length > 0) {
                                            L.geoJSON(data.routes[0].geometry, {
                                                style: { color: rColor, weight: 5, opacity: 0.85, lineCap: 'round', lineJoin: 'round' }
                                            }).addTo(map).bindPopup("<b>Bus Route " + route.id + ": " + route.name + "</b>");
                                        } else {
                                            L.polyline([rStartCoords, rDestCoords], { color: rColor, weight: 5, opacity: 0.85 }).addTo(map);
                                        }
                                    })
                                    .catch(function(e) {
                                        L.polyline([rStartCoords, rDestCoords], { color: rColor, weight: 5, opacity: 0.85 }).addTo(map);
                                    });
                            });

                            if (allBounds.length > 0) {
                                map.fitBounds(allBounds, { padding: [40, 40] });
                            }

                            if (allLiveBusesData && allLiveBusesData.length > 0) {
                                window.updateAllBusLocations(allLiveBusesData);
                            }

                        } else if (isSingleMode) {
                            // Single Terminal Hub Mode (1 Pin Only with Bus Icon)
                            var map = L.map('map-layer', { zoomControl: true }).setView(startCoords, 15);
                            window.map = map;

                            window.map.on('movestart dragstart zoomstart', function(e) {
                                if (e && e.originalEvent) { isUserInteracting = true; }
                            });

                            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '© OpenStreetMap'
                            }).addTo(map);

                            var terminalIcon = L.divIcon({
                                className: 'custom-term-icon',
                                html: "<div style='background:#116530;color:white;padding:8px 14px;border-radius:18px;font-weight:bold;font-size:13px;border:2.5px solid white;box-shadow:0 4px 10px rgba(0,0,0,0.4);white-space:nowrap;'>🚌 " + cleanStartName + "</div>",
                                iconSize: [160, 36],
                                iconAnchor: [80, 18]
                            });

                            L.marker(startCoords, { icon: terminalIcon }).addTo(map)
                                .bindPopup("<b>Bus Terminal: " + cleanStartName + "</b><br><span style='color:#116530;'>Active Bus Hub • " + cleanDestName + "</span>").openPopup();
                        } else {
                            // Single Two-Point Driving Route Mode with Bus Icons
                            window.map = L.map('map-layer', { zoomControl: true }).setView(startCoords, 12);

                            window.map.on('movestart dragstart zoomstart', function(e) {
                                if (e && e.originalEvent) { isUserInteracting = true; }
                            });

                            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                maxZoom: 19,
                                attribution: '© OpenStreetMap'
                            }).addTo(window.map);

                            var blueIcon = L.divIcon({
                                className: 'custom-orig-icon',
                                html: "<div style='background:#1976D2;color:white;padding:6px 12px;border-radius:16px;font-weight:bold;font-size:12px;border:2px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'>🚌 " + cleanStartName + "</div>",
                                iconSize: [140, 32],
                                iconAnchor: [70, 16]
                            });

                            var redIcon = L.divIcon({
                                className: 'custom-dest-icon',
                                html: "<div style='background:#D32F2F;color:white;padding:6px 12px;border-radius:16px;font-weight:bold;font-size:12px;border:3px solid white;box-shadow:0 3px 8px rgba(0,0,0,0.35);white-space:nowrap;'>🚌 " + cleanDestName + "</div>",
                                iconSize: [140, 32],
                                iconAnchor: [70, 16]
                            });

                            L.marker(startCoords, { icon: blueIcon }).addTo(window.map)
                                .bindPopup("<b>Bus Route Origin: " + cleanStartName + "</b>");

                            L.marker(destCoords, { icon: redIcon }).addTo(window.map)
                                .bindPopup("<b>Bus Route Destination: " + cleanDestName + "</b>");

                            if (isDriverNavMode || (paramLiveBusLat !== null && paramLiveBusLng !== null && paramLiveBusLat !== 0.0 && paramLiveBusLng !== 0.0)) {
                                var initialBusPos = (paramLiveBusLat !== null && paramLiveBusLng !== null && paramLiveBusLat !== 0.0 && paramLiveBusLng !== 0.0) ? [paramLiveBusLat, paramLiveBusLng] : startCoords;
                                var initialMarkerLabel = paramLiveBusLabel || (isDriverNavMode ? "Driver Navigation" : "");

                                window.busLiveMarker = L.marker(initialBusPos, {
                                    icon: L.divIcon({
                                        className: 'live-driver-marker',
                                        html: "<div style='background:#116530;color:white;padding:6px 14px;border-radius:20px;font-weight:bold;font-size:12px;border:3px solid white;box-shadow:0 4px 12px rgba(0,0,0,0.4);white-space:nowrap;display:flex;align-items:center;gap:4px;'><span id='driver-nav-arrow' style='display:inline-block;transition:transform 0.3s ease;font-size:14px;'>➔</span> <span id='live-marker-label'>" + initialMarkerLabel + "</span></div>",
                                        iconSize: [160, 36],
                                        iconAnchor: [80, 18]
                                    })
                                }).addTo(window.map);
                            }

                            var lastLat = null;
                            var lastLng = null;

                            window.updateBusLocation = function(lat, lng, heading, labelText, autoCenter) {
                                if (lat === null || lng === null || lat === 0.0 || lng === 0.0) return;

                                lastLat = lat;
                                lastLng = lng;

                                if (window.busLiveMarker) {
                                    window.busLiveMarker.setLatLng([lat, lng]);
                                    if (autoCenter === true && !isUserInteracting && window.map) {
                                        window.map.panTo([lat, lng], { animate: true, duration: 0.5 });
                                    }
                                }

                                var arrowElem = document.getElementById('driver-nav-arrow');
                                if (arrowElem && heading !== undefined) {
                                    arrowElem.style.transform = 'rotate(' + heading + 'deg)';
                                }

                                var labelElem = document.getElementById('live-marker-label');
                                if (labelElem && labelText) {
                                    labelElem.innerText = labelText;
                                }
                            };

                            var osrmUrl = "https://router.project-osrm.org/route/v1/driving/" +
                                startCoords[1] + "," + startCoords[0] + ";" +
                                destCoords[1] + "," + destCoords[0] +
                                "?overview=full&geometries=geojson&steps=true";

                            fetch(osrmUrl)
                                .then(function(res) { return res.json(); })
                                .then(function(data) {
                                    if (data && data.routes && data.routes.length > 0) {
                                        var routeGeometry = data.routes[0].geometry;
                                        var drivingPolyline = L.geoJSON(routeGeometry, {
                                            style: { color: '#116530', weight: 6, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }
                                        }).addTo(window.map);

                                        window.map.fitBounds(drivingPolyline.getBounds(), { padding: [50, 50] });
                                    } else {
                                        drawFallbackPolyline(window.map, startCoords, destCoords);
                                    }
                                })
                                .catch(function(e) {
                                    drawFallbackPolyline(window.map, startCoords, destCoords);
                                });
                        }
                    } catch(e) {
                        console.log(e);
                    }
                }

                function drawFallbackPolyline(map, start, dest) {
                    var midLat = (start[0] + dest[0]) / 2.0 + 0.015;
                    var midLng = (start[1] + dest[1]) / 2.0 - 0.010;
                    var routePath = [start, [midLat, midLng], dest];
                    var polyline = L.polyline(routePath, { color: '#116530', weight: 6, opacity: 0.85 }).addTo(map);
                    map.fitBounds(polyline.getBounds(), { padding: [50, 50] });
                }

                setTimeout(initLeafletEngine, 200);
            </script>
        </body>
        </html>
    """.trimIndent()

    // Write HTML directly to local cache file for native WebView URL loading
    val mapHtmlFile = remember(leafletId, showAllRoutes, isSingleTerminal, isDriverNav, activeRoutesList.size) {
        try {
            val file = File(context.cacheDir, "leaflet_map_${if (showAllRoutes) "all_routes" else leafletId}.html")
            file.writeText(leafletHtmlContent, Charsets.UTF_8)
            file
        } catch (_: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
    ) {
        // Top Header Bar (Only rendered when showTopBar is true)
        if (showTopBar) {
            Surface(
                color = AdminGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (showAllRoutes) "Real-Time Bus Route Tracker" else if (isSingleTerminal) "Bus Terminal Hub Map" else "Bus Route Driving Map",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (showAllRoutes) "Tracking ${activeRoutesList.size} Active Bus Routes on OpenStreetMap" else if (isSingleTerminal) "Terminal: $cleanStart" else "$cleanStart ➔ $cleanDest",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (!isMapLoaded) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AdminGreen,
                trackColor = Color(0xFFE0E0E0)
            )
        }

        // Leaflet Map Webview Container
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
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

                        if (mapHtmlFile != null && mapHtmlFile.exists()) {
                            loadUrl("file://${mapHtmlFile.absolutePath}")
                        } else {
                            loadDataWithBaseURL("https://leafletjs.com", leafletHtmlContent, "text/html", "utf-8", null)
                        }
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Google Maps Style Top Turn Instruction Banner Card Overlay
            if (isDriverNav && navState.isNavigating) {
                Surface(
                    color = Color(0xFF116530),
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (navState.isOffRoute) Icons.Default.AltRoute else Icons.Default.Navigation,
                                contentDescription = "Turn Icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (navState.isNavigating) navState.currentInstruction else "Ready for Google Maps Voice Navigation",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (navState.isNavigating && navState.nextTurnDistanceMeters > 0) {
                            Text(
                                text = "Next maneuver in ${navState.nextTurnDistanceMeters} meters",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { navManager.toggleMute() }
                    ) {
                        Icon(
                            imageVector = if (navState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = Color.White
                        )
                    }
                }
            }

            // Floating My Location Button
            FloatingActionButton(
                onClick = {
                    if (lastCurrentLat != null && lastCurrentLng != null) {
                        webViewRef?.evaluateJavascript(
                            "if(window.map && window.busLiveMarker){ window.busLiveMarker.setLatLng([$lastCurrentLat, $lastCurrentLng]); window.map.panTo([$lastCurrentLat, $lastCurrentLng]); }",
                            null
                        )
                    }
                },
                containerColor = Color.White,
                contentColor = AdminGreen,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .size(48.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }

        // Bottom Google Maps Style Navigation Info Control Card (Only rendered when showBottomCard is true or actively navigating)
        if (showBottomCard || (isDriverNav && navState.isNavigating)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (navState.isNavigating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${navState.totalEtaMinutes} min",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = AdminGreen
                            )
                            Text(
                                text = "${navState.totalDistanceRemainingKm} km • Destination: $cleanDest",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                        }

                        Button(
                            onClick = {
                                navManager.stopNavigation()
                                NavigationService.stopService(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "End Nav", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("End Nav", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$cleanStart ➔ $cleanDest",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AdminGreen
                            )
                            Text(
                                text = "Real-time GPS voice navigation ready",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        if (isDriverNav && !isSingleTerminal && !showAllRoutes) {
                            Button(
                                onClick = {
                                    val startL = lastCurrentLat ?: (startLat ?: 14.5995)
                                    val startG = lastCurrentLng ?: (startLng ?: 120.9842)
                                    val destL = destLat ?: 14.5204
                                    val destG = destLng ?: 121.0122

                                    navManager.startNavigation(startL, startG, destL, destG, cleanDest)
                                    NavigationService.startService(
                                        context,
                                        "Navigating to $cleanDest",
                                        "Calculating ETA..."
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AdminGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Navigation, contentDescription = "Start", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Navigation", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Engine: Real GPS + OSRM Live Road Routing + Android Voice Engine", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
    }
}
}
