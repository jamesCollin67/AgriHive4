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
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.databinding.ActivityProfileBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.hivestreams.SendReportActivity
import com.example.agrihive.camera.CameraActivity
import com.example.agrihive.settings.SettingsActivity
import com.example.agrihive.utils.NetworkAlertDialog
import com.example.agrihive.utils.NetworkUtils

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager

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
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize session manager and load local data first for immediate display
        sessionManager = UserSessionManager(this)
        loadLocalUserData()

        setupObservers()
        setupClickListeners()
        setupBottomNavigationHighlight()
        
        // Check for updated data from EditProfileActivity
        checkForUpdatedData(intent)
    }

    // Load user data from SharedPreferences for immediate display
    private fun loadLocalUserData() {
        if (sessionManager.hasUserData()) {
            val firstName = sessionManager.getFirstName()
            val lastName = sessionManager.getLastName()
            val farm = sessionManager.getFarm()
            val location = sessionManager.getLocation()
            val email = sessionManager.getEmail()
            
            // Display local data immediately
            binding.tvUserName.text = "$firstName $lastName"
            binding.tvEmail.text = if (email.isNotEmpty()) email else "Not set"
            binding.tvFarm.text = if (farm.isNotEmpty()) farm else "Not set"
            binding.tvLocation.text = if (location.isNotEmpty()) location else "Not set"
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Check for internet connection and show message if offline
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetMessage()
        }
        
        // Refresh user data when returning to this page
        viewModel.refreshUserData()
    }

    private fun showNoInternetMessage() {
        // Show a "No internet connection" text on the profile
        binding.tvEmail.text = "No internet connection"
        binding.tvEmail.setTextColor(getColor(R.color.status_error))
        
        // Show toast as additional notification
        Toast.makeText(this, "No internet connection. Showing cached data.", Toast.LENGTH_LONG).show()
        
        // Show dialog for user to try again
        NetworkAlertDialog.show(
            context = this,
            onTryAgain = {
                // Check internet again
                if (NetworkUtils.isNetworkAvailable(this)) {
                    // Refresh data
                    viewModel.refreshUserData()
                } else {
                    showNoInternetMessage()
                }
            },
            onCancel = {
                // Do nothing, stay with cached data
            }
        )
    }

    // Handle updated data from EditProfileActivity
    private fun checkForUpdatedData(intent: Intent?) {
        intent?.let {
            val updatedFirstName = it.getStringExtra("updated_firstName")
            val updatedLastName = it.getStringExtra("updated_lastName")
            val updatedFarm = it.getStringExtra("updated_farm")
            val updatedLocation = it.getStringExtra("updated_location")
            
            if (updatedFirstName != null && updatedLastName != null) {
                // Update UI immediately with the new data
                binding.tvUserName.text = "$updatedFirstName $updatedLastName"
                if (updatedFarm != null) {
                    binding.tvFarm.text = if(updatedFarm.isNotEmpty()) updatedFarm else "Not set"
                }
                if (updatedLocation != null) {
                    binding.tvLocation.text = if(updatedLocation.isNotEmpty()) updatedLocation else "Not set"
                }
            }
        }
    }

    // Handle new intent when returning from EditProfileActivity
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkForUpdatedData(intent)
    }

    private fun setupObservers() {
        // Observe loading state
        viewModel.user.observe(this) { user ->
            binding.tvUserName.text = "${user.firstName} ${user.lastName}"
            binding.tvEmail.text = user.email
            binding.tvFarm.text = if(user.farm.isNotEmpty()) user.farm else "Not set"
            binding.tvLocation.text = if(user.location.isNotEmpty()) user.location else "Not set"
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(this).load(user.photoUrl).placeholder(R.drawable.avatar_placeholder).circleCrop().into(binding.ivProfilePic)
            }
        }
        viewModel.apiaryCount.observe(this) { count ->
            binding.tvApiaryCount.text = count.toString()
        }

        observeNavigation()
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener { viewModel.editClicked() }
        binding.toolbar.setNavigationOnClickListener { viewModel.backClicked() }
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
