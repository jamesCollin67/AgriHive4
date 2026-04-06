package com.example.agrihive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val description: String,
    val timestamp: Long,
    val imageUri: String? = null
)
