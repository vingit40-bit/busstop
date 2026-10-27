package com.example.myfirstapp.conductor

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonRemove
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

@Composable
fun ConductorHeadcountScreen() {
    val context = LocalContext.current
    var schedules by remember { mutableStateOf<List<BusSchedule>>(emptyList()) }
    var selectedSchedule by remember { mutableStateOf<BusSchedule?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val maxCapacity = 50
    var occupiedSeats by remember { mutableStateOf(mutableSetOf(1, 2, 3, 4, 5, 12, 14, 18)) }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToSchedules(
            onUpdate = { list ->
                schedules = list.filter { s ->
                    s.status.equals("Accepted", ignoreCase = true) ||
                    s.status.equals("In Transit", ignoreCase = true) ||
                    s.status.equals("Scheduled", ignoreCase = true)
                }
                if (selectedSchedule == null && schedules.isNotEmpty()) {
                    selectedSchedule = schedules[0]
                }
                isLoading = false
            },
            onError = { _ -> isLoading = false }
        )
    }

    val occupiedCount = occupiedSeats.size
    val availableCount = maxCapacity - occupiedCount

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Conductor Seat & Headcount Management", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ConductorGreen)
            }
        } else if (schedules.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No active bus trips assigned.", color = Color.Gray)
                }
            }
        } else {
            val currentSched = selectedSchedule ?: schedules[0]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Managed Bus: ${currentSched.bus.busNumber} (${currentSched.bus.busType})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ConductorGreen)
                    Text("Route: ${currentSched.route.routeName}", fontSize = 13.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Capacity", fontSize = 12.sp, color = Color.Gray)
                            Text("$maxCapacity Seats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Column {
                            Text("Occupied", fontSize = 12.sp, color = Color.Gray)
                            Text("$occupiedCount Seats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Column {
                            Text("Available", fontSize = 12.sp, color = Color.Gray)
                            Text("$availableCount Seats", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { occupiedCount.toFloat() / maxCapacity.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = Color(0xFFC62828),
                        trackColor = Color(0xFFC8E6C9)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val nextAvailable = (1..maxCapacity).firstOrNull { !occupiedSeats.contains(it) }
                                if (nextAvailable != null) {
                                    occupiedSeats = (occupiedSeats + nextAvailable).toMutableSet()
                                    Toast.makeText(context, "Passenger Boarded: Seat #$nextAvailable", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Bus is fully booked!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Board Passenger")
                        }

                        OutlinedButton(
                            onClick = {
                                if (occupiedSeats.isNotEmpty()) {
                                    val lastOccupied = occupiedSeats.maxOrNull()
                                    if (lastOccupied != null) {
                                        occupiedSeats = (occupiedSeats - lastOccupied).toMutableSet()
                                        Toast.makeText(context, "Passenger Alighted: Seat #$lastOccupied", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "No passengers onboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Alight Passenger")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Live Bus Seat Availability Grid (Tap seat to toggle)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items((1..maxCapacity).toList()) { seatNum ->
                            val isOccupied = occupiedSeats.contains(seatNum)
                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clickable {
                                        if (isOccupied) {
                                            occupiedSeats = (occupiedSeats - seatNum).toMutableSet()
                                            Toast.makeText(context, "Seat #$seatNum marked Available", Toast.LENGTH_SHORT).show()
                                        } else {
                                            occupiedSeats = (occupiedSeats + seatNum).toMutableSet()
                                            Toast.makeText(context, "Seat #$seatNum marked Occupied", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOccupied) Color(0xFFC62828) else Color(0xFFE8F5E9),
                                border = BorderStroke(1.dp, if (isOccupied) Color(0xFFB71C1C) else ConductorGreen)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$seatNum",
                                        color = if (isOccupied) Color.White else ConductorGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
