package com.example.myfirstapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstapp.admin.AdminDashboardScreen
import com.example.myfirstapp.admin.LeafletMapViewScreen
import com.example.myfirstapp.passenger.CommuterMainScreen
import com.example.myfirstapp.passenger.PassengerSearchScreen
import com.example.myfirstapp.ui.theme.MyFirstAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val notifiedRejectedIds = mutableSetOf<String>()
        DatabaseManager.listenToSchedules(
            onUpdate = { fetchedSchedules ->
                fetchedSchedules.forEach { s ->
                    if (s.status == "Rejected" && !notifiedRejectedIds.contains(s.scheduleId)) {
                        notifiedRejectedIds.add(s.scheduleId)
                        DatabaseManager.showAdminNotification(
                            appContext,
                            "⚠️ Driver Rejected Schedule",
                            "Bus ${s.bus.busNumber} (${s.route.routeName}) was rejected. Reason: ${s.rejectionReason.ifBlank { "No reason provided" }}"
                        )
                    }
                }
            },
            onError = {}
        )

        setContent {
            MyFirstAppTheme {
                MainApp()
            }
        }
    }
}

enum class ScreenState {
    LOGIN,
    SIGN_UP,
    HOME
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("TransitGoPrefs", Context.MODE_PRIVATE) }

    var currentScreen by remember { mutableStateOf(ScreenState.LOGIN) }
    var loggedInUser by remember { mutableStateOf<user?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when (currentScreen) {
            ScreenState.SIGN_UP -> {
                currentScreen = ScreenState.LOGIN
            }
            ScreenState.LOGIN, ScreenState.HOME -> {
                showExitDialog = true
            }
        }
    }

    if (showExitDialog) {
        SweetAlertDialog(
            type = SweetAlertType.CONFIRM,
            title = "Exit TransitGo?",
            message = "Are you sure you want to exit TransitGo?",
            confirmText = "Yes, Exit",
            cancelText = "Cancel",
            onConfirm = {
                showExitDialog = false
                (context as? ComponentActivity)?.finish()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    fun handleLoginSuccess(user: user) {
        loggedInUser = user
        prefs.edit()
            .putString("active_uid", user.userId)
            .putString("active_name", user.fullname)
            .putString("active_email", user.email)
            .putString("active_role", user.role)
            .apply()
        DatabaseManager.saveUser(user)
        currentScreen = ScreenState.HOME
    }

    fun handleLogout() {
        prefs.edit()
            .remove("active_uid")
            .remove("active_name")
            .remove("active_email")
            .remove("active_role")
            .apply()
        loggedInUser = null
        currentScreen = ScreenState.LOGIN
    }

    // Auto-check if a user session is active (Auto-Login / Session persistence)
    LaunchedEffect(Unit) {
        val savedUid = prefs.getString("active_uid", "") ?: ""
        val savedName = prefs.getString("active_name", "") ?: ""
        val savedEmail = prefs.getString("active_email", "") ?: ""
        val savedRole = prefs.getString("active_role", "") ?: ""

        if (savedUid.isNotBlank() && savedEmail.isNotBlank()) {
            loggedInUser = user(
                userId = savedUid,
                fullname = savedName.ifBlank { "Commuter User" },
                email = savedEmail,
                password = "",
                role = savedRole.ifBlank { "Passenger" }
            )
            currentScreen = ScreenState.HOME
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            ScreenState.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        handleLoginSuccess(user)
                    },
                    onNavigateToSignUp = {
                        currentScreen = ScreenState.SIGN_UP
                    }
                )
            }
            ScreenState.SIGN_UP -> {
                SignUpScreen(
                    onSignUpSuccess = { user ->
                        handleLoginSuccess(user)
                    },
                    onNavigateToLogin = {
                        currentScreen = ScreenState.LOGIN
                    }
                )
            }
            ScreenState.HOME -> {
                val user = loggedInUser
                if (user != null) {
                    when (user.role.trim().lowercase()) {
                        "admin", "staff" -> {
                            AdminDashboardScreen(
                                user = user,
                                onLogout = ::handleLogout
                            )
                        }
                        "driver" -> {
                            com.example.myfirstapp.driver.DriverScheduleScreen(
                                user = user,
                                onLogout = ::handleLogout
                            )
                        }
                        "conductor" -> {
                            com.example.myfirstapp.conductor.ConductorDashboardScreen(
                                user = user,
                                onLogout = ::handleLogout
                            )
                        }
                        else -> {
                            HomeScreen(
                                user = user,
                                onLogout = ::handleLogout
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    user: user,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (user.role == "Driver") {
        com.example.myfirstapp.driver.DriverScheduleScreen(
            user = user,
            onLogout = onLogout
        )
        return
    }

    var isSearchingDestination by remember { mutableStateOf(false) }
    var trackedRouteName by remember { mutableStateOf<String?>(null) }

    if (trackedRouteName != null) {
        LeafletMapViewScreen(
            leafletId = "L001",
            routeName = trackedRouteName ?: "Manila Transit Route",
            onBack = { trackedRouteName = null }
        )
        return
    }

    if (isSearchingDestination) {
        PassengerSearchScreen(
            onBack = { isSearchingDestination = false },
            onDestinationSelected = { origin, destination ->
                isSearchingDestination = false
                trackedRouteName = "$origin ➔ $destination"
            }
        )
        return
    }

    CommuterMainScreen(
        user = user,
        onLogout = onLogout
    )
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
    MyFirstAppTheme {
        MainApp()
    }
}
