package com.example.agrihive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apiaries")
data class ApiaryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val nodeId: String,
    val ownerId: String,
    val temperature: Double,
    val humidity: Double,
    val moisture: Double,
    val weight: Double,
    val isConnected: Boolean,
    val alertsCount: Int,
    val lastUpdate: Long
)
