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

    private val _symptoms = MutableLiveData<String>()
    val symptoms: LiveData<String> = _symptoms

    private val _treatments = MutableLiveData<String>()
    val treatments: LiveData<String> = _treatments

    private val _riskLevel = MutableLiveData<String>()
    val riskLevel: LiveData<String> = _riskLevel

    private val _riskColor = MutableLiveData<String>()
    val riskColor: LiveData<String> = _riskColor

    private val _navigateBack = MutableLiveData(false)
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _navigateToSaved = MutableLiveData(false)
    val navigateToSaved: LiveData<Boolean> = _navigateToSaved

    private val _scanAgain = MutableLiveData(false)
    val scanAgain: LiveData<Boolean> = _scanAgain

    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    fun setResult(disease: String?, score: Int?) {
        val label = disease?.lowercase() ?: "unknown"
        _diseaseName.value = when {
            label.contains("alive_bees") -> "Healthy Colony"
            label.contains("dead_bees") -> "Dead Bees / Colony Loss"
            label.contains("varroa_mites") -> "Varroa Mite Infestation"
            label.contains("not_a_bee") -> "Unknown Object Detected"
            else -> disease ?: "Unknown"
        }
        
        if (score != null) _healthScore.value = score.coerceIn(0, 100)
        
        updateTreatmentInfo(_diseaseName.value ?: "Unknown")
    }

    private fun updateTreatmentInfo(name: String) {
        when (name) {
            "Healthy Colony" -> {
                _symptoms.value = "• Active bees with steady hum\n• Clear entrance\n• Healthy brood patterns\n• Bees bringing in pollen"
                _treatments.value = "1. Regular monitoring monthly\n2. Ensure fresh water source nearby\n3. Check for mites every 30 days\n4. Maintain enough food stores"
                _riskLevel.value = "Healthy"
                _riskColor.value = "#66BB6A"
            }
            "Varroa Mite Infestation" -> {
                _symptoms.value = "• Visible mites on bees' backs\n• Deformed wings (DWV)\n• Patchy brood pattern\n• Crawling bees at hive entrance"
                _treatments.value = "1. Apply approved mite treatment (Oxalic/Formic Acid)\n2. Use drone brood removal\n3. Re-queen with mite-resistant stock\n4. Screened bottom boards"
                _riskLevel.value = "High Risk"
                _riskColor.value = "#FF9800"
            }
            "Dead Bees / Colony Loss" -> {
                _symptoms.value = "• Pile of dead bees at entrance or floor\n• No activity during warm weather\n• Cold hive with no vibration\n• Intact food stores but dead bees"
                _treatments.value = "1. Clean out the hive and debris\n2. Investigate for starvation or disease\n3. Sterilize equipment before reuse\n4. Seal hive to prevent robbing"
                _riskLevel.value = "Critical"
                _riskColor.value = "#EF5350"
            }
            else -> {
                _symptoms.value = "• AI did not detect a specific bee condition\n• Image might be blurry or far away\n• Non-bee object in focus"
                _treatments.value = "1. Ensure good lighting and focus\n2. Crop image closer to the bees\n3. Clean camera lens\n4. Try capturing a different angle"
                _riskLevel.value = "Unknown"
                _riskColor.value = "#9CAF9F"
            }
        }
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
            "description" to (_treatments.value ?: "Regular monitoring advised"),
            "hiveName" to "Hive Alpha"
        )

        firestore.collection("users").document(uid)
            .collection("saved_treatments")
            .add(savedTreatment)
            .addOnSuccessListener {
                _isSaving.value = false
                
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
