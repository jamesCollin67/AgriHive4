package com.example.agrihive.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
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
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var sessionManager: UserSessionManager
    private lateinit var auth: FirebaseAuth
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sessionManager = UserSessionManager(this)
        auth = FirebaseAuth.getInstance()

        setupNavigation()
        setupBottomNavigation()
        
        // Initial load from session manager
        displayCachedProfile()
        // Refresh from server
        loadUserProfile()

        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_notifications).setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleNotifications(isChecked)
        }
        findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_sync).setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleCloudSync(isChecked)
        }

        setupObservers()
    }

    private fun displayCachedProfile() {
        if (sessionManager.hasUserData()) {
            val name = "${sessionManager.getFirstName()} ${sessionManager.getLastName()}".trim()
            val email = sessionManager.getEmail()
            
            findViewById<TextView>(R.id.tv_settings_user_name).text = if (name.isNotEmpty()) name else "User"
            findViewById<TextView>(R.id.tv_settings_user_email).text = if (email.isNotEmpty()) email else "email@example.com"
        }
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val farm = document.getString("farm") ?: ""
                    val location = document.getString("location") ?: ""
                    val apiaries = document.getLong("apiaries")?.toInt() ?: 0
                    
                    findViewById<TextView>(R.id.tv_settings_user_name).text = "$firstName $lastName".trim()
                    findViewById<TextView>(R.id.tv_settings_user_email).text = email
                    
                    // Sync session manager
                    sessionManager.saveUserData(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        farm = farm,
                        location = location,
                        apiaries = apiaries,
                        uid = uid
                    )
                }
            }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.card_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_activity_log).setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }

        findViewById<View>(R.id.btn_saved_treatments).setOnClickListener {
            startActivity(Intent(this, com.example.agrihive.hivestreams.SavedTreatmentsActivity::class.java))
        }

        findViewById<View>(R.id.btn_device_controls).setOnClickListener {
            startActivity(Intent(this, DeviceControlActivity::class.java))
        }

        findViewById<View>(R.id.btn_subscription).setOnClickListener {
            startActivity(Intent(this, SensorSubscriptionActivity::class.java))
        }

        findViewById<View>(R.id.btn_report_issue).setOnClickListener {
            startActivity(Intent(this, SendReportActivity::class.java))
        }

        findViewById<View>(R.id.btn_edit_profile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        findViewById<View>(R.id.btn_change_password).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<View>(R.id.btn_logout).setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_settings
        bottomNav.setOnItemSelectedListener { item ->
            // BUG FIX: Prevent re-navigation if already on the selected UI
            if (item.itemId == bottomNav.selectedItemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.nav_apiaries -> {
                    startActivity(
                        Intent(this, DashboardActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    finish()
                    true
                }
                R.id.nav_alerts -> {
                    startActivity(
                        Intent(this, NotificationActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
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

    override fun onResume() {
        super.onResume()
        // Refresh profile info when returning from Edit Profile
        displayCachedProfile()
        loadUserProfile()
        // Ensure correct item is selected when returning to this activity
        bottomNav.selectedItemId = R.id.nav_settings
    }
}
