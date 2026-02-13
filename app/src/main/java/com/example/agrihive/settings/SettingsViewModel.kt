package com.example.agrihive.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    // Switch states
    private val _notificationsEnabled = MutableLiveData(true)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled

    private val _cloudSyncEnabled = MutableLiveData(true)
    val cloudSyncEnabled: LiveData<Boolean> = _cloudSyncEnabled

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun toggleCloudSync(enabled: Boolean) {
        _cloudSyncEnabled.value = enabled
    }
}
