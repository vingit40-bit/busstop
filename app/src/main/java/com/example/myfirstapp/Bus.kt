package com.example.myfirstapp

data class Bus(
    val busId: String,
    var busNumber: String,
    var capacity: Int = 50,
    var status: String = "Active",
    var plateNumber: String = "",
    var busType: String = "Air-conditioned",
    var assignedDriver: String = "Unassigned",
    var operator: String = "",
    var route: String = "",
    var busStop: String = "",
    var departureTime: String = "08:00",
    var imageUrl: String = ""
)
