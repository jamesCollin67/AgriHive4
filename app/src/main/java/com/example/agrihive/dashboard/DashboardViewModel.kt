package com.example.agrihive.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DashboardViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")

    private val _showSubscription = MutableLiveData<Boolean>()
    val showSubscription: LiveData<Boolean> = _showSubscription

    private val _navigateToAddApiary = MutableLiveData<Boolean>()
    val navigateToAddApiary: LiveData<Boolean> = _navigateToAddApiary

    fun checkSubscription() {
        val uid = auth.currentUser?.uid ?: return

        usersRef.child(uid).child("subscription")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    _showSubscription.value = !snapshot.exists()
                }

                override fun onCancelled(error: DatabaseError) {
                    _showSubscription.value = true
                }
            })
    }

    fun onAddApiaryClicked() {
        _navigateToAddApiary.value = true
    }

    fun doneNavigatingAddApiary() {
        _navigateToAddApiary.value = false
    }

    fun doneShowingSubscription() {
        _showSubscription.value = false
    }
}
