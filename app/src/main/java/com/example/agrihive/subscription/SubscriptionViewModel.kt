package com.example.agrihive.subscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubscriptionViewModel : ViewModel() {

    private val _selectedPlan = MutableLiveData<SubscriptionPlan?>()
    val selectedPlan: LiveData<SubscriptionPlan?> = _selectedPlan

    private val _selectedPaymentMethod = MutableLiveData<PaymentMethod?>()
    val selectedPaymentMethod: LiveData<PaymentMethod?> = _selectedPaymentMethod

    private val _isProcessing = MutableLiveData<Boolean>(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _paymentSuccess = MutableLiveData<Boolean>(false)
    val paymentSuccess: LiveData<Boolean> = _paymentSuccess

    fun setSelectedPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun setPaymentSuccess(success: Boolean) {
        _paymentSuccess.value = success
    }

    fun processPayment() {
        val plan = _selectedPlan.value
        val paymentMethod = _selectedPaymentMethod.value

        if (plan == null || paymentMethod == null) {
            return
        }

        _isProcessing.value = true

        // Simulate payment processing
        // In production, this would call a payment gateway API
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isProcessing.value = false
            _paymentSuccess.value = true
        }, 2000)
    }

    fun resetPayment() {
        _paymentSuccess.value = false
        _selectedPaymentMethod.value = null
    }

    companion object {
        // Pre-defined plans for the checkout screen
        fun getDefaultPlan(): SubscriptionPlan {
            return SubscriptionPlan(
                id = "default_ai_assistance",
                name = "Sensors with AI Assistance",
                description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
                apiaryTier = ApiaryTier.TIER_1_2,
                billingType = BillingType.MONTHLY,
                price = 899.00
            )
        }
    }
}
