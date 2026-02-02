package com.example.agrihive.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress

    private val _navigate = MutableLiveData<Boolean>()
    val navigate: LiveData<Boolean> = _navigate

    init {
        startLoading()
    }

    private fun startLoading() {
        viewModelScope.launch {
            var value = 0
            while (value <= 100) {
                _progress.value = value
                delay(30)
                value += 4
            }
            _navigate.value = true
        }
    }
}
