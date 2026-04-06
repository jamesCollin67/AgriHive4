package com.example.agrihive.hivestreams

import com.google.firebase.firestore.DocumentId

data class SavedTreatment(
    @DocumentId val id: String = "",
    val diseaseName: String = "",
    val hiveName: String = "",
    val timestamp: Long = 0L,
    val description: String = "",
    val symptoms: String = "",
    val healthScore: Int = 0,
    val imageUrl: String = "",
    val apiaryId: String = ""
)
