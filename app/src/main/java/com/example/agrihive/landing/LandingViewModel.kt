package com.example.agrihive.landing

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LandingViewModel : ViewModel() {

    // LiveData to handle navigation
    private val _navigateToLogin = MutableLiveData<Boolean>()
    val navigateToLogin: LiveData<Boolean> = _navigateToLogin

    fun onGetStartedClicked() {
        _navigateToLogin.value = true
    }

    // Call this after navigation to reset state
    fun doneNavigating() {
        _navigateToLogin.value = false
    }
}
