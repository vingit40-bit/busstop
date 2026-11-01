package com.example.myfirstapp.passenger

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val BusSchedule.resolvedDate: String
    get() = if (bus.operator.isNotBlank()) bus.operator else scheduleDate

fun calculateTripDurationFormatted(dep: String, arr: String): String {
    return try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val d1 = sdf.parse(dep.trim())
        val d2 = sdf.parse(arr.trim())
        if (d1 != null && d2 != null) {
            var diffMs = d2.time - d1.time
            if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000L
            val minutes = (diffMs / (1000 * 60)).toInt()
            val hours = minutes / 60
            val mins = minutes % 60
            if (hours > 0) "Duration ~ ${hours}h ${mins}m" else "Duration ~ ${mins} min"
        } else "Duration ~ 120 min"
    } catch (_: Exception) {
        "Duration ~ 120 min"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectBusScreen(
    origin: String,
    destination: String,
    journeyDate: String = "",
    onBack: () -> Unit = {},
    onBusSelected: (BusSchedule) -> Unit = {}
) {
    val context = LocalContext.current
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var registeredBuses by remember { mutableStateOf<List<Bus>>(emptyList()) }
    var allBookings by remember { mutableStateOf<List<BookingRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }
    val todayFullDateStr = remember { todaySdf.format(Date()) }

    var selectedFullDate by remember { mutableStateOf(journeyDate.ifBlank { todayFullDateStr }) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    // Dynamic Real Calendar Ribbon Generator (Same dynamic calendar logic as ScheduleManagementScreen)
    val dynamicCalendarDays = remember(selectedFullDate) {
        val sdfDayName = SimpleDateFormat("EEE", Locale.ENGLISH)
        val sdfDayNum = SimpleDateFormat("dd", Locale.ENGLISH)
        val sdfFull = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

        val cal = Calendar.getInstance()
        try {
            cal.time = sdfFull.parse(selectedFullDate) ?: Date()
        } catch (_: Exception) {}

        cal.add(Calendar.DAY_OF_YEAR, -3)
        val list = mutableListOf<Triple<String, String, String>>()
        for (i in 0 until 7) {
            val dName = sdfDayName.format(cal.time)
            val dNum = sdfDayNum.format(cal.time)
            val fDate = sdfFull.format(cal.time)
            list.add(Triple(dName, dNum, fDate))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Friendly date header e.g. "Tuesday, March 31, 2026"
    val formattedDisplayDate = remember(selectedFullDate) {
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(selectedFullDate)
            if (parsed != null) SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.ENGLISH).format(parsed) else selectedFullDate
        } catch (_: Exception) {
            selectedFullDate
        }
    }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list
                isLoading = false
            },
            onError = { _ -> isLoading = false }
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

    // Filter Saved Schedules for Selected Route & Date (Exact matching logic as ScheduleManagementScreen)
    val filteredSchedules = remember(schedules, origin, destination, selectedFullDate) {
        val validSchedules = schedules.filter { sched ->
            !sched.status.equals("Rejected", ignoreCase = true) &&
            !sched.status.equals("Cancelled", ignoreCase = true)
        }

        val routeMatching = if (destination.isBlank()) validSchedules else {
            val matching = validSchedules.filter {
                it.route.destination.contains(destination, ignoreCase = true) ||
                it.route.routeName.contains(destination, ignoreCase = true) ||
                it.route.startPoint.contains(origin, ignoreCase = true)
            }
            if (matching.isNotEmpty()) matching else validSchedules
        }

        routeMatching.filter { sched ->
            val sDate = sched.resolvedDate.trim()
            sDate.isBlank() || sDate == selectedFullDate || (selectedFullDate == todayFullDateStr && sDate.isBlank())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Bus", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                .padding(16.dp)
        ) {
            // Origin -> Destination Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = TransitGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$origin ➔ $destination",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TransitGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Header Bar with "Pick Date" button (Matching ScheduleManagementScreen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Selected Date:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Text(formattedDisplayDate, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TransitGreen)
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, TransitGreen),
                    modifier = Modifier.clickable { showDatePickerModal = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = "Pick Date", tint = TransitGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TransitGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Calendar Ribbon Selector
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dynamicCalendarDays) { triple ->
                    val dayName = triple.first
                    val dayNum = triple.second
                    val fullDateStr = triple.third
                    val isSelected = selectedFullDate == fullDateStr

                    Card(
                        modifier = Modifier
                            .width(54.dp)
                            .height(68.dp)
                            .clickable { selectedFullDate = fullDateStr },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) TransitGreen else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayNum,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Available Schedules for Selected Route",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TransitGreen)
                }
            } else if (filteredSchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = "Empty", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Buses Available for $selectedFullDate", fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Please select another date on the calendar above.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredSchedules) { schedule ->
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
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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

    // Material 3 Date Picker Dialog Modal (Same as ScheduleManagementScreen)
    if (showDatePickerModal) {
        val initialMillis = remember(selectedFullDate) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(selectedFullDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }

        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val pickedDate = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(selectedMillis))
                            selectedFullDate = pickedDate
                        }
                        showDatePickerModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TransitGreen)
                ) {
                    Text("Select Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
