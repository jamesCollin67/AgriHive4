package com.example.agrihive.hivestreams

data class WeightAnalyticsData(
    val currentWeight: Double = 0.0,
    val trendStatus: String = "Stable",
    val harvestStatus: String = "Wait",
    val totalGain: Double = 0.0,
    val avgDailyGain: Double = 0.0,
    val peakWeight: Double = 0.0,
    val weightHistory: List<WeightRecord> = emptyList()
)

data class WeightRecord(
    val timestamp: Long = 0L,
    val weight: Double = 0.0
)
