package com.example.agrihive.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _apiaryCount = MutableLiveData<Int>()
    val apiaryCount: LiveData<Int> = _apiaryCount

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

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                val userData = document.toObject(User::class.java)
                userData?.let { _user.value = it }
            }
    }

    private fun listenApiaryCount() {
        val uid = auth.currentUser?.uid ?: return

        realtimeDb.child("apiaries")
            .orderByChild("ownerId")
            .equalTo(uid)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    var count = 0

                    for (child in snapshot.children) {
                        val isActive = child.child("isActive").getValue(Boolean::class.java) ?: false
                        if (isActive) count++
                    }

                    _apiaryCount.value = count
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun editClicked() { _goEdit.value = true }
    fun backClicked() { _goDashboard.value = true }
    fun settingsClicked() { _goSettings.value = true }

    fun doneNav() {
        _goEdit.value = false
        _goDashboard.value = false
        _goSettings.value = false
    }
}
