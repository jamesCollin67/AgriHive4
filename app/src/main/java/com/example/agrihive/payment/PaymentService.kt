package com.example.agrihive.payment

import android.content.Context
import android.content.Intent

/**
 * Payment result callback interface
 */
interface PaymentCallback {
    fun onPaymentSuccess(transactionId: String)
    fun onPaymentFailure(error: String)
    fun onPaymentCancelled()
}

/**
 * Payment request data class
 */
data class PaymentRequest(
    val amount: Double,
    val currency: String = "PHP",
    val description: String,
    val orderId: String
)

/**
 * Base payment service interface
 */
interface PaymentService {
    /**
     * Check if the payment app is installed on the device
     */
    fun isAppInstalled(context: Context): Boolean

    /**
     * Get the payment app package name
     */
    fun getAppPackageName(): String

    /**
     * Initiate payment using the payment app
     * @param context Android context
     * @param request Payment request details
     * @param callback Payment result callback
     */
    fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback)

    /**
     * Handle the return intent from payment app
     */
    fun handleReturnIntent(data: Intent?, callback: PaymentCallback)

    /**
     * Get deep link intent for the payment app
     */
    fun getPaymentIntent(context: Context, request: PaymentRequest): Intent?
}
