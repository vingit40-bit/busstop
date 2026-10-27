package com.example.myfirstapp

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

class BusRoute(
    val routeId: String,
    var routeName: String,
    var startPoint: String,
    var destination: String,
    var status: String = "Active",
    var startLat: Double = 0.0,
    var startLng: Double = 0.0,
    var destLat: Double = 0.0,
    var destLng: Double = 0.0,
    var fare: Double = 0.0
) {
    private val busStop = mutableListOf<BusStop>()

    fun addBusStop(stop: BusStop) {
        busStop.add(stop)
    }

    fun calculateEstimatedFare(): Double {
        if (fare > 0.0) return fare
        if (startLat == 0.0 || destLat == 0.0) return 50.0 // Default base fare
        val earthRadius = 6371.0 // km
        val dLat = Math.toRadians(destLat - startLat)
        val dLng = Math.toRadians(destLng - startLng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(destLat)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = earthRadius * c
        
        // Base fare ₱15 + ₱3.00 per km
        val calculated = 15.0 + (distanceKm * 3.0)
        return round(calculated * 100) / 100.0
    }

    fun displayInfo() {
        println("Route ID: $routeId")
        println("Route Name: $routeName")
        println("Start: $startPoint ($startLat, $startLng)")
        println("Destination: $destination ($destLat, $destLng)")
        println("Fare: ₱$fare (Estimated: ₱${calculateEstimatedFare()})")
        println("Status: $status")

        println("Bus Stop:")
        busStop.forEach {
            println("- ${it.stopName}")
        }
    }
}
