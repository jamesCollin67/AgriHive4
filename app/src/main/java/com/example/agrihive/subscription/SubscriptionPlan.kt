package com.example.agrihive.subscription

/**
 * Represents a subscription plan for sensor monitoring
 */
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String,
    val apiaryTier: ApiaryTier,
    val billingType: BillingType,
    val price: Double,
    val currency: String = "PHP"
) {
    fun getFormattedPrice(): String {
        return "P${String.format("%.2f", price)}"
    }
}

enum class ApiaryTier(val displayName: String, val minApiaries: Int, val maxApiaries: Int?) {
    TIER_1_2("1-2 Apiaries", 1, 2),
    TIER_3_5("3-5 Apiaries", 3, 5),
    TIER_5_PLUS("5+ Apiaries", 5, null)
}

enum class BillingType(val displayName: String) {
    MONTHLY("Monthly payment"),
    QUARTERLY("3 months")
}

enum class PaymentMethod(val displayName: String) {
    GCASH("GCash"),
    PAYMAYA("PayMaya"),
    BDO("BDO Online Banking"),
    PAYPAL("PayPal")
}
