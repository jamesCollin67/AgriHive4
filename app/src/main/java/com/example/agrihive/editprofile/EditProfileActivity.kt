package com.example.agrihive.editprofile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityEditProfileBinding
import com.example.agrihive.profile.ProfileActivity

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: EditProfileViewModel by viewModels()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                binding.profilePic.setImageURI(it)
                viewModel.setSelectedPhotoUri(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Observe user data
        viewModel.user.observe(this) { user ->
            binding.etFirstName.setText(user.firstName)
            binding.etLastName.setText(user.lastName)
            binding.etEmail.setText(user.email)
            binding.etFarm.setText(user.farm)
            binding.etLocation.setText(user.location)

            // Load profile photo if available
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(user.photoUrl)
                    .placeholder(R.drawable.avatar_placeholder)
                    .circleCrop()
                    .into(binding.profilePic)
            }
        }

        // Observe apiary count
        viewModel.apiaryCount.observe(this) { count ->
            binding.etApiaryOwned.setText("$count")
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnUpdateProfile.isEnabled = !isLoading
            binding.btnUpdateProfile.text = if (isLoading) "UPDATING..." else "UPDATE PROFILE"
        }

        // Observe update success
        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                navigateToProfile()
            }
        }

        // Observe error message
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupClickListeners() {
        // Back button - navigate to Profile
        binding.btnBack.setOnClickListener {
            navigateToProfile()
        }

        // Change photo button
        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        // Update profile button
        binding.btnUpdateProfile.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val farm = binding.etFarm.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()

            if (validateInputs(firstName, lastName)) {
                viewModel.updateProfile(firstName, lastName, farm, location)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun validateInputs(firstName: String, lastName: String): Boolean {
        if (firstName.isEmpty()) {
            binding.etFirstName.error = "First name is required"
            binding.etFirstName.requestFocus()
            return false
        }

        if (lastName.isEmpty()) {
            binding.etLastName.error = "Last name is required"
            binding.etLastName.requestFocus()
            return false
        }

        return true
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    // Handle hardware back button press
    override fun onBackPressed() {
        navigateToProfile()
        super.onBackPressed()
    }
}