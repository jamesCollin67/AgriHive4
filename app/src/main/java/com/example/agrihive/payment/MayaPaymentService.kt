package com.example.agrihive.payment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Maya Payment Service Implementation
 * 
 * Maya provides payment integration through:
 * 1. Maya App Deep Link - For app-to-app payments
 * 2. Maya Checkout API - For web-based payments
 * 3. Maya QR API - For QR code payments
 * 
 * This implementation uses the Maya deep link protocol.
 */
class MayaPaymentService : PaymentService {

    companion object {
        // Maya app package names (multiple options for different versions)
        val MAYA_PACKAGES = listOf(
            "com.maya.Maya",
            "com.paymaya.ui.android",
            "ph.com.maya",
            "com.maya",
            "com.maya.wallet",
            "com.voyager.maya",
            "com.maya.login"
        )
        
        // Maya Deep Link scheme
        const val MAYA_SCHEME = "maya"
        const val MAYA_HOST = "pay"
        
        // Maya API Base URL (for server-side integration)
        const val MAYA_API_BASE = "https://api.maya.com"
        
        // Maya Checkout API endpoints
        const val MAYA_CHECKOUT = "/checkout/v1/checkouts"
        const val MAYA_QR = "/payments/v1/qr"
        
        // For demo: Replace with actual credentials from Maya
        const val PUBLIC_API_KEY = "YOUR_MAYA_PUBLIC_KEY"
        const val SECRET_API_KEY = "YOUR_MAYA_SECRET_KEY"
    }

    override fun isAppInstalled(context: Context): Boolean {
        // Don't check for specific package - just try to open with implicit intent
        // This is more reliable as Maya may have different package names
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("maya://pay")
            intent.resolveActivity(context.packageManager) != null
        } catch (e: Exception) {
            false
        }
    }

    override fun getAppPackageName(): String {
        // Return the first package in the list (preferred)
        return MAYA_PACKAGES.first()
    }

    override fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        // Try to find an installed Maya app
        var installedPackage: String? = null
        val packageManager = context.packageManager
        for (packageName in MAYA_PACKAGES) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                installedPackage = packageName
                break
            } catch (e: PackageManager.NameNotFoundException) {
                // Try next package
            }
        }

        if (installedPackage == null) {
            // Try opening Maya using implicit intent (let system decide)
            try {
                val paymentUri = buildPaymentUri(request)
                val intent = Intent(Intent.ACTION_VIEW, paymentUri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback.onPaymentSuccess("Payment initiated - Opened Maya")
                return
            } catch (e: Exception) {
                // If implicit intent fails, show the error
                callback.onPaymentFailure("Maya app is not installed. Please install Maya from the Play Store.")
                return
            }
        }

        try {
            val paymentUri = buildPaymentUri(request)
            val intent = packageManager.getLaunchIntentForPackage(installedPackage)
            
            if (intent != null) {
                intent.data = paymentUri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                callback.onPaymentSuccess("Payment initiated - Maya app opened")
            } else {
                callback.onPaymentFailure("Unable to launch Maya app")
            }
        } catch (e: Exception) {
            callback.onPaymentFailure("Failed to initiate Maya payment: ${e.message}")
        }
    }

    override fun getPaymentIntent(context: Context, request: PaymentRequest): Intent? {
        // Try to find an installed Maya app
        var installedPackage: String? = null
        val packageManager = context.packageManager
        for (packageName in MAYA_PACKAGES) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                installedPackage = packageName
                break
            } catch (e: PackageManager.NameNotFoundException) {
                // Try next package
            }
        }

        if (installedPackage == null) {
            return null
        }

        return try {
            val paymentUri = buildPaymentUri(request)
            val intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setPackage(installedPackage)
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
                    callback.onPaymentSuccess(transactionId ?: "Maya transaction completed")
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
     * Build the Maya payment URI for deep linking
     * 
     * Maya URI format:
     * maya://pay?amount=XXX&merchantname=XXX&merchantref=XXX
     */
    private fun buildPaymentUri(request: PaymentRequest): Uri {
        val builder = Uri.Builder()
            .scheme(MAYA_SCHEME)
            .authority(MAYA_HOST)
            .appendQueryParameter("amount", String.format("%.2f", request.amount))
            .appendQueryParameter("merchantname", "AgriHive")
            .appendQueryParameter("merchantref", request.orderId)
            .appendQueryParameter("description", request.description)
        
        return builder.build()
    }

    /**
     * Create Maya checkout session via API
     * This requires server-side implementation with proper API keys
     */
    suspend fun createCheckout(request: PaymentRequest): MayaCheckoutResponse? {
        // This would be implemented with actual API call
        /*
        val api = MayaApiService.create()
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
     * Create Maya QR code via API
     */
    suspend fun createQRPayment(request: PaymentRequest): MayaQRResponse? {
        // This would be implemented with actual API call
        /*
        val api = MayaApiService.create()
        val response = api.createQR(
            amount = request.amount,
            requestReference = request.orderId
        )
        return response
        */
        return null
    }

    /**
     * Check payment status via Maya API
     */
    suspend fun checkPaymentStatus(checkoutId: String): MayaPaymentStatus? {
        // This would be implemented with actual API call
        return null
    }
}

/**
 * Maya API Response models
 */
data class MayaCheckoutResponse(
    val checkoutId: String,
    val redirectUrl: String,
    val expiresAt: String
)

data class MayaQRResponse(
    val qrCode: String,
    val requestReference: String,
    val expiresAt: String
)

data class MayaPaymentStatus(
    val checkoutId: String,
    val status: String,
    val amount: Double,
    val paymentToken: String?
)
