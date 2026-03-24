package com.example.agrihive.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.data.UserSessionManager
import com.example.agrihive.device.DeviceControlActivity
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.hivestreams.SendReportActivity
import com.example.agrihive.log.ActivityLogActivity
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.notification.NotificationActivity
import com.example.agrihive.sensorsubscription.SensorSubscriptionActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sessionManager = UserSessionManager(this)
        auth = FirebaseAuth.getInstance()

        setupNavigation()
        setupBottomNavigation()

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_notifications).setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_sync).setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCloudSync(isChecked)
        }

        setupObservers()
    }

    private fun setupNavigation() {
        // Activity Log
        findViewById<View>(R.id.btn_activity_log).setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }

        // Saved Treatments
        findViewById<View>(R.id.btn_saved_treatments).setOnClickListener {
            // Check if activity exists in manifest or project, if not show toast or route to placeholder
            try {
                val intent = Intent(this, Class.forName("com.example.agrihive.settings.SavedTreatmentsActivity"))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Saved Treatments coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        // Device Controls
        findViewById<View>(R.id.btn_device_controls).setOnClickListener {
            startActivity(Intent(this, DeviceControlActivity::class.java))
        }

        // Subscription
        findViewById<View>(R.id.btn_subscription).setOnClickListener {
            startActivity(Intent(this, SensorSubscriptionActivity::class.java))
        }

        // Report an Issue
        findViewById<View>(R.id.btn_report_issue).setOnClickListener {
            startActivity(Intent(this, SendReportActivity::class.java))
        }

        // Edit Profile
        findViewById<View>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Change Password
        findViewById<View>(R.id.btn_change_password).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Logout
        findViewById<View>(R.id.btn_logout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_settings
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apiaries -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                else -> false
            }
        }
    }

    private fun setupObservers() {
        viewModel.notificationsEnabled.observe(this) { enabled ->
            findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_notifications).isChecked = enabled
        }
        viewModel.cloudSyncEnabled.observe(this) { enabled ->
            findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_sync).isChecked = enabled
        }
    }

    private fun showLogoutDialog() {
        val prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { dialog, _ ->
                dialog.dismiss()
                auth.signOut()
                sessionManager.clearUserData()
                ActivityLogViewModel.getInstance().resetUserState()
                prefs.edit().putBoolean("subscription_dialog_shown_this_login", false).apply()

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
