package com.example.agrihive.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PaymentSuccessViewModel : ViewModel() {

    private val _navigateDashboard = MutableLiveData(false)
    val navigateDashboard: LiveData<Boolean> = _navigateDashboard

    fun onBackToDashboardClicked() {
        _navigateDashboard.value = true
    }

    fun doneNavigateDashboard() {
        _navigateDashboard.value = false
    }
}
