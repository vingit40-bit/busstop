package com.example.myfirstapp.conductor

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myfirstapp.user

val ConductorGreen = Color(0xFF1B5E20)
val ConductorBg = Color(0xFFF1F8E9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConductorDashboardScreen(
    user: user,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("Trips") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conductor Portal - ${user.fullname}", color = Color.White, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ConductorGreen)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DirectionsBus, contentDescription = "Trips") },
                    label = { Text("Trips") },
                    selected = selectedTab == "Trips",
                    onClick = { selectedTab = "Trips" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConductorGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Ticketing") },
                    label = { Text("Ticketing") },
                    selected = selectedTab == "Ticketing",
                    onClick = { selectedTab = "Ticketing" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConductorGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Headcount") },
                    label = { Text("Headcount") },
                    selected = selectedTab == "Headcount",
                    onClick = { selectedTab = "Headcount" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConductorGreen)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Commute, contentDescription = "Ops") },
                    label = { Text("Ops") },
                    selected = selectedTab == "Ops",
                    onClick = { selectedTab = "Ops" },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = ConductorGreen)
                )
            }
        },
        containerColor = ConductorBg
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (selectedTab) {
                "Trips" -> ConductorTripsScreen()
                "Ticketing" -> ConductorTicketingScreen()
                "Headcount" -> ConductorHeadcountScreen()
                "Ops" -> ConductorOpsScreen()
            }
        }
    }
}
