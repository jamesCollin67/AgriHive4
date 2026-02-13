package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityProfilePageBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.settings.SettingsActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePageBinding
    private val viewModel: ProfileViewModel by viewModels()

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Refresh profile data
            viewModel.refreshUserData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Observe user info
        viewModel.user.observe(this) { user ->
            binding.userName.text = "${user.firstName} ${user.lastName}"
            binding.tvEmail.text = user.email
            binding.tvFarm.text = if(user.farm.isNotEmpty()) user.farm else "Not set"
            binding.tvLocation.text = if(user.location.isNotEmpty()) user.location else "Not set"

            // Load profile photo
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
            val apiaryText = if (count == 1) "1 apiary" else "$count apiaries"
            binding.tvApiary.text = apiaryText
        }

        observeNavigation()
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            viewModel.editClicked()
        }

        binding.btnBack.setOnClickListener {
            viewModel.backClicked()
        }

        binding.tvHomeLogo.setOnClickListener {
            viewModel.backClicked()
        }
    }

    private fun observeNavigation() {
        viewModel.goEdit.observe(this) {
            if(it) {
                val intent = Intent(this, EditProfileActivity::class.java)
                editProfileLauncher.launch(intent)
                viewModel.doneNav()
            }
        }

        viewModel.goDashboard.observe(this) {
            if(it) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                viewModel.doneNav()
            }
        }

        viewModel.goSettings.observe(this) {
            if(it) {
                startActivity(Intent(this, SettingsActivity::class.java))
                viewModel.doneNav()
            }
        }
    }
}