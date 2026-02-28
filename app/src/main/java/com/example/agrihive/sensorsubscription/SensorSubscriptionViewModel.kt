package com.example.agrihive.sensorsubscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SensorSubscriptionViewModel : ViewModel() {

    // Available subscription plans
    data class PricingTier(
        val tierName: String,
        val monthlyPrice: Double,
        val quarterlyPrice: Double
    )

    private val _pricingTiers = MutableLiveData<List<PricingTier>>()
    val pricingTiers: LiveData<List<PricingTier>> = _pricingTiers

    init {
        loadPricingTiers()
    }

    private fun loadPricingTiers() {
        val tiers = listOf(
            PricingTier(
                tierName = "1-2",
                monthlyPrice = 183.00,
                quarterlyPrice = 550.00
            ),
            PricingTier(
                tierName = "3-5",
                monthlyPrice = 250.00,
                quarterlyPrice = 750.00
            ),
            PricingTier(
                tierName = "5+",
                monthlyPrice = 333.00,
                quarterlyPrice = 999.00
            )
        )
        _pricingTiers.value = tiers
    }

    fun getPlanDescription(tierName: String): String {
        return when (tierName) {
            "1-2" -> "Perfect for small-scale beekeepers with 1-2 apiary locations"
            "3-5" -> "Ideal for medium-sized beekeeping operations with 3-5 apiaries"
            "5+" -> "Best for large-scale commercial beekeepers with 5+ apiaries"
            else -> ""
        }
    }
}
