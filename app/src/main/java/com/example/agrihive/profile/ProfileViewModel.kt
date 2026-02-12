package com.example.agrihive.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user


    private val _goEdit = MutableLiveData<Boolean>()
    val goEdit: LiveData<Boolean> = _goEdit

    private val _goDashboard = MutableLiveData<Boolean>()
    val goDashboard: LiveData<Boolean> = _goDashboard

    private val _goSettings = MutableLiveData<Boolean>()
    val goSettings: LiveData<Boolean> = _goSettings


    init {
        loadUser()
    }

    private fun loadUser(){

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener {

                val userData = it.toObject(User::class.java)
                _user.value = userData
            }
    }


    fun editClicked(){
        _goEdit.value = true
    }

    fun backClicked(){
        _goDashboard.value = true
    }

    fun settingsClicked(){
        _goSettings.value = true
    }

    fun doneNav(){
        _goEdit.value = false
        _goDashboard.value = false
        _goSettings.value = false
    }
}