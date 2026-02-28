package com.example.agrihive.device

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DeviceControlViewModel : ViewModel() {

    private val _currentTemperature = MutableLiveData<Float>(37.0f)
    val currentTemperature: LiveData<Float> = _currentTemperature

    private val _notificationEnabled = MutableLiveData<Boolean>(true)
    val notificationEnabled: LiveData<Boolean> = _notificationEnabled

    private val _fanAutoTimeSeconds = MutableLiveData<Int>(60) // Default 1 minute
    val fanAutoTimeSeconds: LiveData<Int> = _fanAutoTimeSeconds

    private val _fanAutoControlEnabled = MutableLiveData<Boolean>(false)
    val fanAutoControlEnabled: LiveData<Boolean> = _fanAutoControlEnabled

    fun setCoolingFanNotification(enabled: Boolean) {
        _notificationEnabled.value = enabled
    }

    fun setFanAutoTime(seconds: Int) {
        _fanAutoTimeSeconds.value = seconds
        _fanAutoControlEnabled.value = seconds > 0
    }

    fun updateTemperature(temp: Float) {
        _currentTemperature.value = temp
    }

    // Simulate temperature change (in real app, this would come from IoT sensor)
    fun simulateTemperatureReading(temp: Float) {
        _currentTemperature.value = temp
    }
}
