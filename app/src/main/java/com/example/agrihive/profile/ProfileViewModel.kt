package com.example.agrihive.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val sessionManager = UserSessionManager(application)
    private var apiaryListener: ListenerRegistration? = null

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _apiaryCount = MutableLiveData<Int>()
    val apiaryCount: LiveData<Int> = _apiaryCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _goEdit = MutableLiveData<Boolean>()
    val goEdit: LiveData<Boolean> = _goEdit

    private val _goDashboard = MutableLiveData<Boolean>()
    val goDashboard: LiveData<Boolean> = _goDashboard

    private val _goSettings = MutableLiveData<Boolean>()
    val goSettings: LiveData<Boolean> = _goSettings

    init {
        loadUser()
        listenApiaryCount()
    }

    private fun loadUser() {
        val uid = auth.currentUser?.uid ?: return
        
        // Load from Session Manager first for instant display
        if (sessionManager.hasUserData()) {
            _user.value = User(
                uid = sessionManager.getUid(),
                firstName = sessionManager.getFirstName(),
                lastName = sessionManager.getLastName(),
                email = sessionManager.getEmail(),
                farm = sessionManager.getFarm(),
                location = sessionManager.getLocation(),
                apiaries = sessionManager.getApiaries()
            )
        }
        
        // Then get from server to ensure accuracy
        _isLoading.value = true
        firestore.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                _isLoading.value = false
                val userData = doc.toObject(User::class.java)
                userData?.let {
                    _user.value = it
                    // Sync session manager with latest data
                    sessionManager.saveUserData(
                        it.firstName, it.lastName, it.email, it.farm, it.location, it.apiaries, it.uid
                    )
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    fun refreshUserData() {
        loadUser()
        listenApiaryCount()
    }

    private fun listenApiaryCount() {
        val uid = auth.currentUser?.uid ?: return
        apiaryListener?.remove()
        apiaryListener = firestore.collection("apiaries")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, _ ->
                _apiaryCount.value = snapshot?.size() ?: 0
            }
    }

    fun editClicked() {
        _goEdit.value = true
    }

    fun backClicked() {
        _goDashboard.value = true
    }

    fun settingsClicked() {
        _goSettings.value = true
    }

    fun doneNav() {
        _goEdit.value = false
        _goDashboard.value = false
        _goSettings.value = false
    }

    override fun onCleared() {
        super.onCleared()
        apiaryListener?.remove()
    }
}