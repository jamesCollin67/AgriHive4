package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SendReportViewModel : ViewModel() {

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _navigateToCapture = MutableLiveData(false)
    val navigateToCapture: LiveData<Boolean> = _navigateToCapture

    private val _navigateToReportSent = MutableLiveData(false)
    val navigateToReportSent: LiveData<Boolean> = _navigateToReportSent

    fun onCaptureAttachmentClicked() {
        _navigateToCapture.value = true
    }

    fun onSubmitClicked(description: String) {
        if (description.isBlank()) {
            _errorMessage.value = "Please describe the issue"
            return
        }
        _navigateToReportSent.value = true
    }

    fun doneCaptureNavigation() {
        _navigateToCapture.value = false
    }

    fun doneReportSentNavigation() {
        _navigateToReportSent.value = false
    }

    fun doneError() {
        _errorMessage.value = null
    }
}
