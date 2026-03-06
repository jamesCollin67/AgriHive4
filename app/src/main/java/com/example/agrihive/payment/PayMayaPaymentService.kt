package com.example.agrihive.payment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * PayMaya Payment Service Implementation
 * 
 * PayMaya provides payment integration through:
 * 1. PayMaya App Deep Link - For app-to-app payments
 * 2. PayMaya Checkout API - For web-based payments
 * 3. PayMaya QR API - For QR code payments
 * 
 * This implementation uses the PayMaya deep link protocol.
 */
class PayMayaPaymentService : PaymentService {

    companion object {
        // PayMaya app package name
        const val PAYMAYA_PACKAGE = "com.paymaya.ui.android"
        
        // PayMaya Deep Link scheme
        const val PAYMAYA_SCHEME = "paymaya"
        const val PAYMAYA_HOST = "pay"
        
        // PayMaya API Base URL (for server-side integration)
        const val PAYMAYA_API_BASE = "https://api.paymaya.com"
        
        // PayMaya Checkout API endpoints
        const val PAYMAYA_CHECKOUT = "/checkout/v1/checkouts"
        const val PAYMAYA_QR = "/payments/v1/qr"
        
        // For demo: Replace with actual credentials from PayMaya
        const val PUBLIC_API_KEY = "YOUR_PAYMAYA_PUBLIC_KEY"
        const val SECRET_API_KEY = "YOUR_PAYMAYA_SECRET_KEY"
    }

    override fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PAYMAYA_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAppPackageName(): String = PAYMAYA_PACKAGE

    override fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        if (!isAppInstalled(context)) {
            callback.onPaymentFailure("PayMaya app is not installed. Please install PayMaya from the Play Store.")
            return
        }

        try {
            val paymentUri = buildPaymentUri(request)
            val intent = context.packageManager.getLaunchIntentForPackage(PAYMAYA_PACKAGE)
            
            if (intent != null) {
                intent.data = paymentUri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback.onPaymentSuccess("Payment initiated - PayMaya app opened")
            } else {
                callback.onPaymentFailure("Unable to launch PayMaya app")
            }
        } catch (e: Exception) {
            callback.onPaymentFailure("Failed to initiate PayMaya payment: ${e.message}")
        }
    }

    override fun getPaymentIntent(context: Context, request: PaymentRequest): Intent? {
        return try {
            val paymentUri = buildPaymentUri(request)
            val intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage(PAYMAYA_PACKAGE)
            intent
        } catch (e: Exception) {
            null
        }
    }

    override fun handleReturnIntent(data: Intent?, callback: PaymentCallback) {
        data?.let { intent ->
            val resultCode = intent.getStringExtra("result")
            val transactionId = intent.getStringExtra("transactionId")
            
            when (resultCode) {
                "success" -> {
                    callback.onPaymentSuccess(transactionId ?: "PayMaya transaction completed")
                }
                "cancelled" -> {
                    callback.onPaymentCancelled()
                }
                else -> {
                    callback.onPaymentFailure(intent.getStringExtra("message") ?: "Payment failed")
                }
            }
        } ?: run {
            callback.onPaymentCancelled()
        }
    }

    /**
     * Build the PayMaya payment URI for deep linking
     * 
     * PayMaya URI format:
     * paymaya://pay?amount=XXX&merchantname=XXX&merchantref=XXX
     */
    private fun buildPaymentUri(request: PaymentRequest): Uri {
        val builder = Uri.Builder()
            .scheme(PAYMAYA_SCHEME)
            .authority(PAYMAYA_HOST)
            .appendQueryParameter("amount", String.format("%.2f", request.amount))
            .appendQueryParameter("merchantname", "AgriHive")
            .appendQueryParameter("merchantref", request.orderId)
            .appendQueryParameter("description", request.description)
        
        return builder.build()
    }

    /**
     * Create PayMaya checkout session via API
     * This requires server-side implementation with proper API keys
     */
    suspend fun createCheckout(request: PaymentRequest): PayMayaCheckoutResponse? {
        // This would be implemented with actual API call
        /*
        val api = PayMayaApiService.create()
        val response = api.createCheckout(
            amount = request.amount,
            description = request.description,
            requestReference = request.orderId
        )
        return response
        */
        return null
    }

    /**
     * Create PayMaya QR code via API
     */
    suspend fun createQRPayment(request: PaymentRequest): PayMayaQRResponse? {
        // This would be implemented with actual API call
        /*
        val api = PayMayaApiService.create()
        val response = api.createQR(
            amount = request.amount,
            requestReference = request.orderId
        )
        return response
        */
        return null
    }

    /**
     * Check payment status via PayMaya API
     */
    suspend fun checkPaymentStatus(checkoutId: String): PayMayaPaymentStatus? {
        // This would be implemented with actual API call
        return null
    }
}

/**
 * PayMaya API Response models
 */
data class PayMayaCheckoutResponse(
    val checkoutId: String,
    val redirectUrl: String,
    val expiresAt: String
)

data class PayMayaQRResponse(
    val qrCode: String,
    val requestReference: String,
    val expiresAt: String
)

data class PayMayaPaymentStatus(
    val checkoutId: String,
    val status: String,
    val amount: Double,
    val paymentToken: String?
)
