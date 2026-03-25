package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityEditProfileBinding
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.settings.SettingsActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        binding.btnSave.text = "Edit Profile"
        binding.btnSave.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(this) { user ->
            user?.let {
                binding.tvDisplayName.text = "${it.firstName} ${it.lastName}"
                binding.tvDisplayEmail.text = it.email
                
                binding.etFullName.setText("${it.firstName} ${it.lastName}")
                binding.etEmail.setText(it.email)
                // Phone is not in the User model based on previous code search, but if it's there:
                // binding.etPhone.setText(it.phone)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserData()
    }
}
