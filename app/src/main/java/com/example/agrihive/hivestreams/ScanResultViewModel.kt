package com.example.agrihive.hivestreams

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class ScanResultViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var hiveName: String = "Hive"
    private var apiaryId: String? = null
    private var imageUriString: String? = null

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

    fun setScanContext(hiveName: String?, apiaryId: String?, imageUri: String?) {
        this.hiveName = hiveName?.trim()?.takeIf { it.isNotEmpty() } ?: "Hive"
        this.apiaryId = apiaryId
        this.imageUriString = imageUri
    }

    fun setResult(disease: String?, score: Int?) {
        val label = disease?.lowercase() ?: "unknown"
        val displayName = when {
            label.contains("alive_bees") || label == "healthy" -> "Healthy Colony"
            label.contains("dead_bees") -> "Dead Bees / Colony Loss"
            label.contains("varroa_mites") || label.contains("varroa") -> "Varroa Mite Infestation"
            label.contains("chalkbrood") -> "Chalkbrood Infection"
            label.contains("not_a_bee") -> "Not a bee"
            else -> disease ?: "Unknown"
        }
        _diseaseName.value = displayName

        val raw = score?.coerceIn(0, 100) ?: 50
        val adjusted = when (displayName) {
            "Healthy Colony" -> raw.coerceIn(72, 100)
            "Not a bee" -> raw.coerceIn(45, 55)
            else -> raw.coerceAtMost(49).coerceAtLeast(12)
        }
        _healthScore.value = adjusted

        applyDiagnosisCopy(displayName, adjusted)
    }

    private fun applyDiagnosisCopy(displayName: String, score: Int) {
        _symptoms.value = DiagnosisCopy.symptomsFor(displayName)
        _treatments.value = DiagnosisCopy.treatmentsFor(displayName)
        val (label, _, fg) = DiagnosisCopy.severityBadge(score)
        _riskLevel.value = label
        _riskColor.value = fg
    }

    fun onBackClicked() {
        _navigateBack.value = true
    }

    fun onSaveClicked() {
        val uid = auth.currentUser?.uid ?: return
        val disease = _diseaseName.value ?: "Unknown"
        val score = _healthScore.value ?: 0
        val symptomsText = _symptoms.value ?: ""
        val treatmentsText = _treatments.value ?: "Regular monitoring advised"

        _isSaving.value = true

        val uriStr = imageUriString
        if (!uriStr.isNullOrBlank()) {
            val uri = runCatching { Uri.parse(uriStr) }.getOrNull()
            if (uri != null) {
                uploadImageThenSave(uid, uri, disease, score, symptomsText, treatmentsText)
                return
            }
        }
        persistTreatment(uid, "", disease, score, symptomsText, treatmentsText)
    }

    private fun uploadImageThenSave(
        uid: String,
        uri: Uri,
        disease: String,
        score: Int,
        symptomsText: String,
        treatmentsText: String
    ) {
        val ref = storage.reference.child("saved_treatments/$uid/${UUID.randomUUID()}.jpg")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { download ->
                    persistTreatment(uid, download.toString(), disease, score, symptomsText, treatmentsText)
                }.addOnFailureListener {
                    persistTreatment(uid, "", disease, score, symptomsText, treatmentsText)
                }
            }
            .addOnFailureListener {
                persistTreatment(uid, "", disease, score, symptomsText, treatmentsText)
            }
    }

    private fun persistTreatment(
        uid: String,
        imageUrl: String,
        disease: String,
        score: Int,
        symptomsText: String,
        treatmentsText: String
    ) {
        val data = hashMapOf(
            "diseaseName" to disease,
            "healthScore" to score,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "description" to treatmentsText,
            "symptoms" to symptomsText,
            "hiveName" to hiveName,
            "imageUrl" to imageUrl,
            "apiaryId" to (apiaryId ?: "")
        )

        firestore.collection("users").document(uid)
            .collection("saved_treatments")
            .add(data)
            .addOnSuccessListener {
                _isSaving.value = false
                ActivityLogViewModel.getInstance().addLog(
                    LogType.HIVE_SENSOR,
                    "Scanned $hiveName: $disease (Score: $score)"
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
