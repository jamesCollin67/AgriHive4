package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.addapiary.Apiary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HiveStreamsViewModel : ViewModel() {

    private val database = FirebaseDatabase.getInstance().reference

    private val _apiaryData = MutableLiveData<Apiary?>()
    val apiaryData: LiveData<Apiary?> = _apiaryData

    private val _weightAnalytics = MutableLiveData<WeightAnalyticsData?>()
    val weightAnalytics: LiveData<WeightAnalyticsData?> = _weightAnalytics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var apiaryListener: ValueEventListener? = null
    private var analyticsListener: ValueEventListener? = null

    fun startListening(apiaryId: String) {
        _isLoading.value = true
        apiaryListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _isLoading.value = false
                val apiary = snapshot.getValue(Apiary::class.java)
                _apiaryData.value = apiary
            }

            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false
                _errorMessage.value = error.message
            }
        }

        database.child("apiaries").child(apiaryId).addValueEventListener(apiaryListener!!)
        
        // Listen for weight analytics/history
        fetchWeightAnalytics(apiaryId)
    }

    private fun fetchWeightAnalytics(apiaryId: String) {
        analyticsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val analytics = snapshot.getValue(WeightAnalyticsData::class.java)
                _weightAnalytics.value = analytics
            }

            override fun onCancelled(error: DatabaseError) {
                _errorMessage.value = "Failed to load analytics: ${error.message}"
            }
        }
        database.child("weight_analytics").child(apiaryId).addValueEventListener(analyticsListener!!)
    }

    fun stopListening(apiaryId: String) {
        apiaryListener?.let {
            database.child("apiaries").child(apiaryId).removeEventListener(it)
        }
        analyticsListener?.let {
            database.child("weight_analytics").child(apiaryId).removeEventListener(it)
        }
    }
}
