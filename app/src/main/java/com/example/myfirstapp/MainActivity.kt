package com.example.myfirstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
    var currentScreen by remember { mutableStateOf(ScreenState.LOGIN) }
    var loggedInUser by remember { mutableStateOf<user?>(null) }

    // Auto-check if a user session is active (Auto-Login / Remember Me feature)
    LaunchedEffect(Unit) {
        // Session handled via Login / Remember Me
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentScreen) {
            ScreenState.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        loggedInUser = user
                        DatabaseManager.saveUser(user)
                        currentScreen = ScreenState.HOME
                    },
                    onNavigateToSignUp = {
                        currentScreen = ScreenState.SIGN_UP
                    }
                )
            }
            ScreenState.SIGN_UP -> {
                SignUpScreen(
                    onSignUpSuccess = { user ->
                        loggedInUser = user
                        DatabaseManager.saveUser(user)
                        currentScreen = ScreenState.HOME
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
                                onLogout = {
                                    loggedInUser = null
                                    currentScreen = ScreenState.LOGIN
                                }
                            )
                        }
                        "driver" -> {
                            com.example.myfirstapp.driver.DriverScheduleScreen(
                                user = user,
                                onLogout = {
                                    loggedInUser = null
                                    currentScreen = ScreenState.LOGIN
                                }
                            )
                        }
                        "conductor" -> {
                            com.example.myfirstapp.conductor.ConductorDashboardScreen(
                                user = user,
                                onLogout = {
                                    loggedInUser = null
                                    currentScreen = ScreenState.LOGIN
                                }
                            )
                        }
                        else -> {
                            HomeScreen(
                                user = user,
                                onLogout = {
                                    loggedInUser = null
                                    currentScreen = ScreenState.LOGIN
                                }
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
