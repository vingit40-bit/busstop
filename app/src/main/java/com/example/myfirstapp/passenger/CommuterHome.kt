package com.example.myfirstapp.passenger

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BookingRecord
import com.example.myfirstapp.Bus
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.TransitGreen
import com.example.myfirstapp.isBookingForBus
import com.example.myfirstapp.user
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BusThumbnail(
    imageUrl: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(imageUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val bmp = try {
                when {
                    imageUrl.startsWith("data:image") && imageUrl.contains("base64,") -> {
                        val base64Data = imageUrl.substringAfter("base64,")
                        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }
                    imageUrl.startsWith("/") -> {
                        val file = File(imageUrl)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }
                    imageUrl.startsWith("content://") -> {
                        val uri = android.net.Uri.parse(imageUrl)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val bytes = inputStream?.readBytes()
                        if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
                    }
                    imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                        val url = java.net.URL(imageUrl)
                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        val bytes = connection.inputStream.readBytes()
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                bitmap = bmp
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE8F5EC)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = contentDescription,
                    tint = TransitGreen,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TransitGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterHome(
    user: user,
    onLogout: () -> Unit,
    onSearchClick: () -> Unit = {},
    onBusSelected: (BusSchedule) -> Unit = {}
) {
    val context = LocalContext.current
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var registeredBuses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var allBookings by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list
                isLoading = false
            },
            onError = { e ->
                errorMessage = e.localizedMessage ?: "Failed to load schedules"
                isLoading = false
            }
        )
        DatabaseManager.listenToBuses(
            onUpdate = { buses ->
                registeredBuses = buses
            },
            onError = {}
        )
        DatabaseManager.listenToBookings(
            onUpdate = { bookings ->
                allBookings = bookings
            },
            onError = {}
        )
    }

    val todaySchedules = remember(schedules) {
        schedules.filter { sched ->
            !sched.status.equals("Rejected", ignoreCase = true) && !sched.status.equals("Cancelled", ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hello, ${user.fullname.substringBefore(' ')}!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("Where would you like to go?", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        },
        containerColor = Color(0xFFF5F8F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Destination search, matching the compact TransitGo home layout.
            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSearchClick),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Search your destination", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(10.dp), color = TransitGreen, modifier = Modifier.clickable(onClick = onSearchClick)) {
                        Icon(Icons.Default.Search, contentDescription = "Search buses", tint = Color.White, modifier = Modifier.padding(9.dp).size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            HomeActionCard(
                title = "Search Bus",
                subtitle = "Find a trip and book seats",
                icon = Icons.Default.DirectionsBus,
                modifier = Modifier.fillMaxWidth(),
                onClick = onSearchClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                TextButton(onClick = onSearchClick, contentPadding = PaddingValues(0.dp)) {
                    Text("View all", color = TransitGreen, fontSize = 12.sp)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSearchClick),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    listOf("Search a route", "Explore bus terminals", "Plan your next trip").forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onSearchClick).padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE8F5EC), modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(if (index == 1) Icons.Default.Place else Icons.Default.History, contentDescription = null, tint = TransitGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(label, color = Color(0xFF26332B), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }
                        if (index < 2) HorizontalDivider(color = Color(0xFFF0F2F0))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Buses Scheduled Today",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = todayDateStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TransitGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TransitGreen)
                }
            } else if (errorMessage.isNotEmpty()) {
                Text(text = errorMessage, color = Color.Red, fontSize = 14.sp)
            } else if (todaySchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No bus schedules available today.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    todaySchedules.forEach { schedule ->
                        val fareDisplay = if (schedule.route.fare > 0) schedule.route.fare else schedule.route.calculateEstimatedFare()
                        val tripDurationText = calculateTripDurationFormatted(schedule.departureTime, schedule.arrivalTime)

                        val totalCapacity = if (schedule.bus.capacity > 0) schedule.bus.capacity else 50
                        val busBookedSeats = allBookings
                            .filter { isBookingForBus(it.busId, schedule.bus.busNumber, schedule.bus.busId, schedule.bus.plateNumber) }
                            .flatMap { it.seats.split(",") }
                            .mapNotNull { it.trim().toIntOrNull() }
                            .toSet()
                        val remainingSeats = (totalCapacity - busBookedSeats.size).coerceAtLeast(0)
                        val isFullyBooked = remainingSeats <= 0

                        val busImgUrl = schedule.bus.imageUrl.ifBlank {
                            registeredBuses.find { b -> schedule.bus.busNumber.contains(b.busNumber, ignoreCase = true) || schedule.bus.busNumber.contains(b.busId, ignoreCase = true) }?.imageUrl ?: ""
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        BusThumbnail(
                                            imageUrl = busImgUrl,
                                            contentDescription = "Bus Image",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "${schedule.departureTime} - ${schedule.arrivalTime}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = Color.Black
                                            )
                                            Text(tripDurationText, fontSize = 12.sp, color = Color.Gray)
                                        }
                                    }
                                    Text(
                                        text = "₱%.0f".format(fareDisplay),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = TransitGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF0F2F0))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Route: ${schedule.route.routeName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Driver: ${schedule.bus.assignedDriver}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Bus: ${schedule.bus.busNumber} • ${schedule.bus.busType.ifBlank { "Air-conditioned" }}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "Remaining Seats: $remainingSeats / $totalCapacity",
                                            fontSize = 12.sp,
                                            color = if (!isFullyBooked) TransitGreen else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (isFullyBooked) {
                                                Toast.makeText(context, "Bus is fully booked! No available seats.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                onBusSelected(schedule)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFullyBooked) Color.Gray else TransitGreen
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(if (isFullyBooked) "Full" else "Select", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Select", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
