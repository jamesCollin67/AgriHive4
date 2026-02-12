package com.example.agrihive.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agrihive.data.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val repository = SessionRepository()

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _navigate = MutableLiveData(false)
    val navigate: LiveData<Boolean> = _navigate

    fun startSplash() {
        viewModelScope.launch {
            var progressValue = 0
            while (progressValue <= 100) {
                delay(30)
                progressValue += 2
                _progress.value = progressValue
            }

            // Optional: you can check if user is logged in here
            val isLoggedIn = repository.isUserLoggedIn()

            // Navigate to LandingActivity
            _navigate.value = true
        }
    }
}
