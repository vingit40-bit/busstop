package com.example.myfirstapp.passenger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.TransitGreen

data class DateItem(val dayName: String, val dateNum: String, var isSelected: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectBusScreen(
    origin: String,
    destination: String,
    journeyDate: String = "2026-10-26",
    onBack: () -> Unit = {},
    onBusSelected: (BusSchedule) -> Unit = {}
) {
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedDateNum by remember { mutableStateOf("23") }

    val datesList = remember {
        listOf(
            DateItem("Mon", "21"),
            DateItem("Tue", "22"),
            DateItem("Wed", "23", true),
            DateItem("Thu", "24"),
            DateItem("Fri", "25")
        )
    }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list
                isLoading = false
            },
            onError = { _ -> isLoading = false }
        )
    }

    val filteredSchedules = remember(schedules, origin, destination) {
        if (destination.isBlank()) schedules
        else {
            val matching = schedules.filter {
                it.route.destination.contains(destination, ignoreCase = true) ||
                it.route.routeName.contains(destination, ignoreCase = true) ||
                it.route.startPoint.contains(origin, ignoreCase = true)
            }
            if (matching.isNotEmpty()) matching else schedules
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

            // Date Selection Strip Bar
            Text("Select Date", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(datesList) { dateItem ->
                    val isSel = dateItem.dateNum == selectedDateNum
                    Card(
                        modifier = Modifier
                            .width(58.dp)
                            .height(64.dp)
                            .clickable { selectedDateNum = dateItem.dateNum },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSel) TransitGreen else Color.White),
                        border = BorderStroke(1.dp, if (isSel) TransitGreen else Color.LightGray)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(dateItem.dayName, fontSize = 11.sp, color = if (isSel) Color.White else Color.Gray)
                            Text(dateItem.dateNum, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Available Schedules for Selected Route", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TransitGreen)
                }
            } else if (filteredSchedules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No buses available for this route.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredSchedules) { schedule ->
                        val fareDisplay = if (schedule.route.fare > 0) schedule.route.fare else schedule.route.calculateEstimatedFare()
                        val remainingSeats = (20..38).random()

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
                                    Column {
                                        Text("${schedule.departureTime} - ${schedule.arrivalTime}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                                        Text("Duration ~ 120 min", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text("₱%.0f".format(fareDisplay), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TransitGreen)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Bus: ${schedule.bus.busNumber} • ${schedule.bus.busType}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        Text("Remaining Seats: $remainingSeats", fontSize = 12.sp, color = TransitGreen, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            onBusSelected(schedule)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TransitGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                    ) {
                                        Text("Select", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
