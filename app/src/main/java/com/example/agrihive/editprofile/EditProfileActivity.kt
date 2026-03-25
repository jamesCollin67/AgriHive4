package com.example.agrihive.editprofile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Edit Profile Page — Editable fields for name, email, and phone.
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
        binding.btnSave.setOnClickListener { updateProfile() }
    }

    private fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fn = doc.getString("firstName") ?: ""
                    val ln = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    
                    binding.tvDisplayName.text = "$fn $ln".trim()
                    binding.tvDisplayEmail.text = email
                    
                    binding.etFullName.setText("$fn $ln".trim())
                    binding.etEmail.setText(email)
                    binding.etPhone.setText(phone)
                }
            }
    }

    private fun updateProfile() {
        val fullName = binding.etFullName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        
        val parts = fullName.split(" ", limit = 2)
        val fn = parts.getOrElse(0) { "" }
        val ln = parts.getOrElse(1) { "" }
        
        val uid = auth.currentUser?.uid ?: return

        val updates = mutableMapOf<String, Any>(
            "firstName" to fn,
            "lastName" to ln,
            "phone" to phone
        )

        firestore.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                sessionManager.saveUserData(firstName = fn, lastName = ln, email = binding.etEmail.text.toString())
                binding.tvDisplayName.text = fullName
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, it.message ?: "Update failed", Toast.LENGTH_SHORT).show()
            }
    }
}
