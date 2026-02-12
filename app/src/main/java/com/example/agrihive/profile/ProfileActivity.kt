package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityProfilePageBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.settings.SettingsActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfilePageBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfilePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.apiaryCount.observe(this) { count ->
            binding.tvApiary.text = "$count active hives"
        }

        viewModel.user.observe(this) { user ->
            binding.userName.text = "${user.firstName} ${user.lastName}"
            binding.tvEmail.text = user.email
            binding.tvApiary.text = "Loading..."
            binding.tvFarm.text = if(user.farm.isNotEmpty()) user.farm else "Not set"
            binding.tvLocation.text = if(user.location.isNotEmpty()) user.location else "Not set"
        }

        binding.btnEditProfile.setOnClickListener { viewModel.editClicked() }
        binding.btnBack.setOnClickListener { viewModel.backClicked() }

        observeNavigation()
    }

    private fun observeNavigation() {
        viewModel.goEdit.observe(this) {
            if(it) {
                startActivity(Intent(this, EditProfileActivity::class.java))
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
