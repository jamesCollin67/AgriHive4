package com.example.agrihive.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

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
        
        // First try to get cached data for immediate display
        firestore.collection("users").document(uid)
            .get(Source.CACHE)
            .addOnSuccessListener { doc ->
                val userData = doc.toObject(User::class.java)
                userData?.let {
                    _user.value = it
                }
            }
        
        // Then get from server and show loading if it takes time
        _isLoading.value = true
        firestore.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                _isLoading.value = false
                val userData = doc.toObject(User::class.java)
                userData?.let {
                    _user.value = it
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
        realtimeDb.child("apiaries").orderByChild("ownerId").equalTo(uid)
            .addValueEventListener(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount.toInt()
                    _apiaryCount.value = count
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
}