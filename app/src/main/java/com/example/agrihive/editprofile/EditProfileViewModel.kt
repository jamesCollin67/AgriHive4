package com.example.agrihive.editprofile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _apiaryCount = MutableLiveData<Int>()
    val apiaryCount: LiveData<Int> = _apiaryCount

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var selectedPhotoUri: Uri? = null

    init {
        loadUserData()
        loadApiaryCount()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val userData = doc.toObject(User::class.java)
                userData?.let {
                    _user.value = it
                }
            }
            .addOnFailureListener { exception ->
                _errorMessage.value = "Failed to load user data: ${exception.message}"
            }
    }

    private fun loadApiaryCount() {
        val uid = auth.currentUser?.uid ?: return

        realtimeDb.child("apiaries").orderByChild("ownerId").equalTo(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val count = snapshot.childrenCount.toInt()
                    _apiaryCount.value = count
                }

                override fun onCancelled(error: DatabaseError) {
                    _apiaryCount.value = 0
                }
            })
    }

    fun setSelectedPhotoUri(uri: Uri) {
        selectedPhotoUri = uri
    }

    fun updateProfile(firstName: String, lastName: String, farm: String, location: String) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        // If photo is selected, upload it first
        if (selectedPhotoUri != null) {
            uploadProfilePhoto(uid, firstName, lastName, farm, location)
        } else {
            updateUserData(uid, firstName, lastName, farm, location, _user.value?.photoUrl ?: "")
        }
    }

    private fun uploadProfilePhoto(
        uid: String,
        firstName: String,
        lastName: String,
        farm: String,
        location: String
    ) {
        val photoRef = storage.child("profile_photos/$uid.jpg")

        selectedPhotoUri?.let { uri ->
            photoRef.putFile(uri)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updateUserData(uid, firstName, lastName, farm, location, downloadUri.toString())
                    }.addOnFailureListener { exception ->
                        _isLoading.value = false
                        _errorMessage.value = "Failed to get photo URL: ${exception.message}"
                    }
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    _errorMessage.value = "Failed to upload photo: ${exception.message}"
                }
        }
    }

    private fun updateUserData(
        uid: String,
        firstName: String,
        lastName: String,
        farm: String,
        location: String,
        photoUrl: String
    ) {
        val updates = hashMapOf<String, Any>(
            "firstName" to firstName,
            "lastName" to lastName,
            "farm" to farm,
            "location" to location,
            "photoUrl" to photoUrl
        )

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                _isLoading.value = false
                _updateSuccess.value = true
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                _errorMessage.value = "Failed to update profile: ${exception.message}"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}