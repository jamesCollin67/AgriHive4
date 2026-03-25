package com.example.agrihive.hivestreams

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ScanResultViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _diseaseName = MutableLiveData("Healthy")
    val diseaseName: LiveData<String> = _diseaseName

    private val _healthScore = MutableLiveData(100)
    val healthScore: LiveData<Int> = _healthScore

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _navigateToSaved = MutableLiveData(false)
    val navigateToSaved: LiveData<Boolean> = _navigateToSaved

    private val _scanAgain = MutableLiveData(false)
    val scanAgain: LiveData<Boolean> = _scanAgain

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    fun setResult(disease: String?, score: Int?) {
        if (!disease.isNullOrBlank()) _diseaseName.value = disease
        if (score != null) _healthScore.value = score.coerceIn(0, 100)
    }

    fun onBackClicked() {
        _navigateBack.value = true
    }

    fun onSaveClicked() {
        val uid = auth.currentUser?.uid ?: return
        val disease = _diseaseName.value ?: "Unknown"
        val score = _healthScore.value ?: 0
        
        _isSaving.value = true
        
        val savedTreatment = hashMapOf(
            "diseaseName" to disease,
            "healthScore" to score,
            "timestamp" to System.currentTimeMillis(),
            "description" to if (score < 50) "Urgent treatment required" else "Regular monitoring advised",
            "hiveName" to "Hive Alpha"
        )

        firestore.collection("users").document(uid)
            .collection("saved_treatments")
            .add(savedTreatment)
            .addOnSuccessListener {
                _isSaving.value = false
                
                // Log to Activity Log
                ActivityLogViewModel.getInstance().addLog(
                    LogType.HIVE_SENSOR,
                    "Scanned hive: Found $disease (Score: $score)"
                )

                _navigateToSaved.value = true
            }
            .addOnFailureListener {
                _isSaving.value = false
            }
    }

    fun onScanAgainClicked() {
        _scanAgain.value = true
    }

    fun doneBack() {
        _navigateBack.value = false
    }

    fun doneNavigateToSaved() {
        _navigateToSaved.value = false
    }

    fun doneScanAgain() {
        _scanAgain.value = false
    }
}
