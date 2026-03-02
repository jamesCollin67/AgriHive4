package com.example.agrihive.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.device.DeviceControlActivity
import com.example.agrihive.log.ActivityLogActivity
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.profile.ProfileActivity
import com.example.agrihive.sensorsubscription.SensorSubscriptionActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager
    private lateinit var auth: FirebaseAuth

    // UI elements
    private lateinit var btnBack: ImageView
    private lateinit var footerNav: View

    private lateinit var rowLogout: LinearLayout
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchCloudSync: SwitchCompat

    private lateinit var rowDeviceControls: LinearLayout
    private lateinit var rowActivityLog: LinearLayout
    private lateinit var rowChangePassword: LinearLayout
    private lateinit var rowSubscription: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_page)

        // Initialize session manager and auth
        sessionManager = UserSessionManager(this)
        auth = FirebaseAuth.getInstance()

        // Initialize repository with app context for local storage
        com.example.agrihive.log.ActivityLogRepository.init(applicationContext)

        initViews()
        setupUI()
        setupObservers()
        setupBottomNavigationHighlight()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        footerNav = findViewById(R.id.footerNav)

        rowLogout = findViewById(R.id.rowLogout)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchCloudSync = findViewById(R.id.switchCloudSync)

        rowDeviceControls = findViewById(R.id.rowDeviceControls)
        rowActivityLog = findViewById(R.id.rowActivityLog)
        rowChangePassword = findViewById(R.id.rowChangePassword)
        rowSubscription = findViewById(R.id.rowSubscription)

        // Setup SwipeRefreshLayout with yellow color
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setColorSchemeResources(R.color.yellow_primary)
        swipeRefresh.setProgressBackgroundColorSchemeColor(
            resources.getColor(android.R.color.white, theme)
        )
        swipeRefresh.setOnRefreshListener {
            // Refresh settings data
            swipeRefresh.postDelayed({
                swipeRefresh.isRefreshing = false
            }, 1000) // Stop after 1 second
        }
    }

    private fun setupUI() {
        // Back button
        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Bottom navigation click listeners
        // Home
        footerNav.findViewById<View>(R.id.navHomeContainer)?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // History - placeholder
        footerNav.findViewById<View>(R.id.navSearchContainer)?.setOnClickListener {
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
        footerNav.findViewById<View>(R.id.navScanContainer)?.setOnClickListener {
            // Camera functionality coming soon
        }

        // Profile
        footerNav.findViewById<View>(R.id.navProfileContainer)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Settings - Already in Settings page
        footerNav.findViewById<View>(R.id.navHistoryContainer)?.setOnClickListener {
            // Already in Settings - do nothing
        }

        // Logout
        rowLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Switches
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }

        switchCloudSync.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCloudSync(isChecked)
        }

        // Other rows
        rowDeviceControls.setOnClickListener {
            val intent = Intent(this, DeviceControlActivity::class.java)
            startActivity(intent)
        }

        rowActivityLog.setOnClickListener {
            val intent = Intent(this, ActivityLogActivity::class.java)
            startActivity(intent)
        }

        rowChangePassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        rowSubscription.setOnClickListener {
            val intent = Intent(this, SensorSubscriptionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigationHighlight() {
        // Highlight Settings when on Settings page
        val navSettings = footerNav.findViewById<ImageView>(R.id.navHistory)
        val navHome = footerNav.findViewById<ImageView>(R.id.navHome)
        val navSearch = footerNav.findViewById<ImageView>(R.id.navSearch)
        val navProfile = footerNav.findViewById<ImageView>(R.id.navProfile)
        
        val tvSettings = footerNav.findViewById<TextView>(R.id.tvHistory)
        val tvHome = footerNav.findViewById<TextView>(R.id.tvHome)
        val tvSearch = footerNav.findViewById<TextView>(R.id.tvSearch)
        val tvProfile = footerNav.findViewById<TextView>(R.id.tvProfile)
        
        val activeColor = getColor(R.color.nav_active)
        val inactiveColor = getColor(R.color.nav_inactive)
        
        // Set Settings as selected (yellow)
        navSettings?.setColorFilter(activeColor)
        tvSettings?.setTextColor(activeColor)
        
        // Reset others to inactive (gray)
        navHome?.setColorFilter(inactiveColor)
        navSearch?.setColorFilter(inactiveColor)
        navProfile?.setColorFilter(inactiveColor)
        
        tvHome?.setTextColor(inactiveColor)
        tvSearch?.setTextColor(inactiveColor)
        tvProfile?.setTextColor(inactiveColor)
    }

    private fun setupObservers() {
        viewModel.notificationsEnabled.observe(this) { enabled ->
            if (switchNotifications.isChecked != enabled)
                switchNotifications.isChecked = enabled
        }

        viewModel.cloudSyncEnabled.observe(this) { enabled ->
            if (switchCloudSync.isChecked != enabled)
                switchCloudSync.isChecked = enabled
        }
    }

    private fun showLogoutDialog() {
        val prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { dialog, _ ->
                dialog.dismiss()

                // Sign out from Firebase Auth
                auth.signOut()

                // Clear user session data from SharedPreferences
                sessionManager.clearUserData()

                // NOTE: We do NOT clear ActivityLog local storage here
                // because user wants logs to persist after logout
                // When same user logs back in, their logs will be loaded from local storage

                // Reset subscription dialog flag so it shows on next login
                prefs.edit().putBoolean("subscription_dialog_shown_this_login", false).apply()

                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

                // Navigate to Login page and clear activity stack
                val intent = Intent(this, com.example.agrihive.login.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
