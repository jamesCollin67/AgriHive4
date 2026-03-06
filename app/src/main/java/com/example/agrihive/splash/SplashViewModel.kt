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

            // Check if user is logged in
            val isLoggedIn = firebaseAuth.currentUser != null

            // Navigate based on login status
            if (isLoggedIn) {
                _navigate.value = "dashboard"
            } else {
                _navigate.value = "landing"
            }
        }
    }

    fun doneNavigating() {
        _navigate.value = null
    }
}
