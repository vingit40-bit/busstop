package com.example.myfirstapp.passenger

import androidx.compose.runtime.Composable

@Composable
fun LiveTrackingScreen(
    busId: String = "B1203",
    routeName: String = "Manila Transit Route",
    onBack: () -> Unit = {}
) {
    CommuterLiveNavigationScreen(
        busId = busId,
        routeName = routeName,
        onBack = onBack
    )
}
