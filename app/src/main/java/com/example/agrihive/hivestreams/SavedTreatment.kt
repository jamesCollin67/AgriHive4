package com.example.agrihive.hivestreams

data class SavedTreatment(
    val id: String = "",
    val diseaseName: String = "",
    val hiveName: String = "",
    val timestamp: Long = 0L,
    val description: String = "",
    val healthScore: Int = 0,
    val imageUrl: String = ""
)
