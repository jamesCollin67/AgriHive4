package com.example.agrihive.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.profile.ProfileActivity

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    // UI elements
    private lateinit var btnBack: ImageView
    private lateinit var footerNav: LinearLayout

    private lateinit var rowLogout: LinearLayout
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchCloudSync: SwitchCompat

    private lateinit var rowDeviceControls: LinearLayout
    private lateinit var rowActivityLog: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_page)

        initViews()
        setupUI()
        setupObservers()
        setupBottomNavigationHighlight()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        footerNav = findViewById(R.id.footerNav) as LinearLayout

        rowLogout = findViewById(R.id.rowLogout)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchCloudSync = findViewById(R.id.switchCloudSync)

        rowDeviceControls = findViewById(R.id.rowDeviceControls)
        rowActivityLog = findViewById(R.id.rowActivityLog)
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
        footerNav.findViewById<LinearLayout>(R.id.navHomeContainer)?.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        // History - placeholder
        footerNav.findViewById<LinearLayout>(R.id.navHistoryContainer)?.setOnClickListener {
            // History functionality coming soon
        }

        // Camera - placeholder
        footerNav.findViewById<LinearLayout>(R.id.navScanContainer)?.setOnClickListener {
            // Camera functionality coming soon
        }

        // Profile
        footerNav.findViewById<LinearLayout>(R.id.navProfileContainer)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
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
            Toast.makeText(this, "Device Controls clicked", Toast.LENGTH_SHORT).show()
        }

        rowActivityLog.setOnClickListener {
            Toast.makeText(this, "Activity Log clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigationHighlight() {
        // Highlight Settings when on Settings page
        val navSettings = footerNav.findViewById<ImageView>(R.id.navSettings)
        val tvSettings = footerNav.findViewById<TextView>(R.id.tvSettings)
        
        navSettings?.isSelected = true
        tvSettings?.setTextColor(getColor(R.color.honey_dark))
        
        // Reset others
        footerNav.findViewById<ImageView>(R.id.navHome)?.isSelected = false
        footerNav.findViewById<ImageView>(R.id.navHistory)?.isSelected = false
        footerNav.findViewById<ImageView>(R.id.navProfile)?.isSelected = false
        
        footerNav.findViewById<TextView>(R.id.tvHome)?.setTextColor(getColor(android.R.color.black))
        footerNav.findViewById<TextView>(R.id.tvHistory)?.setTextColor(getColor(android.R.color.black))
        footerNav.findViewById<TextView>(R.id.tvScan)?.setTextColor(getColor(android.R.color.black))
        footerNav.findViewById<TextView>(R.id.tvProfile)?.setTextColor(getColor(android.R.color.black))
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
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                finishAffinity() // exit all activities
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
