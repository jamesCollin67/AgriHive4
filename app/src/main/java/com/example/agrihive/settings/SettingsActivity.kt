package com.example.agrihive.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.profile.ProfileActivity

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    // UI elements
    private lateinit var btnBack: ImageView
    private lateinit var footerNav: LinearLayout
    private lateinit var navHome: ImageView
    private lateinit var navProfile: ImageView

    private lateinit var rowLogout: LinearLayout
    private lateinit var switchNotifications: Switch
    private lateinit var switchCloudSync: Switch

    private lateinit var rowDeviceControls: LinearLayout
    private lateinit var rowActivityLog: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_page)

        initViews()
        setupUI()
        setupObservers()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        footerNav = findViewById(R.id.footerNav) as LinearLayout
        navHome = footerNav.findViewById(R.id.navHome)
        navProfile = footerNav.findViewById(R.id.navProfile)

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

        // Bottom nav
        navHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
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
