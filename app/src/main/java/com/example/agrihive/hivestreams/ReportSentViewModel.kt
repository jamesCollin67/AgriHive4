package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class ReportSentViewModel : ViewModel() {

    private val _ticketId = MutableLiveData(generateTicketId())
    val ticketId: LiveData<String> = _ticketId

    private val _submittedDate = MutableLiveData(
        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
    )
    val submittedDate: LiveData<String> = _submittedDate

    private val _navigateDashboard = MutableLiveData(false)
    val navigateDashboard: LiveData<Boolean> = _navigateDashboard

    fun onBackToDashboardClicked() {
        _navigateDashboard.value = true
    }

    fun doneNavigateDashboard() {
        _navigateDashboard.value = false
    }

    private fun generateTicketId(): String = "RPT-${Random.nextInt(10000000, 99999999)}"
}
