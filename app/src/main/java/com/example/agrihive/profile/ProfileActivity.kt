package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
        setupBottomNavigationHighlight()
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

        // Bottom Navigation Click Listeners
        val footerNav = binding.root.findViewById<android.view.View>(R.id.footerNav)
        
        // Home
        footerNav?.findViewById<LinearLayout>(R.id.navHomeContainer)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        // History - placeholder
        footerNav?.findViewById<LinearLayout>(R.id.navHistoryContainer)?.setOnClickListener {
            // History functionality coming soon
        }

        // Camera - placeholder
        footerNav?.findViewById<LinearLayout>(R.id.navScanContainer)?.setOnClickListener {
            // Camera functionality coming soon
        }

        // Settings
        footerNav?.findViewById<LinearLayout>(R.id.navSettingsContainer)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun setupBottomNavigationHighlight() {
        // Highlight Profile when on Profile page
        val footerNav = binding.root.findViewById<android.view.View>(R.id.footerNav)
        
        val navProfile = footerNav?.findViewById<ImageView>(R.id.navProfile)
        val tvProfile = footerNav?.findViewById<TextView>(R.id.tvProfile)
        
        navProfile?.isSelected = true
        tvProfile?.setTextColor(getColor(R.color.honey_dark))
        
        // Reset others
        footerNav?.findViewById<ImageView>(R.id.navHome)?.isSelected = false
        footerNav?.findViewById<ImageView>(R.id.navHistory)?.isSelected = false
        footerNav?.findViewById<ImageView>(R.id.navSettings)?.isSelected = false
        
        footerNav?.findViewById<TextView>(R.id.tvHome)?.setTextColor(getColor(android.R.color.black))
        footerNav?.findViewById<TextView>(R.id.tvHistory)?.setTextColor(getColor(android.R.color.black))
        footerNav?.findViewById<TextView>(R.id.tvScan)?.setTextColor(getColor(android.R.color.black))
        footerNav?.findViewById<TextView>(R.id.tvSettings)?.setTextColor(getColor(android.R.color.black))
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
