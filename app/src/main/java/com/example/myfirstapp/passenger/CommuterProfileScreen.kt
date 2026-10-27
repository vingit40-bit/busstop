package com.example.myfirstapp.passenger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.TransitGreen
import com.example.myfirstapp.user

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommuterProfileScreen(
    user: user,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commuter Profile & Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TransitGreen)
            )
        },
        containerColor = Color(0xFFF5F8F6)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TransitGreen, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(user.fullname, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(user.email, fontSize = 14.sp, color = Color.Gray)
            Text("Account Type: ${user.role}", fontSize = 13.sp, color = TransitGreen, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Preferences & Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TransitGreen)
                    HorizontalDivider()
                    Text("• Favorite Stops & Routes", fontSize = 14.sp, color = Color.DarkGray)
                    Text("• Service Announcements & Advisories", fontSize = 14.sp, color = Color.DarkGray)
                    Text("• Help & Support / Feedback", fontSize = 14.sp, color = Color.DarkGray)
                    Text("• About TransitGo System (v1.0)", fontSize = 14.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
