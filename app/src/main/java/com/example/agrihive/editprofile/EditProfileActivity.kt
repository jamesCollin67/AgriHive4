package com.example.agrihive.editprofile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.databinding.ActivityEditProfileBinding
import com.example.agrihive.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Edit Profile Page — Editable fields for name, email, apiaries, farm, location. (Spec)
 */
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var sessionManager: UserSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sessionManager = UserSessionManager(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        loadProfile()
        binding.btnUpdate.setOnClickListener { updateProfile() }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val fn = doc.getString("firstName") ?: sessionManager.getFirstName()
                val ln = doc.getString("lastName") ?: sessionManager.getLastName()
                val email = doc.getString("email") ?: sessionManager.getEmail()
                val farm = doc.getString("farm") ?: sessionManager.getFarm()
                val location = doc.getString("location") ?: sessionManager.getLocation()
                binding.tilName.editText?.setText("$fn $ln".trim().ifEmpty { "Full Name" })
                binding.tilEmail.editText?.setText(email)
                binding.tilFarmName.editText?.setText(farm)
                binding.tilLocation.editText?.setText(location)
            }
    }

    private fun updateProfile() {
        val fullName = binding.tilName.editText?.text?.toString()?.trim() ?: ""
        val parts = fullName.split(" ", limit = 2)
        val fn = parts.getOrElse(0) { "" }
        val ln = parts.getOrElse(1) { "" }
        val email = binding.tilEmail.editText?.text?.toString()?.trim() ?: ""
        val farm = binding.tilFarmName.editText?.text?.toString()?.trim() ?: ""
        val location = binding.tilLocation.editText?.text?.toString()?.trim() ?: ""
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).update(
            mapOf(
                "firstName" to fn,
                "lastName" to ln,
                "email" to email,
                "farm" to farm,
                "location" to location
            )
        ).addOnSuccessListener {
            sessionManager.saveUserData(firstName = fn, lastName = ln, email = email, farm = farm, location = location)
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, it.message ?: "Update failed", Toast.LENGTH_SHORT).show()
        }
    }
}
