package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SavedTreatmentsViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _treatments = MutableLiveData<List<SavedTreatment>>()
    val treatments: LiveData<List<SavedTreatment>> = _treatments

    private val _totalScans = MutableLiveData(0)
    val totalScans: LiveData<Int> = _totalScans

    private val _issuesFound = MutableLiveData(0)
    val issuesFound: LiveData<Int> = _issuesFound

    private val _healthyCount = MutableLiveData(0)
    val healthyCount: LiveData<Int> = _healthyCount

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadTreatments()
    }

    fun loadTreatments() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        firestore.collection("users").document(uid)
            .collection("saved_treatments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                _isLoading.value = false
                if (e != null || snapshot == null) return@addSnapshotListener

                val list = snapshot.toObjects(SavedTreatment::class.java)
                _treatments.value = list
                
                // Calculate stats
                _totalScans.value = list.size
                _issuesFound.value = list.count { it.healthScore < 80 }
                _healthyCount.value = list.count { it.healthScore >= 80 }
            }
    }

    fun onBackClicked() {
        _navigateBack.value = true
    }

    fun doneBack() {
        _navigateBack.value = false
    }
}
