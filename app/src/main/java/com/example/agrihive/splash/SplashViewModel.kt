package com.example.agrihive.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _progress = MutableLiveData(0)
    val progress: LiveData<Int> = _progress

    private val _navigate = MutableLiveData<String?>()
    val navigate: LiveData<String?> = _navigate

    fun startSplash() {
        viewModelScope.launch {
            var progressValue = 0
            while (progressValue <= 100) {
                delay(30)
                progressValue += 2
                _progress.value = progressValue
            }

            // Always go to landing/welcome screen first as requested
            // This allows user to login or register
            _navigate.value = "landing"
        }
    }

    fun doneNavigating() {
        _navigate.value = null
    }
}
