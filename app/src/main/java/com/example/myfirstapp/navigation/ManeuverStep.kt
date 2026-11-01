package com.example.myfirstapp.navigation

data class ManeuverStep(
    val stepId: Int,
    val latitude: Double,
    val longitude: Double,
    val instruction: String,
    val maneuverType: String = "turn",
    val modifier: String = "",
    val streetName: String = "",
    val distanceMeters: Double = 0.0,
    val bearing: Float = 0f,
    var isSpokenEarly: Boolean = false,
    var isSpokenImmediate: Boolean = false,
    var isCompleted: Boolean = false
)
