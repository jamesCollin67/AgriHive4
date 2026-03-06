package com.example.agrihive.payment

import com.example.agrihive.subscription.PaymentMethod

/**
 * Factory class to create payment service instances based on payment method
 */
object PaymentServiceFactory {

    /**
     * Get the appropriate payment service for the given payment method
     */
    fun getPaymentService(paymentMethod: PaymentMethod): PaymentService {
        return when (paymentMethod) {
            PaymentMethod.GCASH -> GCashPaymentService()
            PaymentMethod.PAYMAYA -> PayMayaPaymentService()
            PaymentMethod.BDO -> BDOPaymentService()
            PaymentMethod.PAYPAL -> PayPalPaymentService()
        }
    }

    /**
     * Check if the payment app is installed for the given payment method
     */
    fun isPaymentAppInstalled(paymentMethod: PaymentMethod, context: android.content.Context): Boolean {
        return getPaymentService(paymentMethod).isAppInstalled(context)
    }

    /**
     * Get display message for when payment app is not installed
     */
    fun getAppNotInstalledMessage(paymentMethod: PaymentMethod): String {
        return when (paymentMethod) {
            PaymentMethod.GCASH -> "GCash app is not installed. Please install GCash from the Play Store."
            PaymentMethod.PAYMAYA -> "PayMaya app is not installed. Please install PayMaya from the Play Store."
            PaymentMethod.BDO -> "BDO Online Banking app is not installed. Please install it from the Play Store."
            PaymentMethod.PAYPAL -> "PayPal app is not installed. Please install PayPal from the Play Store."
        }
    }
}
