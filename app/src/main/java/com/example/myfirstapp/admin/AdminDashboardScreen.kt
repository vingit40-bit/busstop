package com.example.myfirstapp.admin

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.DatabaseManager
import com.example.myfirstapp.PaymentSettings
import com.example.myfirstapp.user

val AdminGreen = Color(0xFF116530)
val AdminBg = Color(0xFFF5F8F6)

@Composable
fun AdminDashboardScreen(
    user: user,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf("Dashboard") }
    var activeSubScreen by remember { mutableStateOf<String?>(null) }

    if (selectedTab == "Schedules") {
        ScheduleManagementScreen(user = user, onBack = { selectedTab = "Dashboard" })
        return
    }

    if (selectedTab == "Map") {
        AdminLiveBusTrackingScreen(onBack = { selectedTab = "Dashboard" })
        return
    }

    if (selectedTab == "Settings") {
        AdminPaymentSettingsScreen(onBack = { selectedTab = "Dashboard" })
        return
    }

    if (activeSubScreen != null) {
        when (activeSubScreen) {
            "Bus" -> BusManagementScreen(onBack = { activeSubScreen = null })
            "Driver" -> DriverManagementScreen(onBack = { activeSubScreen = null })
            "Conductor" -> ConductorManagementScreen(onBack = { activeSubScreen = null })
            "Schedule" -> ScheduleManagementScreen(user = user, onBack = { activeSubScreen = null })
            "Route" -> RouteManagementScreen(onBack = { activeSubScreen = null })
            "Tracker" -> AdminLiveBusTrackingScreen(onBack = { activeSubScreen = null })
            "Terminal" -> TerminalManagementScreen(onBack = { activeSubScreen = null })
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$activeSubScreen Management", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { activeSubScreen = null },
                            colors = ButtonDefaults.buttonColors(containerColor = AdminGreen)
                        ) {
                            Text("Back to Admin Portal")
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                listOf("Dashboard", "Schedules", "Map", "Settings").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    "Dashboard" -> Icons.Default.Dashboard
                                    "Schedules" -> Icons.Default.CalendarMonth
                                    "Map" -> Icons.Default.Map
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = tab,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AdminGreen,
                            selectedTextColor = AdminGreen,
                            indicatorColor = Color(0xFFE8F5E9)
                        )
                    )
                }
            }
        },
        containerColor = AdminBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AdminGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Admin Portal",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = user.fullname,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminStatCard(title = "Buses Active", value = "24/30", modifier = Modifier.weight(1f))
                AdminStatCard(title = "Today's Routes", value = "42", modifier = Modifier.weight(1f))
                AdminStatCard(title = "Terminal Bays", value = "5/8", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quick Action",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        icon = Icons.Default.DirectionsBus,
                        title = "Bus\nManagement",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Bus" }
                    )
                    QuickActionTile(
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        title = "Route\nManagement",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Route" }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        icon = Icons.Default.Schedule,
                        title = "Schedule\nManagement",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Schedule" }
                    )
                    QuickActionTile(
                        icon = Icons.Default.Person,
                        title = "Driver\nManagement",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Driver" }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionTile(
                        icon = Icons.Default.Store,
                        title = "Terminal\nHubs",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Terminal" }
                    )
                    QuickActionTile(
                        icon = Icons.Default.Person,
                        title = "Conductor\nManagement",
                        modifier = Modifier.weight(1f),
                        onClick = { activeSubScreen = "Conductor" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Logout Admin Session", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdminGreen)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentSettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var gcashNumber by remember { mutableStateOf("0917-123-4567") }
    var paymayaNumber by remember { mutableStateOf("0918-987-6543") }
    var gcashQrUri by remember { mutableStateOf<Uri?>(null) }
    var paymayaQrUri by remember { mutableStateOf<Uri?>(null) }

    var gcashQrUrl by remember { mutableStateOf("") }
    var paymayaQrUrl by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }

    val gcashPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) gcashQrUri = uri
    }

    val paymayaPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) paymayaQrUri = uri
    }

    LaunchedEffect(Unit) {
        DatabaseManager.listenToPaymentSettings(
            onUpdate = { settings ->
                gcashNumber = settings.gcashNumber
                paymayaNumber = settings.paymayaNumber
                gcashQrUrl = settings.gcashQrUrl
                paymayaQrUrl = settings.paymayaQrUrl
            },
            onError = {}
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Online Payment & QR Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Configure GCash and PayMaya QR codes for payments",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminGreen)
            )
        },
        containerColor = AdminBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // GCash Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("GCash Payment Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AdminGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = gcashNumber,
                        onValueChange = { gcashNumber = it },
                        label = { Text("GCash Mobile / Account Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { gcashPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = AdminGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (gcashQrUri != null) "✓ New GCash QR Selected" else "Upload Official GCash QR Image",
                            color = AdminGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PayMaya Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PayMaya Payment Configuration", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AdminGreen)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = paymayaNumber,
                        onValueChange = { paymayaNumber = it },
                        label = { Text("PayMaya Account / Mobile Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { paymayaPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = AdminGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (paymayaQrUri != null) "✓ New PayMaya QR Selected" else "Upload Official PayMaya QR Image",
                            color = AdminGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    isSaving = true
                    val saveAction = { gUrl: String, pUrl: String ->
                        val settings = PaymentSettings(
                            id = "default_settings",
                            gcashNumber = gcashNumber.trim(),
                            gcashQrUrl = gUrl,
                            paymayaNumber = paymayaNumber.trim(),
                            paymayaQrUrl = pUrl
                        )
                        DatabaseManager.savePaymentSettings(
                            settings,
                            onSuccess = {
                                isSaving = false
                                Toast.makeText(context, "Payment settings & QR codes saved to Supabase!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { err ->
                                isSaving = false
                                Toast.makeText(context, "Error: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (gcashQrUri != null) {
                        DatabaseManager.uploadBusImageToSupabaseStorage(
                            context, gcashQrUri!!,
                            onSuccess = { uploadedGUrl ->
                                if (paymayaQrUri != null) {
                                    DatabaseManager.uploadBusImageToSupabaseStorage(
                                        context, paymayaQrUri!!,
                                        onSuccess = { uploadedPUrl -> saveAction(uploadedGUrl, uploadedPUrl) },
                                        onFailure = { saveAction(uploadedGUrl, paymayaQrUrl) }
                                    )
                                } else saveAction(uploadedGUrl, paymayaQrUrl)
                            },
                            onFailure = { saveAction(gcashQrUrl, paymayaQrUrl) }
                        )
                    } else if (paymayaQrUri != null) {
                        DatabaseManager.uploadBusImageToSupabaseStorage(
                            context, paymayaQrUri!!,
                            onSuccess = { uploadedPUrl -> saveAction(gcashQrUrl, uploadedPUrl) },
                            onFailure = { saveAction(gcashQrUrl, paymayaQrUrl) }
                        )
                    } else {
                        saveAction(gcashQrUrl, paymayaQrUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGreen),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("Save Payment QR", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = AdminGreen, maxLines = 1)
        }
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(88.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = AdminGreen,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 14.sp
            )
        }
    }
}
