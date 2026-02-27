package com.example.agrihive.editprofile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.agrihive.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class EditProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference
    // Explicitly use the storage bucket from google-services.json
    private val storage: StorageReference = FirebaseStorage.getInstance("gs://agrihive-3dc60.firebasestorage.app").getReference()

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
        val uid = auth.currentUser?.uid
        
        if (uid == null) {
            _errorMessage.value = "User not authenticated. Please login again."
            return
        }

        _isLoading.value = true
        
        firestore.collection("users").document(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                _isLoading.value = false
                if (doc.exists()) {
                    val userData = doc.toObject(User::class.java)
                    userData?.let {
                        _user.value = it
                    } ?: run {
                        _errorMessage.value = "Failed to parse user data"
                    }
                } else {
                    // Create a new user document if it doesn't exist
                    createUserDocument(uid)
                }
            }
            .addOnFailureListener { exception ->
                _isLoading.value = false
                // Check for specific error types
                val errorMsg = when {
                    exception.message?.contains("OFFLINE") == true -> 
                        "No internet connection. Please check your network."
                    exception.message?.contains("document does not exist") == true -> 
                        "User profile not found. Please try again."
                    else -> "Failed to load user data: ${exception.message}"
                }
                _errorMessage.value = errorMsg
            }
    }

    private fun createUserDocument(uid: String) {
        val newUser = User(
            uid = uid,
            email = auth.currentUser?.email ?: "",
            firstName = "",
            lastName = "",
            farm = "",
            location = "",
            photoUrl = ""
        )
        firestore.collection("users").document(uid)
            .set(newUser)
            .addOnSuccessListener {
                _user.value = newUser
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to create user profile"
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
        val uid = auth.currentUser?.uid
        
        if (uid == null) {
            _errorMessage.value = "User not authenticated. Please login again."
            return
        }
        
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
        // Use a unique filename to avoid conflicts
        val timestamp = System.currentTimeMillis()
        val photoRef = storage.child("profile_photos").child("${uid}_$timestamp.jpg")

        selectedPhotoUri?.let { uri ->
            // Check if URI is valid
            if (uri.toString().isEmpty()) {
                _isLoading.value = false
                _errorMessage.value = "Invalid photo selected. Please try again."
                return
            }
            
            photoRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // Photo uploaded successfully, now get download URL
                    photoRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            updateUserData(uid, firstName, lastName, farm, location, downloadUri.toString())
                        }
                        .addOnFailureListener { exception ->
                            _isLoading.value = false
                            _errorMessage.value = "Failed to get photo URL: ${exception.message}"
                        }
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    // Provide more helpful error message
                    val errorMsg = when {
                        exception.message?.contains("object does not exist") == true ->
                            "Failed to upload photo: Storage bucket not configured. Please contact support."
                        exception.message?.contains("permission") == true ->
                            "Failed to upload photo: Permission denied. Check storage rules."
                        exception.message?.contains("not found") == true ->
                            "Failed to upload photo: File not found. Please select a different photo."
                        exception.message?.contains("no such host") == true ->
                            "Failed to upload photo: Network error. Please check your connection."
                        else -> "Failed to upload photo: ${exception.message}"
                    }
                    _errorMessage.value = errorMsg
                }
        } ?: run {
            _isLoading.value = false
            _errorMessage.value = "No photo selected"
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