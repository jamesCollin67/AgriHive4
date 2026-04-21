package com.example.agrihive.hivestreams

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class SavedTreatment(
    @DocumentId val id: String = "",
    val diseaseName: String = "",
    val hiveName: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val description: String = "",
    val symptoms: String = "",
    val healthScore: Int = 0,
    val imageUrl: String = "",
    val apiaryId: String = ""
) {
    val timestampMillis: Long get() = timestamp.toDate().time
}
