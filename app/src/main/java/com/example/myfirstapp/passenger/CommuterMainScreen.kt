package com.example.myfirstapp.passenger

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.myfirstapp.BusSchedule
import com.example.myfirstapp.TransitGreen
import com.example.myfirstapp.user

data class BookingRecord(
    val busId: String,
    val routeName: String,
    val seats: List<Int>,
    val totalPrice: Double,
    val status: String = "Confirmed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterMainScreen(
    user: user,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("Home") }

    var flowStep by remember { mutableStateOf<String?>(null) }

    var selectedOrigin by remember { mutableStateOf("Central Terminal - Manila") }
    var selectedDestination by remember { mutableStateOf("Airport Hub - NAIA") }
    var activeSelectedSchedule by remember { mutableStateOf<BusSchedule?>(null) }

    val bookingsList = remember { mutableStateListOf<BookingRecord>() }

    if (flowStep != null) {
        when (flowStep) {
            "Search" -> {
                PassengerSearchScreen(
                    onBack = { flowStep = null },
                    onDestinationSelected = { orig, dest ->
                        selectedOrigin = orig
                        selectedDestination = dest
                        flowStep = "SelectBus"
                    }
                )
                return
            }
            "SelectBus" -> {
                SelectBusScreen(
                    origin = selectedOrigin,
                    destination = selectedDestination,
                    onBack = { flowStep = "Search" },
                    onBusSelected = { sched ->
                        activeSelectedSchedule = sched
                        flowStep = "SelectSeat"
                    }
                )
                return
            }
            "SelectSeat" -> {
                val sched = activeSelectedSchedule
                if (sched != null) {
                    SelectSeatScreen(
                        schedule = sched,
                        onBack = { flowStep = "SelectBus" },
                        onBookingConfirmed = { seatNums, price ->
                            bookingsList.add(
                                0,
                                BookingRecord(
                                    busId = sched.bus.busNumber,
                                    routeName = sched.route.routeName,
                                    seats = seatNums,
                                    totalPrice = price
                                )
                            )
                            Toast.makeText(context, "Booking Confirmed! Seats: ${seatNums.joinToString()}", Toast.LENGTH_LONG).show()
                            flowStep = "LiveTracking"
                        }
                    )
                    return
                }
            }
            "LiveTracking" -> {
                val sched = activeSelectedSchedule
                LiveTrackingScreen(
                    busId = sched?.bus?.busNumber ?: "B1203",
                    routeName = sched?.route?.routeName ?: "Manila Transit Route",
                    onBack = { flowStep = null; selectedTab = "Bookings" }
                )
                return
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == "Home",
                    onClick = { selectedTab = "Home" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TransitGreen, selectedTextColor = TransitGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Bookings") },
                    label = { Text("Bookings") },
                    selected = selectedTab == "Bookings",
                    onClick = { selectedTab = "Bookings" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TransitGreen, selectedTextColor = TransitGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Tracking") },
                    label = { Text("Tracking") },
                    selected = selectedTab == "Tracking",
                    onClick = { selectedTab = "Tracking" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TransitGreen, selectedTextColor = TransitGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == "Profile",
                    onClick = { selectedTab = "Profile" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TransitGreen, selectedTextColor = TransitGreen)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                "Home" -> CommuterHome(
                    user = user,
                    onLogout = onLogout,
                    onSearchClick = { flowStep = "Search" }
                )
                "Bookings" -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("My Seat Bookings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (bookingsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No active bookings yet", color = Color.Gray)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { flowStep = "Search" },
                                        colors = ButtonDefaults.buttonColors(containerColor = TransitGreen)
                                    ) {
                                        Text("Book a Bus Trip Now")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                items(bookingsList) { b ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Bus: ${b.busId}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TransitGreen)
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF4CAF50)) {
                                                    Text(b.status, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Route: ${b.routeName}", fontSize = 13.sp)
                                            Text("Booked Seats: ${b.seats.joinToString()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Total Paid: ₱%.2f".format(b.totalPrice), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TransitGreen)

                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { flowStep = "LiveTracking" },
                                                colors = ButtonDefaults.buttonColors(containerColor = TransitGreen),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Track Live Bus")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Tracking" -> {
                    LiveTrackingScreen(
                        busId = activeSelectedSchedule?.bus?.busNumber ?: "B1203",
                        routeName = activeSelectedSchedule?.route?.routeName ?: "Manila Transit Route",
                        onBack = { selectedTab = "Home" }
                    )
                }
                "Profile" -> CommuterProfileScreen(user = user, onLogout = onLogout)
            }
        }
    }
}
