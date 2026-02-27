package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

    override fun onResume() {
        super.onResume()
        // Refresh user data when returning to this page
        viewModel.refreshUserData()
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
        footerNav?.findViewById<View>(R.id.navHomeContainer)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        // History - placeholder
        footerNav?.findViewById<View>(R.id.navSearchContainer)?.setOnClickListener {
            // Highlight History (yellow) and show toast
            val navSearch = footerNav.findViewById<ImageView>(R.id.navSearch)
            val tvSearch = footerNav.findViewById<TextView>(R.id.tvSearch)
            val activeColor = getColor(R.color.nav_active)
            val inactiveColor = getColor(R.color.nav_inactive)
            
            // Reset all to inactive first
            footerNav.findViewById<ImageView>(R.id.navHome)?.setColorFilter(inactiveColor)
            footerNav.findViewById<ImageView>(R.id.navProfile)?.setColorFilter(inactiveColor)
            footerNav.findViewById<ImageView>(R.id.navHistory)?.setColorFilter(inactiveColor)
            footerNav.findViewById<TextView>(R.id.tvHome)?.setTextColor(inactiveColor)
            footerNav.findViewById<TextView>(R.id.tvProfile)?.setTextColor(inactiveColor)
            footerNav.findViewById<TextView>(R.id.tvHistory)?.setTextColor(inactiveColor)
            
            // Highlight History (yellow)
            navSearch?.setColorFilter(activeColor)
            tvSearch?.setTextColor(activeColor)
            
            Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Camera - placeholder
        footerNav?.findViewById<View>(R.id.navScanContainer)?.setOnClickListener {
            // Camera functionality coming soon
        }

        // Profile - Already in Profile page
        footerNav?.findViewById<View>(R.id.navProfileContainer)?.setOnClickListener {
            // Already in Profile - do nothing
        }

        // Settings
        footerNav?.findViewById<View>(R.id.navHistoryContainer)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun setupBottomNavigationHighlight() {
        // Highlight Profile when on Profile page
        val footerNav = binding.root.findViewById<android.view.View>(R.id.footerNav)
        
        val navProfile = footerNav?.findViewById<ImageView>(R.id.navProfile)
        val navHome = footerNav?.findViewById<ImageView>(R.id.navHome)
        val navSearch = footerNav?.findViewById<ImageView>(R.id.navSearch)
        val navSettings = footerNav?.findViewById<ImageView>(R.id.navHistory)
        
        val tvProfile = footerNav?.findViewById<TextView>(R.id.tvProfile)
        val tvHome = footerNav?.findViewById<TextView>(R.id.tvHome)
        val tvSearch = footerNav?.findViewById<TextView>(R.id.tvSearch)
        val tvSettings = footerNav?.findViewById<TextView>(R.id.tvHistory)
        
        val activeColor = getColor(R.color.nav_active)
        val inactiveColor = getColor(R.color.nav_inactive)
        
        // Set Profile as selected (yellow)
        navProfile?.setColorFilter(activeColor)
        tvProfile?.setTextColor(activeColor)
        
        // Reset others to inactive (gray)
        navHome?.setColorFilter(inactiveColor)
        navSearch?.setColorFilter(inactiveColor)
        navSettings?.setColorFilter(inactiveColor)
        
        tvHome?.setTextColor(inactiveColor)
        tvSearch?.setTextColor(inactiveColor)
        tvSettings?.setTextColor(inactiveColor)
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
