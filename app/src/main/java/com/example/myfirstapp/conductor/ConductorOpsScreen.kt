package com.example.myfirstapp.conductor

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConductorOpsScreen() {
    val context = LocalContext.current
    var tripReadiness by remember { mutableStateOf("Ready for Departure") }
    var luggageStatus by remember { mutableStateOf("Loaded & Secured") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Trip Communication & Readiness", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Driver & Conductor Coordination", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ConductorGreen)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Trip Status: $tripReadiness", fontSize = 14.sp)
                Text("Luggage Status: $luggageStatus", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        tripReadiness = "Ready for Departure"
                        Toast.makeText(context, "Notified driver: Ready for Departure!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ConductorGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Signal Driver: Ready for Departure")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        luggageStatus = "All Luggage Secured"
                        Toast.makeText(context, "Luggage compartment secured.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Luggage Secured")
                }
            }
        }
    }
}
