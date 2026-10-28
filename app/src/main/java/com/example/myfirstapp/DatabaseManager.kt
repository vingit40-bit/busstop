package com.example.myfirstapp

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class DriverRecord(
    val driverId: String,
    var driverName: String,
    var contactNumber: String,
    var licenseNumber: String,
    var licenseExpiry: String,
    var assignedBus: String,
    var employmentStatus: String,
    var email: String = "",
    var password: String = ""
)

data class ConductorRecord(
    val id: String,
    var fullName: String,
    var employeeId: String,
    var phoneNumber: String,
    var email: String,
    var username: String = "",
    var role: String = "Conductor",
    var assignedBusId: String = "",
    var status: String = "On Duty",
    var password: String = "password123"
)

data class BookingRecord(
    val bookingId: String,
    val busId: String,
    val routeName: String,
    val seats: String,
    val totalPrice: Double,
    val passengerEmail: String,
    val status: String = "Confirmed",
    val paymentMethod: String = "Cash",
    val receiptUrl: String = "",
    val paymentStatus: String = "Pending"
)

data class LocationSuggestion(
    val title: String,
    val subtitle: String,
    val lat: Double? = null,
    val lng: Double? = null
)

suspend fun fetchLiveOpenStreetMapSuggestions(query: String): List<LocationSuggestion> {
    if (query.trim().length < 2) return emptyList()
    return withContext(Dispatchers.IO) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=8&countrycodes=ph"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "TransitGoApp/1.0 (Android Native App)")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(jsonString)

                val results = mutableListOf<LocationSuggestion>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val displayName = obj.optString("display_name", "")
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)

                    val parts = displayName.split(",")
                    val shortTitle = parts.firstOrNull()?.trim() ?: displayName
                    val remainingSubtitle = if (parts.size > 1) parts.drop(1).joinToString(",").trim() else "OpenStreetMap Real Location"

                    results.add(
                        LocationSuggestion(
                            title = shortTitle,
                            subtitle = remainingSubtitle,
                            lat = if (lat != 0.0) lat else null,
                            lng = if (lon != 0.0) lon else null
                        )
                    )
                }
                results
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object DatabaseManager {
    private const val SUPABASE_URL = "https://ulhylotzjyvevtiufrmb.supabase.co/rest/v1"
    private const val SUPABASE_KEY = "sb_publishable_5oMPc30otC3gVjqvTH_SUA_JWwlIn9U"

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun JSONObject.safeString(key: String, default: String = ""): String {
        if (this.isNull(key)) return default
        val v = this.optString(key, default)
        return if (v == "null" || v.isEmpty()) default else v
    }

    private fun makeRequest(endpoint: String, method: String, jsonBody: String? = null): String? {
        return try {
            val url = URL("$SUPABASE_URL/$endpoint")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("apikey", SUPABASE_KEY)
                setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                doInput = true
                if (jsonBody != null && (method == "POST" || method == "PATCH" || method == "PUT")) {
                    doOutput = true
                }
            }

            if (jsonBody != null && (method == "POST" || method == "PATCH" || method == "PUT")) {
                OutputStreamWriter(connection.outputStream).use { it.write(jsonBody) }
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("Supabase", "Error $responseCode: $err")
                null
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Exception: ${e.localizedMessage}", e)
            null
        }
    }

    // --- USERS ---
    fun saveUser(
        user: user,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("user_id", user.userId)
                put("fullname", user.fullname)
                put("email", user.email)
                put("role", user.role)
                put("password", "")
            }.toString()

            try {
                val url = URL("$SUPABASE_URL/users")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                    doInput = true
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { it.write(json) }
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                    Log.e("Supabase", "Save user error: $err")
                    withContext(Dispatchers.Main) { onFailure(Exception(err)) }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Save user exception: ${e.localizedMessage}", e)
                withContext(Dispatchers.Main) { onFailure(e) }
            }
        }
    }

    fun getUser(
        userId: String,
        onSuccess: (user?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onSuccess(null)
            return
        }
        ioScope.launch {
            val res = makeRequest("users?user_id=eq.$userId", "GET")
            withContext(Dispatchers.Main) {
                try {
                    if (res != null) {
                        val arr = JSONArray(res)
                        if (arr.length() > 0) {
                            val obj = arr.getJSONObject(0)
                            val uId = obj.safeString("user_id", userId)
                            val fullname = obj.safeString("fullname", "Transit User")
                            val email = obj.safeString("email")
                            val role = obj.safeString("role", "Passenger")
                            val password = obj.safeString("password")
                            onSuccess(user(uId, fullname, email, password, role))
                        } else {
                            onSuccess(null)
                        }
                    } else {
                        onSuccess(null)
                    }
                } catch (e: Exception) {
                    onFailure(e)
                }
            }
        }
    }

    // --- BUSES ---
    fun saveBus(
        bus: Bus,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("bus_id", bus.busId)
                put("bus_number", bus.busNumber)
                put("plate_number", bus.plateNumber)
                put("bus_type", bus.busType)
                put("capacity", bus.capacity)
                put("assigned_driver", bus.assignedDriver)
                put("operator", bus.operator)
                put("route", bus.route)
                put("bus_stop", bus.busStop)
                put("departure_time", bus.departureTime)
                put("status", bus.status)
                put("image_url", bus.imageUrl)
            }.toString()

            val res = makeRequest("buses", "POST", json)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to save bus"))
            }
        }
    }

    fun listenToBuses(
        onUpdate: (List<Bus>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("buses?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<Bus>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                Bus(
                                    busId = obj.safeString("bus_id"),
                                    busNumber = obj.safeString("bus_number"),
                                    capacity = obj.optInt("capacity", 50),
                                    status = obj.safeString("status", "Active"),
                                    plateNumber = obj.safeString("plate_number"),
                                    busType = obj.safeString("bus_type", "Air-conditioned"),
                                    assignedDriver = obj.safeString("assigned_driver", "Unassigned"),
                                    operator = obj.safeString("operator"),
                                    route = obj.safeString("route"),
                                    busStop = obj.safeString("bus_stop"),
                                    departureTime = obj.safeString("departure_time", "08:00"),
                                    imageUrl = obj.safeString("image_url")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteBus(
        busId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("buses?bus_id=eq.$busId", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null || res == "") onSuccess() else onFailure(Exception("Failed to delete bus"))
            }
        }
    }

    // --- DRIVERS ---
    fun saveDriverDetails(
        driverId: String,
        driverName: String,
        contactNumber: String,
        licenseNumber: String,
        licenseExpiry: String,
        assignedBus: String,
        employmentStatus: String,
        email: String = "",
        password: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val existingRes = makeRequest("drivers?driver_id=eq.$driverId", "GET")
            var finalEmail = email
            var finalPass = password
            if (existingRes != null) {
                try {
                    val arr = JSONArray(existingRes)
                    if (arr.length() > 0) {
                        val obj = arr.getJSONObject(0)
                        if (finalEmail.isBlank()) {
                            finalEmail = obj.safeString("email")
                        }
                        if (finalPass.isBlank()) {
                            finalPass = obj.safeString("password")
                        }
                    }
                } catch (_: Exception) {}
            }

            val json = JSONObject().apply {
                put("driver_id", driverId)
                put("driver_name", driverName)
                put("contact_number", contactNumber)
                put("license_number", licenseNumber)
                put("license_expiry", licenseExpiry)
                put("assigned_bus", assignedBus)
                put("employment_status", employmentStatus)
                put("email", finalEmail)
                put("password", finalPass)
            }.toString()

            try {
                val url = URL("$SUPABASE_URL/drivers")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                    doInput = true
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { it.write(json) }
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    if (finalEmail.isNotBlank() && finalPass.isNotBlank()) {
                        val driverUser = user(driverId, driverName, finalEmail, "", "Driver")
                        saveUser(driverUser)
                    }
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                    Log.e("Supabase", "Save driver error: $err")
                    withContext(Dispatchers.Main) { onFailure(Exception(err)) }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Save driver exception: ${e.localizedMessage}", e)
                withContext(Dispatchers.Main) { onFailure(e) }
            }
        }
    }

    fun listenToDrivers(
        onUpdate: (List<DriverRecord>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("drivers?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<DriverRecord>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                DriverRecord(
                                    driverId = obj.safeString("driver_id"),
                                    driverName = obj.safeString("driver_name"),
                                    contactNumber = obj.safeString("contact_number"),
                                    licenseNumber = obj.safeString("license_number"),
                                    licenseExpiry = obj.safeString("license_expiry"),
                                    assignedBus = obj.safeString("assigned_bus", "Unassigned"),
                                    employmentStatus = obj.safeString("employment_status", "On Duty"),
                                    email = obj.safeString("email"),
                                    password = obj.safeString("password")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteDriver(
        driverId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("drivers?driver_id=eq.$driverId", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete driver"))
            }
        }
    }

    fun authenticateDriver(
        identifier: String,
        passwordInput: String,
        onResult: (driver: DriverRecord?, errorMsg: String?) -> Unit
    ) {
        val cleanId = identifier.trim().lowercase()
        val cleanPass = passwordInput.trim()
        if (cleanId.isBlank()) {
            onResult(null, "Please enter your Driver ID or Email")
            return
        }

        ioScope.launch {
            val res = makeRequest("drivers?select=*", "GET")
            withContext(Dispatchers.Main) {
                try {
                    if (res != null) {
                        val arr = JSONArray(res)
                        var matchedDriver: DriverRecord? = null

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val dId = obj.safeString("driver_id")
                            val dName = obj.safeString("driver_name")
                            val dEmail = obj.safeString("email")
                            val dPass = obj.safeString("password")

                            if (dId.trim().lowercase() == cleanId || dEmail.trim().lowercase() == cleanId || dName.trim().lowercase() == cleanId) {
                                if (dPass.isBlank() || dPass.trim() == cleanPass) {
                                    matchedDriver = DriverRecord(
                                        driverId = dId,
                                        driverName = dName,
                                        contactNumber = obj.safeString("contact_number"),
                                        licenseNumber = obj.safeString("license_number"),
                                        licenseExpiry = obj.safeString("license_expiry"),
                                        assignedBus = obj.safeString("assigned_bus", "Unassigned"),
                                        employmentStatus = obj.safeString("employment_status", "On Duty"),
                                        email = dEmail,
                                        password = dPass
                                    )
                                    break
                                }
                            }
                        }

                        if (matchedDriver != null) {
                            onResult(matchedDriver, null)
                        } else {
                            onResult(null, "Invalid Driver ID / Email or Password")
                        }
                    } else {
                        onResult(null, "Failed to connect to database")
                    }
                } catch (e: Exception) {
                    onResult(null, e.localizedMessage ?: "Authentication error")
                }
            }
        }
    }

    // --- BUS STOPS ---
    fun saveBusStop(
        stop: BusStop,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("stop_id", stop.stopId)
                put("stop_name", stop.stopName)
                put("location", stop.location)
                put("latitude", stop.latitude)
                put("longitude", stop.longitude)
                put("bays_count", stop.baysCount)
                put("active_bays", stop.activeBays)
                put("operating_hours", stop.operatingHours)
                put("status", stop.status)
                put("image_url", stop.imageUrl)
            }.toString()

            val res = makeRequest("bus_stops", "POST", json)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to save bus stop"))
            }
        }
    }

    fun listenToBusStops(
        onUpdate: (List<BusStop>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("bus_stops?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<BusStop>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                BusStop(
                                    stopId = obj.safeString("stop_id"),
                                    stopName = obj.safeString("stop_name"),
                                    location = obj.safeString("location"),
                                    latitude = obj.optDouble("latitude", 14.5995),
                                    longitude = obj.optDouble("longitude", 120.9842),
                                    baysCount = obj.optInt("bays_count", 8),
                                    activeBays = obj.optInt("active_bays", 5),
                                    operatingHours = obj.safeString("operating_hours", "04:00 AM - 11:00 PM"),
                                    status = obj.safeString("status", "Active"),
                                    imageUrl = obj.safeString("image_url")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteBusStop(
        stopId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("bus_stops?stop_id=eq.$stopId", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete bus stop"))
            }
        }
    }

    // --- BUS ROUTES ---
    fun saveBusRoute(
        route: BusRoute,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("route_id", route.routeId)
                put("route_name", route.routeName)
                put("start_point", route.startPoint)
                put("destination", route.destination)
                put("status", route.status)
                put("start_lat", route.startLat)
                put("start_lng", route.startLng)
                put("dest_lat", route.destLat)
                put("dest_lng", route.destLng)
                put("fare", route.fare)
            }.toString()

            val res = makeRequest("bus_routes", "POST", json)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to save route"))
            }
        }
    }

    fun listenToRoutes(
        onUpdate: (List<BusRoute>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("bus_routes?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<BusRoute>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                BusRoute(
                                    routeId = obj.safeString("route_id"),
                                    routeName = obj.safeString("route_name"),
                                    startPoint = obj.safeString("start_point"),
                                    destination = obj.safeString("destination"),
                                    status = obj.safeString("status", "Active"),
                                    startLat = obj.optDouble("start_lat", 0.0),
                                    startLng = obj.optDouble("start_lng", 0.0),
                                    destLat = obj.optDouble("dest_lat", 0.0),
                                    destLng = obj.optDouble("dest_lng", 0.0),
                                    fare = obj.optDouble("fare", 0.0)
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteBusRoute(
        routeId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("bus_routes?route_id=eq.$routeId", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete route"))
            }
        }
    }

    // --- BUS SCHEDULES ---
    fun saveBusSchedule(
        schedule: BusSchedule,
        driverName: String = "",
        scheduleDate: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val resolvedDriver = if (driverName.isNotBlank()) driverName else schedule.bus.assignedDriver
            val json = JSONObject().apply {
                put("schedule_id", schedule.scheduleId)
                put("bus_number", schedule.bus.busNumber)
                put("route_name", schedule.route.routeName)
                put("driver_name", resolvedDriver)
                put("departure_time", schedule.departureTime)
                put("arrival_time", schedule.arrivalTime)
                put("status", schedule.status)
                put("schedule_date", scheduleDate)
            }.toString()

            val res = makeRequest("bus_schedules", "POST", json)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to save schedule"))
            }
        }
    }

    fun listenToSchedules(
        onUpdate: (List<BusSchedule>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val schedRes = makeRequest("bus_schedules?select=*", "GET")
                val busRes = makeRequest("buses?select=*", "GET")
                val routeRes = makeRequest("bus_routes?select=*", "GET")
                if (schedRes != null) {
                    try {
                        val busMap = mutableMapOf<String, Bus>()
                        if (busRes != null) {
                            val busArr = JSONArray(busRes)
                            for (i in 0 until busArr.length()) {
                                val bObj = busArr.getJSONObject(i)
                                val bNum = bObj.safeString("bus_number")
                                val bId = bObj.safeString("bus_id")
                                val b = Bus(
                                    busId = bId,
                                    busNumber = bNum,
                                    capacity = bObj.optInt("capacity", 50),
                                    status = bObj.safeString("status", "Active"),
                                    plateNumber = bObj.safeString("plate_number"),
                                    busType = bObj.safeString("bus_type", "Air-conditioned"),
                                    assignedDriver = bObj.safeString("assigned_driver", "Unassigned"),
                                    imageUrl = bObj.safeString("image_url")
                                )
                                busMap[bNum.trim().lowercase()] = b
                                busMap[bId.trim().lowercase()] = b
                                busMap[bNum] = b
                                busMap[bId] = b
                            }
                        }

                        val routeMap = mutableMapOf<String, BusRoute>()
                        if (routeRes != null) {
                            val routeArr = JSONArray(routeRes)
                            for (i in 0 until routeArr.length()) {
                                val rObj = routeArr.getJSONObject(i)
                                val rName = rObj.safeString("route_name")
                                val rId = rObj.safeString("route_id")
                                val r = BusRoute(
                                    routeId = rId,
                                    routeName = rName,
                                    startPoint = rObj.safeString("start_point"),
                                    destination = rObj.safeString("destination"),
                                    status = rObj.safeString("status", "Active"),
                                    startLat = rObj.optDouble("start_lat", 0.0),
                                    startLng = rObj.optDouble("start_lng", 0.0),
                                    destLat = rObj.optDouble("dest_lat", 0.0),
                                    destLng = rObj.optDouble("dest_lng", 0.0),
                                    fare = rObj.optDouble("fare", 0.0)
                                )
                                routeMap[rName.trim().lowercase()] = r
                                routeMap[rId.trim().lowercase()] = r
                            }
                        }

                        val arr = JSONArray(schedRes)
                        val list = mutableListOf<BusSchedule>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val scheduleId = obj.safeString("schedule_id")
                            val busNumber = obj.safeString("bus_number", "BUS-001")
                            val routeName = obj.safeString("route_name", "City Terminal ➔ Airport Hub")
                            val driverName = obj.safeString("driver_name", "Unassigned")
                            val departureTime = obj.safeString("departure_time", "10:00 AM")
                            val arrivalTime = obj.safeString("arrival_time", "01:30 PM")
                            val status = obj.safeString("status", "Scheduled")
                            val scheduleDate = obj.safeString("schedule_date", "")
                            val rejectionReason = obj.safeString("rejection_reason", "")

                            val startP = if (routeName.contains(" ➔ ")) {
                                routeName.split(" ➔ ")[0].trim()
                            } else "Central Terminal"

                            val destP = if (routeName.contains(" ➔ ")) {
                                routeName.split(" ➔ ")[1].trim()
                            } else "Airport Hub"

                            val cleanBusNum = busNumber.trim().lowercase()
                            val matchedBus = busMap[cleanBusNum] ?: busMap[busNumber] ?: busMap.values.find { it.busNumber.trim().lowercase() == cleanBusNum } ?: Bus(scheduleId, busNumber, 50, status).apply {
                                assignedDriver = driverName
                            }

                            val cleanRouteName = routeName.trim().lowercase()
                            val matchedRoute = routeMap[cleanRouteName] ?: routeMap.values.find { it.routeName.trim().lowercase().contains(cleanRouteName) } ?: BusRoute("R01", routeName, startP, destP)

                            val resolvedDriver = if (driverName.isNotBlank() && driverName != "Unassigned") {
                                driverName
                            } else if (matchedBus.assignedDriver.isNotBlank() && matchedBus.assignedDriver != "Unassigned") {
                                matchedBus.assignedDriver
                            } else {
                                "Unassigned"
                            }
                            matchedBus.assignedDriver = resolvedDriver

                            val scheduleObj = BusSchedule(scheduleId, matchedBus, matchedRoute, departureTime, arrivalTime)
                            scheduleObj.status = status
                            scheduleObj.rejectionReason = rejectionReason
                            list.add(scheduleObj)
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteBusSchedule(
        scheduleId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete schedule"))
            }
        }
    }

    fun updateScheduleStatus(
        scheduleId: String,
        newStatus: String,
        driverName: String = "",
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val jsonObj = JSONObject().apply {
                put("status", newStatus)
                if (driverName.isNotBlank()) {
                    put("driver_name", driverName)
                }
            }.toString()

            val res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "PATCH", jsonObj)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to update status"))
            }
        }
    }

    fun updateScheduleRejection(
        scheduleId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val jsonObj = JSONObject().apply {
                put("status", "Rejected")
                put("driver_name", "Unassigned")
                put("rejection_reason", reason)
            }.toString()

            var res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "PATCH", jsonObj)
            if (res == null) {
                val fallbackJson = JSONObject().apply {
                    put("status", "Rejected")
                    put("driver_name", "Unassigned")
                }.toString()
                res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "PATCH", fallbackJson)
            }

            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to update rejection"))
            }
        }
    }

    fun reassignScheduleDriver(
        scheduleId: String,
        newDriverName: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val jsonObj = JSONObject().apply {
                put("driver_name", newDriverName)
                put("status", "Scheduled")
            }.toString()

            var res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "PATCH", jsonObj)
            if (res == null) {
                val fallbackJson = JSONObject().apply {
                    put("driver_name", newDriverName)
                    put("status", "Scheduled")
                }.toString()
                res = makeRequest("bus_schedules?schedule_id=eq.$scheduleId", "PATCH", fallbackJson)
            }

            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to reassign driver"))
            }
        }
    }

    fun authenticateUser(
        emailInput: String,
        passwordInput: String,
        onResult: (user: user?, errorMsg: String?) -> Unit
    ) {
        val cleanEmail = emailInput.trim().lowercase()
        val cleanPass = passwordInput.trim()
        if (cleanEmail.isBlank()) {
            onResult(null, "Please enter your email")
            return
        }

        ioScope.launch {
            val res = makeRequest("users?select=*", "GET")
            withContext(Dispatchers.Main) {
                try {
                    if (res != null) {
                        val arr = JSONArray(res)
                        var foundUser: user? = null

                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val uEmail = obj.safeString("email")
                            val uPass = obj.safeString("password")

                            if (uEmail.trim().lowercase() == cleanEmail) {
                                if (uPass.isBlank() || uPass == cleanPass) {
                                    foundUser = user(
                                        userId = obj.safeString("user_id"),
                                        fullname = obj.safeString("fullname", "Transit User"),
                                        email = uEmail,
                                        password = uPass,
                                        role = obj.safeString("role", "Passenger")
                                    )
                                    break
                                }
                            }
                        }

                        if (foundUser != null) {
                            onResult(foundUser, null)
                        } else {
                            onResult(null, "Invalid email or password / Account not found")
                        }
                    } else {
                        onResult(null, "Failed to connect to database")
                    }
                } catch (e: Exception) {
                    onResult(null, e.localizedMessage ?: "Authentication failed")
                }
            }
        }
    }

    // --- CONDUCTORS ---
    fun saveConductor(
        conductor: ConductorRecord,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("id", conductor.id)
                put("full_name", conductor.fullName)
                put("employee_id", conductor.employeeId)
                put("phone_number", conductor.phoneNumber)
                put("email", conductor.email)
                put("username", conductor.username)
                put("role", conductor.role)
                if (conductor.assignedBusId.isNotBlank()) {
                    put("assigned_bus_id", conductor.assignedBusId)
                }
                put("status", conductor.status)
                put("password", conductor.password)
            }.toString()

            try {
                val url = URL("$SUPABASE_URL/conductors")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "resolution=merge-duplicates,return=representation")
                    doInput = true
                    doOutput = true
                }

                OutputStreamWriter(connection.outputStream).use { it.write(json) }
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    if (conductor.email.isNotBlank()) {
                        val conductorUser = user(conductor.id, conductor.fullName, conductor.email, conductor.password, "Conductor")
                        saveUser(conductorUser)
                    }
                    withContext(Dispatchers.Main) { onSuccess() }
                } else {
                    val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                    Log.e("Supabase", "Save conductor error: $err")
                    withContext(Dispatchers.Main) { onFailure(Exception(err)) }
                }
            } catch (e: Exception) {
                Log.e("Supabase", "Save conductor exception: ${e.localizedMessage}", e)
                withContext(Dispatchers.Main) { onFailure(e) }
            }
        }
    }

    fun listenToConductors(
        onUpdate: (List<ConductorRecord>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("conductors?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<ConductorRecord>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                ConductorRecord(
                                    id = obj.safeString("id"),
                                    fullName = obj.safeString("full_name"),
                                    employeeId = obj.safeString("employee_id"),
                                    phoneNumber = obj.safeString("phone_number"),
                                    email = obj.safeString("email"),
                                    username = obj.safeString("username"),
                                    role = obj.safeString("role", "Conductor"),
                                    assignedBusId = obj.safeString("assigned_bus_id"),
                                    status = obj.safeString("status", "On Duty"),
                                    password = obj.safeString("password", "password123")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun deleteConductor(
        id: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("conductors?id=eq.$id", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete conductor"))
            }
        }
    }

    // --- BOOKINGS & SEAT MANAGEMENT ---
    fun saveBooking(
        booking: BookingRecord,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val json = JSONObject().apply {
                put("booking_id", booking.bookingId)
                put("bus_id", booking.busId)
                put("route_name", booking.routeName)
                put("seats", booking.seats)
                put("total_price", booking.totalPrice)
                put("passenger_email", booking.passengerEmail)
                put("status", booking.status)
                put("payment_method", booking.paymentMethod)
                put("receipt_url", booking.receiptUrl)
                put("payment_status", booking.paymentStatus)
            }.toString()

            val res = makeRequest("bookings", "POST", json)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to save booking"))
            }
        }
    }

    fun deleteBookingForSeat(
        busId: String,
        seatNum: Int,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val res = makeRequest("bookings?bus_id=eq.$busId&seats=like.%25$seatNum%25", "DELETE")
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to delete booking"))
            }
        }
    }

    fun listenToBookings(
        onUpdate: (List<BookingRecord>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            while (isActive) {
                val res = makeRequest("bookings?select=*", "GET")
                if (res != null) {
                    try {
                        val arr = JSONArray(res)
                        val list = mutableListOf<BookingRecord>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                BookingRecord(
                                    bookingId = obj.safeString("booking_id"),
                                    busId = obj.safeString("bus_id"),
                                    routeName = obj.safeString("route_name"),
                                    seats = obj.safeString("seats"),
                                    totalPrice = obj.optDouble("total_price", 0.0),
                                    passengerEmail = obj.safeString("passenger_email"),
                                    status = obj.safeString("status", "Confirmed"),
                                    paymentMethod = obj.safeString("payment_method", "Cash"),
                                    receiptUrl = obj.safeString("receipt_url", ""),
                                    paymentStatus = obj.safeString("payment_status", "Pending")
                                )
                            )
                        }
                        withContext(Dispatchers.Main) { onUpdate(list) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { onError(e) }
                    }
                }
                delay(5000)
            }
        }
    }

    fun updateBookingPaymentStatus(
        bookingId: String,
        newPaymentStatus: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        ioScope.launch {
            val jsonObj = JSONObject().apply {
                put("payment_status", newPaymentStatus)
            }.toString()

            val res = makeRequest("bookings?booking_id=eq.$bookingId", "PATCH", jsonObj)
            withContext(Dispatchers.Main) {
                if (res != null) onSuccess() else onFailure(Exception("Failed to update payment status"))
            }
        }
    }

    fun showAdminNotification(context: android.content.Context, title: String, message: String) {
        val channelId = "transitgo_admin_alerts"
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "TransitGo Admin Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for driver rejections and dispatch alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify((0..100000).random(), builder.build())
        } catch (_: SecurityException) {}
    }

    fun uploadBusImageToSupabaseStorage(
        context: android.content.Context,
        imageUri: android.net.Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        ioScope.launch {
            try {
                val fileName = "bus_${System.currentTimeMillis()}.jpg"
                val url = URL("https://ulhylotzjyvevtiufrmb.supabase.co/storage/v1/object/bus_image/$fileName")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("apikey", SUPABASE_KEY)
                    setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
                    setRequestProperty("Content-Type", "image/jpeg")
                    doInput = true
                    doOutput = true
                }

                context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    connection.outputStream.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val publicUrl = "https://ulhylotzjyvevtiufrmb.supabase.co/storage/v1/object/public/bus_image/$fileName"
                    withContext(Dispatchers.Main) { onSuccess(publicUrl) }
                } else {
                    // Fallback to Base64 string if 401/403 unauthorized occurs due to bucket RLS policies
                    val b64 = convertUriToBase64(context, imageUri)
                    if (b64.isNotBlank()) {
                        withContext(Dispatchers.Main) { onSuccess(b64) }
                    } else {
                        val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Error $responseCode"
                        withContext(Dispatchers.Main) { onFailure(Exception(err)) }
                    }
                }
            } catch (e: Exception) {
                try {
                    val b64 = convertUriToBase64(context, imageUri)
                    if (b64.isNotBlank()) {
                        withContext(Dispatchers.Main) { onSuccess(b64) }
                        return@launch
                    }
                } catch (_: Exception) {}
                withContext(Dispatchers.Main) { onFailure(e) }
            }
        }
    }

    private fun convertUriToBase64(context: android.content.Context, uri: android.net.Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return ""
            "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}
