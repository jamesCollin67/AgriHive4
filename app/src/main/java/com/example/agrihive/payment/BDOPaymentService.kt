package com.example.agrihive.payment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * BDO Payment Service Implementation
 * 
 * BDO provides payment integration through:
 * 1. BDO Online Banking App Deep Link - For app-to-app payments
 * 2. BDO Payment Gateway API - For merchant integrations (requires BDO Business Banking)
 * 
 * This implementation uses BDO's online banking deep link protocol.
 * Note: BDO requires corporate banking enrollment for API access.
 */
class BDOPaymentService : PaymentService {

    companion object {
        // BDO Online Banking app package name
        const val BDO_PACKAGE = "com.bdo.onlinebanking"
        const val BDO_OLD_PACKAGE = "com.bdo.client.foundation.ent.branch"  // Legacy app
        
        // BDO Deep Link scheme
        const val BDO_SCHEME = "bdo"
        const val BDO_HOST = "payment"
        
        // BDO API Base URL (for server-side integration)
        const val BDO_API_BASE = "https://online.bdo.com.ph"
        
        // BDO Payment Gateway endpoints
        const val BDO_PAYMENT_GATEWAY = "/bdo-paygateway/api/v1"
        const val BDO_CREATE_ORDER = "/orders"
        const val BDO_CHECK_STATUS = "/orders/status"
        
        // For demo: Replace with actual credentials from BDO
        const val MERCHANT_ID = "YOUR_BDO_MERCHANT_ID"
        const val MERCHANT_KEY = "YOUR_BDO_MERCHANT_KEY"
    }

    override fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(BDO_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo(BDO_OLD_PACKAGE, 0)
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    override fun getAppPackageName(): String = BDO_PACKAGE

    override fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        if (!isAppInstalled(context)) {
            // Try to open BDO online banking website as fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://online.bdo.com.ph/session"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
                callback.onPaymentSuccess("BDO Online Banking website opened")
            } catch (e: Exception) {
                callback.onPaymentFailure("BDO app is not installed. Please install BDO Online Banking from the Play Store.")
            }
            return
        }

        try {
            val paymentUri = buildPaymentUri(request)
            val intent = context.packageManager.getLaunchIntentForPackage(BDO_PACKAGE)
            
            if (intent != null) {
                intent.data = paymentUri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback.onPaymentSuccess("Payment initiated - BDO app opened")
            } else {
                // Try legacy app
                val legacyIntent = context.packageManager.getLaunchIntentForPackage(BDO_OLD_PACKAGE)
                if (legacyIntent != null) {
                    legacyIntent.data = paymentUri
                    legacyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(legacyIntent)
                    callback.onPaymentSuccess("Payment initiated - BDO app opened")
                } else {
                    callback.onPaymentFailure("Unable to launch BDO app")
                }
            }
        } catch (e: Exception) {
            callback.onPaymentFailure("Failed to initiate BDO payment: ${e.message}")
        }
    }

    override fun getPaymentIntent(context: Context, request: PaymentRequest): Intent? {
        return try {
            val paymentUri = buildPaymentUri(request)
            
            // Try primary app first, then legacy
            var intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage(BDO_PACKAGE)
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
                    callback.onPaymentSuccess(transactionId ?: "BDO transaction completed")
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
     * Build the BDO payment URI for deep linking
     */
    private fun buildPaymentUri(request: PaymentRequest): Uri {
        val builder = Uri.Builder()
            .scheme(BDO_SCHEME)
            .authority(BDO_HOST)
            .appendQueryParameter("amount", String.format("%.2f", request.amount))
            .appendQueryParameter("merchantname", "AgriHive")
            .appendQueryParameter("merchantref", request.orderId)
            .appendQueryParameter("description", request.description)
        
        return builder.build()
    }

    /**
     * Create BDO Payment Gateway order via API
     * This requires server-side implementation with BDO Business Banking credentials
     */
    suspend fun createPaymentOrder(request: PaymentRequest): BDOPaymentResponse? {
        // This would be implemented with actual API call
        /*
        val api = BDOApiService.create()
        val response = api.createOrder(
            merchantId = MERCHANT_ID,
            amount = request.amount,
            currency = request.currency,
            orderReference = request.orderId,
            description = request.description
        )
        return response
        */
        return null
    }

    /**
     * Check payment status via BDO Payment Gateway API
     */
    suspend fun checkPaymentStatus(orderId: String): BDOPaymentStatus? {
        // This would be implemented with actual API call
        return null
    }
}

/**
 * BDO API Response models
 */
data class BDOPaymentResponse(
    val orderId: String,
    val paymentUrl: String,
    val expiresAt: String
)

data class BDOPaymentStatus(
    val orderId: String,
    val status: String,
    val amount: Double,
    val transactionDate: String?
)
