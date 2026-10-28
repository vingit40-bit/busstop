package com.example.myfirstapp.passenger

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.myfirstapp.BookingRecord
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.TransitGreen

data class SeatItem(
    val seatNumber: Int,
    var status: String // "Available", "Selected", "Occupied"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectSeatScreen(
    schedule: BusSchedule,
    onBack: () -> Unit = {},
    onBookingConfirmed: (List<Int>, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val unitFare = if (schedule.route.fare > 0) schedule.route.fare else schedule.route.calculateEstimatedFare()

    val maxCapacity = 32
    var occupiedSeatNumbers by remember { mutableStateOf(setOf<Int>()) }

    var paymentMethod by remember { mutableStateOf("Cash") } // "Cash", "GCash", "PayMaya"
    var receiptUriStr by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            receiptUriStr = uri.toString()
            Toast.makeText(context, "Real receipt image attached from gallery!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(schedule.bus.busNumber) {
        DatabaseManager.listenToBookings(
            onUpdate = { bookings ->
                val booked = mutableSetOf<Int>()
                bookings.forEach { b ->
                    if (b.busId == schedule.bus.busNumber || b.busId.isBlank()) {
                        b.seats.split(",").forEach { s ->
                            s.trim().toIntOrNull()?.let { booked.add(it) }
                        }
                    }
                }
                occupiedSeatNumbers = booked
            },
            onError = {}
        )
    }

    var seatsList by remember(occupiedSeatNumbers) {
        mutableStateOf(
            (1..maxCapacity).map { num ->
                SeatItem(num, if (occupiedSeatNumbers.contains(num)) "Occupied" else "Available")
            }
        )
    }

    val selectedSeats = seatsList.filter { it.status == "Selected" }
    val totalPrice = selectedSeats.size * unitFare

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Seat & Fare", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Price:", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
                        Text("₱%.2f".format(totalPrice), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TransitGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Mode of Payment:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "GCash", "PayMaya").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (paymentMethod != "Cash") {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (receiptUriStr.isNotBlank()) Color(0xFF2E7D32) else TransitGreen)
                        ) {
                            Text(if (receiptUriStr.isNotBlank()) "✓ Real Receipt Image Attached" else "Upload Real $paymentMethod Receipt (Gallery)", fontSize = 12.sp, color = if (receiptUriStr.isNotBlank()) Color(0xFF2E7D32) else TransitGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (selectedSeats.isEmpty()) {
                                Toast.makeText(context, "Please select at least one seat", Toast.LENGTH_SHORT).show()
                            } else if (paymentMethod != "Cash" && receiptUriStr.isBlank()) {
                                Toast.makeText(context, "Please upload your real $paymentMethod receipt image from gallery", Toast.LENGTH_SHORT).show()
                            } else {
                                val seatNums = selectedSeats.map { it.seatNumber }
                                val bookingId = "BKG-" + (1000..9999).random()
                                val newBooking = BookingRecord(
                                    bookingId = bookingId,
                                    busId = schedule.bus.busNumber,
                                    routeName = schedule.route.routeName,
                                    seats = seatNums.joinToString(","),
                                    totalPrice = totalPrice,
                                    passengerEmail = "passenger@transitgo.com",
                                    paymentMethod = paymentMethod,
                                    receiptUrl = receiptUriStr,
                                    paymentStatus = if (paymentMethod == "Cash") "Pay Cash to Conductor" else "Pending Receipt Verification"
                                )
                                DatabaseManager.saveBooking(
                                    newBooking,
                                    onSuccess = {
                                        Toast.makeText(context, "Seat(s) booked successfully via $paymentMethod!", Toast.LENGTH_SHORT).show()
                                        onBookingConfirmed(seatNums, totalPrice)
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(context, "Booking saved locally. Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        onBookingConfirmed(seatNums, totalPrice)
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TransitGreen),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Book Selected Seat (${selectedSeats.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFFF5F8F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Bus: ${schedule.bus.busNumber} • Route: ${schedule.route.routeName}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TransitGreen)
            Spacer(modifier = Modifier.height(14.dp))

            // Seat Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp)).border(1.dp, TransitGreen, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Available", fontSize = 12.sp, color = Color.DarkGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(TransitGreen, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Selected", fontSize = 12.sp, color = Color.DarkGray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFFFF9800), RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Occupied", fontSize = 12.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bus Seat Grid
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Driver Side ➔ Front", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(seatsList) { seat ->
                            val isOcc = seat.status == "Occupied"
                            val isSel = seat.status == "Selected"

                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable(enabled = !isOcc) {
                                        seatsList = seatsList.map { item ->
                                            if (item.seatNumber == seat.seatNumber) {
                                                val newStatus = if (item.status == "Selected") "Available" else "Selected"
                                                item.copy(status = newStatus)
                                            } else item
                                        }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isOcc -> Color(0xFFFF9800)
                                    isSel -> TransitGreen
                                    else -> Color(0xFFE8F5E9)
                                },
                                border = BorderStroke(1.dp, if (isSel || isOcc) Color.Transparent else TransitGreen)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${seat.seatNumber}",
                                        color = if (isSel || isOcc) Color.White else TransitGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
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
