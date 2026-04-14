package com.example.agrihive.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles PayMongo API calls.
 * - createPaymentLink  → generic payment link (any card/e-wallet on PayMongo checkout)
 * - createSource       → direct GCash or Maya source (redirects straight to e-wallet)
 */
object PayMongoService {

    private val api by lazy { PayMongoApi.create() }

    // PayMongo requires HTTPS redirect URLs for the Sources API.
    // These redirect back to the app via the deep link intent filter in AndroidManifest.
    const val SUCCESS_URL = "https://agrihive-cd18a.web.app/payment/success"
    const val FAILED_URL  = "https://agrihive-cd18a.web.app/payment/failed"

    // Deep link schemes used by PaymentDetailsActivity to detect return
    const val DEEP_LINK_SUCCESS = "agrihive://paymongo/success"
    const val DEEP_LINK_FAILED  = "agrihive://paymongo/failed"

    sealed class Result {
        data class Success(val checkoutUrl: String, val referenceId: String) : Result()
        data class Error(val message: String) : Result()
    }

    /**
     * Create a PayMongo Payment Link (works for all payment methods on their hosted page).
     */
    suspend fun createPaymentLink(amountPhp: Double, description: String): Result =
        withContext(Dispatchers.IO) {
            try {
                val body = PayMongoLinkRequest(
                    data = PayMongoLinkRequest.LinkData(
                        attributes = PayMongoLinkRequest.Attributes(
                            amount = (amountPhp * 100).toInt(),
                            description = description
                        )
                    )
                )
                val response = api.createLink(body)
                if (response.isSuccessful) {
                    val attrs = response.body()?.data?.attributes
                    if (attrs != null) {
                        Result.Success(attrs.checkout_url, attrs.reference_number)
                    } else {
                        Result.Error("Empty response from PayMongo")
                    }
                } else {
                    Result.Error("PayMongo error ${response.code()}: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "Network error")
            }
        }

    /**
     * Create a PayMongo Source for GCash or Maya.
     * @param type "gcash" or "paymaya"
     */
    suspend fun createSource(
        amountPhp: Double,
        type: String,
        description: String,
        billingName: String = "AgriHive User",
        billingEmail: String = "user@agrihive.com"
    ): Result = withContext(Dispatchers.IO) {
        try {
            val body = PayMongoSourceRequest(
                data = PayMongoSourceRequest.SourceData(
                    attributes = PayMongoSourceRequest.Attributes(
                        amount = (amountPhp * 100).toInt(),
                        type = type,
                        redirect = PayMongoSourceRequest.Redirect(
                            success = SUCCESS_URL,
                            failed = FAILED_URL
                        ),
                        billing = PayMongoSourceRequest.Billing(
                            name = billingName,
                            email = billingEmail
                        )
                    )
                )
            )
            val response = api.createSource(body)
            if (response.isSuccessful) {
                val attrs = response.body()?.data?.attributes
                val id    = response.body()?.data?.id ?: ""
                if (attrs != null) {
                    Result.Success(attrs.redirect.checkout_url, id)
                } else {
                    Result.Error("Empty response from PayMongo")
                }
            } else {
                Result.Error("PayMongo error ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
