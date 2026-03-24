package com.example.agrihive.addapiary

data class Apiary(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val nodeId: String = "",
    val ownerId: String = "",
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val moisture: Double = 0.0,
    val weight: Double = 0.0,
    val isConnected: Boolean = false,
    val alertsCount: Int = 0,
    val lastUpdate: Long = 0L
)
