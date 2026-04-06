package com.example.agrihive.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PaymentDetailsViewModel : ViewModel() {

    enum class Method { GCASH, PAYMAYA, BDO, PAYPAL }

    private val _selectedMethod = MutableLiveData(Method.GCASH)
    val selectedMethod: LiveData<Method> = _selectedMethod

    private val _planPrice = MutableLiveData<Double>(750.0)
    val planPrice: LiveData<Double> = _planPrice

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    fun setPlanDetails(price: Double) {
        if (price > 0) {
            _planPrice.value = price
        }
    }

    fun selectMethod(method: Method) {
        _selectedMethod.value = method
    }

    fun onBackClicked() {
        _navigateBack.value = true
    }

    fun doneBack() {
        _navigateBack.value = false
    }
}
