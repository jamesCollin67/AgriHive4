package com.example.agrihive.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * PayPal Payment Service Implementation
 * 
 * PayPal provides payment integration through:
 * 1. PayPal Android SDK - Direct in-app payments
 * 2. PayPal Checkout - Web-based payments
 * 3. PayPal App Deep Link - For app-to-app payments
 * 
 * This implementation uses PayPal's SDK approach with fallback to deep linking.
 */
class PayPalPaymentService : PaymentService {

    companion object {
        // PayPal app package name
        const val PAYPAL_PACKAGE = "com.paypal.android.p2pmobile"
        const val PAYPAL_SANDBOX_PACKAGE = "com.paypal.hack"
        
        // PayPal Deep Link scheme
        const val PAYPAL_SCHEME = "paypal"
        const val PAYPAL_HOST = "payment"
        
        // PayPal API Base URL
        const val PAYPAL_API_BASE = "https://api.paypal.com"
        const val PAYPAL_SANDBOX_BASE = "https://api.sandbox.paypal.com"
        
        // PayPal Checkout API endpoints
        const val PAYPAL_ORDERS = "/v2/checkout/orders"
        const val PAYPAL_CAPTURE = "/v2/checkout/orders/{order_id}/capture"
        
        // For demo: Replace with actual credentials from PayPal Developer Portal
        const val PAYPAL_CLIENT_ID = "YOUR_PAYPAL_CLIENT_ID"
        const val PAYPAL_CLIENT_SECRET = "YOUR_PAYPAL_CLIENT_SECRET"
        
        // PayPal deep link return URL
        const val RETURN_URL = "agrhive://paypal-return"
        const val CANCEL_URL = "agrhive://paypal-cancel"
    }

    override fun isAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(PAYPAL_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo(PAYPAL_SANDBOX_PACKAGE, 0)
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    override fun getAppPackageName(): String = PAYPAL_PACKAGE

    override fun initiatePayment(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        // Option 1: If PayPal app is installed, use deep link
        if (isAppInstalled(context)) {
            initiateViaApp(context, request, callback)
        } else {
            // Option 2: Use PayPal web checkout
            initiateViaWeb(context, request, callback)
        }
    }

    /**
     * Initiate payment via PayPal app
     */
    private fun initiateViaApp(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        try {
            val paymentUri = buildPaymentUri(request)
            
            val intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            callback.onPaymentSuccess("Payment initiated - PayPal app opened")
        } catch (e: Exception) {
            // Fall back to web checkout
            initiateViaWeb(context, request, callback)
        }
    }

    /**
     * Initiate payment via PayPal web checkout
     */
    private fun initiateViaWeb(context: Context, request: PaymentRequest, callback: PaymentCallback) {
        try {
            // Build PayPal checkout URL
            val checkoutUrl = buildWebCheckoutUrl(request)
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            callback.onPaymentSuccess("Payment initiated - PayPal web checkout opened")
        } catch (e: Exception) {
            callback.onPaymentFailure("Failed to initiate PayPal payment: ${e.message}")
        }
    }

    override fun getPaymentIntent(context: Context, request: PaymentRequest): Intent? {
        return try {
            val paymentUri = buildPaymentUri(request)
            val intent = Intent(Intent.ACTION_VIEW, paymentUri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent
        } catch (e: Exception) {
            null
        }
    }

    override fun handleReturnIntent(data: Intent?, callback: PaymentCallback) {
        data?.let { intent ->
            // Handle the return URL from PayPal
            val uri = intent.data
            uri?.let { paymentUri ->
                val status = paymentUri.getQueryParameter("status")
                val transactionId = paymentUri.getQueryParameter("transactionId")
                
                when (status) {
                    "COMPLETED" -> {
                        callback.onPaymentSuccess(transactionId ?: "PayPal transaction completed")
                    }
                    "CANCELLED" -> {
                        callback.onPaymentCancelled()
                    }
                    else -> {
                        val error = paymentUri.getQueryParameter("error")
                        callback.onPaymentFailure(error ?: "Payment failed")
                    }
                }
            }
        } ?: run {
            callback.onPaymentCancelled()
        }
    }

    /**
     * Build PayPal app deep link URI
     */
    private fun buildPaymentUri(request: PaymentRequest): Uri {
        val builder = Uri.Builder()
            .scheme(PAYPAL_SCHEME)
            .authority(PAYPAL_HOST)
            .appendQueryParameter("amount", String.format("%.2f", request.amount))
            .appendQueryParameter("currency", request.currency)
            .appendQueryParameter("merchantname", "AgriHive")
            .appendQueryParameter("merchantref", request.orderId)
            .appendQueryParameter("description", request.description)
            .appendQueryParameter("returnurl", RETURN_URL)
            .appendQueryParameter("cancelurl", CANCEL_URL)
        
        return builder.build()
    }

    /**
     * Build PayPal web checkout URL
     * This is used when PayPal app is not installed
     */
    private fun buildWebCheckoutUrl(request: PaymentRequest): String {
        val baseUrl = "$PAYPAL_API_BASE$PAYPAL_ORDERS"
        
        // In a real implementation, you would:
        // 1. Create an order via PayPal API server-side
        // 2. Get the approval URL from the response
        // 3. Open that URL in a webview/browser
        
        // For demo purposes, we construct a basic checkout URL
        return "$baseUrl?client_id=$PAYPAL_CLIENT_ID" +
                "&amount=${String.format("%.2f", request.amount)}" +
                "&currency=${request.currency}" +
                "&description=${Uri.encode(request.description)}" +
                "&return_url=${Uri.encode(RETURN_URL)}" +
                "&cancel_url=${Uri.encode(CANCEL_URL)}"
    }

    /**
     * Create PayPal order via API
     * This requires server-side implementation with proper API credentials
     */
    suspend fun createOrder(request: PaymentRequest): PayPalOrderResponse? {
        // This would be implemented with actual API call to PayPal
        // Using OAuth 2.0 for authentication
        /*
        val api = PayPalApiService.create()
        val response = api.createOrder(
            intent = "CAPTURE",
            amount = request.amount,
            currency = request.currency,
            description = request.description
        )
        return response
        */
        return null
    }

    /**
     * Capture PayPal order
     */
    suspend fun captureOrder(orderId: String): PayPalCaptureResponse? {
        // This would be implemented with actual API call
        return null
    }

    /**
     * Check payment status via PayPal API
     */
    suspend fun checkOrderStatus(orderId: String): PayPalOrderStatus? {
        // This would be implemented with actual API call
        return null
    }
}

/**
 * PayPal API Response models
 */
data class PayPalOrderResponse(
    val orderId: String,
    val status: String,
    val approveLink: String,
    val expiresAt: String
)

data class PayPalCaptureResponse(
    val orderId: String,
    val status: String,
    val transactionId: String,
    val amount: Double,
    val currency: String
)

data class PayPalOrderStatus(
    val orderId: String,
    val status: String,
    val amount: Double,
    val currency: String,
    val createTime: String,
    val updateTime: String
)
