package com.example.agrihive.payment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * GCash Payment Service Implementation
 * 
 * GCash provides multiple ways to integrate payments:
 * 1. GCash App Deep Link - For app-to-app payments
 * 2. GCash Partner API - For business integrations (requires partnership)
 * 
 * This implementation uses the GCash deep link protocol for QR-based payments
 * and can be extended to use the Partner API with proper credentials.
 */
class GCashPaymentService : PaymentService {

    companion object {
        // GCash app package name
        const val GCASH_PACKAGE = "com.globe.gcash"
        
        // GCash Deep Link scheme
        const val GCASH_SCHEME = "gcash"
        const val GCASH_HOST = "payment"
        
        // GCash Partner API Base URL (for server-side integration)
        const val GCASH_API_BASE = "https://api.gcash.com"
        
        // GCash Partner API Endpoints
        const val GCASH_CREATE_QR = "/bills/payment/v1/onetime QR"
        const val GCASH_CHECK_STATUS = "/bills/payment/v1/transactions/status"
        
        // For demo: Replace with actual merchant ID from GCash partnership
        const val MERCHANT_ID = "YOUR_GCASH_MERCHANT_ID"
        const val MERCHANT_KEY = "YOUR_GCASH_MERCHANT_KEY"
    }

    override fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(GCASH_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAppPackageName(): String = GCASH_PACKAGE

    override fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        if (!isAppInstalled(context)) {
            callback.onPaymentFailure("GCash app is not installed. Please install GCash from the Play Store.")
            return
        }

        try {
            // Method 1: Using GCash QR Payment Deep Link
            // This opens GCash app with pre-filled payment details
            val paymentUri = buildPaymentUri(request)
            val intent = context.packageManager.getLaunchIntentForPackage(GCASH_PACKAGE)
            
            if (intent != null) {
                // Set the data URI for deep linking
                intent.data = paymentUri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback.onPaymentSuccess("Payment initiated - GCash app opened")
            } else {
                callback.onPaymentFailure("Unable to launch GCash app")
            }
        } catch (e: Exception) {
            callback.onPaymentFailure("Failed to initiate GCash payment: ${e.message}")
        }
    }

    override fun getPaymentIntent(context: Context, request: PaymentRequest): Intent? {
        return try {
            val paymentUri = buildPaymentUri(request)
            val intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage(GCASH_PACKAGE)
            intent
        } catch (e: Exception) {
            null
        }
    }

    override fun handleReturnIntent(data: Intent?, callback: PaymentCallback) {
        // Handle the return intent from GCash app
        // GCash returns results via the intent data
        data?.let { intent ->
            val resultCode = intent.getStringExtra("result")
            val transactionId = intent.getStringExtra("transactionId")
            
            when (resultCode) {
                "success" -> {
                    callback.onPaymentSuccess(transactionId ?: "GCash transaction completed")
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
     * Build the GCash payment URI for deep linking
     * 
     * GCash URI format:
     * gcash://payment?amount=XXX&merchantname=XXX&merchantref=XXX
     */
    private fun buildPaymentUri(request: PaymentRequest): Uri {
        val builder = Uri.Builder()
            .scheme(GCASH_SCHEME)
            .authority(GCASH_HOST)
            .appendQueryParameter("amount", String.format("%.2f", request.amount))
            .appendQueryParameter("merchantname", "AgriHive")
            .appendQueryParameter("merchantref", request.orderId)
            .appendQueryParameter("description", request.description)
        
        return builder.build()
    }

    /**
     * Create QR code payment via GCash Partner API
     * This requires server-side implementation with proper API keys
     * 
     * @param request Payment request
     * @return Response with QR code URL
     */
    suspend fun createQRPayment(request: PaymentRequest): GCashQRResponse? {
        // This would be implemented with actual API call
        // Using Retrofit or OkHttp to call GCash Partner API
        /*
        val api = GCashApiService.create()
        val response = api.createQRPayment(
            merchantId = MERCHANT_ID,
            amount = request.amount,
            merchantRef = request.orderId,
            description = request.description
        )
        return response
        */
        return null
    }

    /**
     * Check payment status via GCash Partner API
     */
    suspend fun checkPaymentStatus(transactionRef: String): GCashPaymentStatus? {
        // This would be implemented with actual API call
        /*
        val api = GCashApiService.create()
        val response = api.checkStatus(
            merchantId = MERCHANT_ID,
            transactionRef = transactionRef
        )
        return response
        */
        return null
    }
}

/**
 * GCash API Response models
 */
data class GCashQRResponse(
    val qrCode: String,
    val transactionRef: String,
    val expiresAt: String
)

data class GCashPaymentStatus(
    val transactionRef: String,
    val status: String,
    val amount: Double,
    val timestamp: String
)
