package com.example.myfirstapp.conductor

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConductorTicketingScreen() {
    val context = LocalContext.current
    var passengerType by remember { mutableStateOf("Regular") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var customFare by remember { mutableStateOf("50") }
    var ticketsIssuedCount by remember { mutableStateOf(148) }
    var totalRevenue by remember { mutableStateOf(7400.0) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Fare Collection & Ticketing", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Issue Passenger Ticket", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ConductorGreen)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customFare,
                    onValueChange = { customFare = it },
                    label = { Text("Fare Amount (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Passenger Type:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Regular", "Student", "Senior").forEach { type ->
                        FilterChip(
                            selected = passengerType == type,
                            onClick = {
                                passengerType = type
                                customFare = when(type) {
                                    "Regular" -> "50"
                                    "Student" -> "40"
                                    else -> "35"
                                }
                            },
                            label = { Text(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Payment Method:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Cash", "GCash / QR", "Card").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val amt = customFare.toDoubleOrNull() ?: 50.0
                        ticketsIssuedCount++
                        totalRevenue += amt
                        Toast.makeText(context, "Ticket Issued! Fare: ₱$amt ($paymentMethod)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Issue & Print Ticket", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Trip Collection Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ConductorGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Tickets Issued: $ticketsIssuedCount", fontSize = 14.sp)
                Text("Total Revenue Collected: ₱%.2f".format(totalRevenue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ConductorGreen)
            }
        }
    }
}
