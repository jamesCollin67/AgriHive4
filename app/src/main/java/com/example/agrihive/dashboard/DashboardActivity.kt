package com.example.agrihive.dashboard

import ApiaryAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.addapiary.AddApiaryActivity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.View
import com.example.agrihive.profile.ProfileActivity
import com.example.agrihive.settings.SettingsActivity

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var apiaryAdapter: ApiaryAdapter
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)

        prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)

        // Show subscription dialog on login (when coming from login screen) - only once per session
        val fromLogin = intent.getBooleanExtra("FROM_LOGIN", false)
        val subscriptionShownThisSession = prefs.getBoolean("subscription_dialog_shown_this_login", false)
        
        if (fromLogin && !subscriptionShownThisSession) {
            showSubscriptionDialog()
            prefs.edit().putBoolean("subscription_dialog_shown_this_login", true).apply()
        }

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerApiaries)

        apiaryAdapter = ApiaryAdapter { apiary ->

            val intent = Intent(this, com.example.agrihive.hivestreams.HiveStreamsActivity::class.java)
            intent.putExtra("APIARY_ID", apiary.id)
            intent.putExtra("APIARY_NAME", apiary.name)
            startActivity(intent)
        }

        recyclerView.apply {
            adapter = apiaryAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }


        // Observe apiary list
        viewModel.apiaryList.observe(this) { list ->

            apiaryAdapter.submitList(list)

            findViewById<TextView>(R.id.tvActiveHives).text =
                "${list.count { it.isActive }} active hives"

            findViewById<TextView>(R.id.tvEmpty).isVisible = list.isEmpty()
        }


        // Add Apiary button
        findViewById<android.widget.Button>(R.id.btnAddApiary).setOnClickListener {
            viewModel.onAddApiaryClicked()
        }

        // Setup bottom navigation with highlighting
        setupBottomNavigation()

        // Navigation to Add Apiary
        viewModel.navigateToAddApiary.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, AddApiaryActivity::class.java))
                viewModel.doneNavigatingAddApiary()
            }
        }

        // Subscription popup - show every time user logs in
        viewModel.checkSubscription()
        viewModel.showSubscription.observe(this) { show ->
            if (show) {
                showSubscriptionDialog()
                viewModel.doneShowingSubscription()
            }
        }
    }

    private fun setupBottomNavigation() {
        // Get the bottom navigation layout
        val footerNav = findViewById<View>(R.id.footerNav)
        
        // Get ImageViews for icon color change
        val navHome = footerNav?.findViewById<ImageView>(R.id.navHome)
        val navSearch = footerNav?.findViewById<ImageView>(R.id.navSearch)
        val navProfile = footerNav?.findViewById<ImageView>(R.id.navProfile)
        val navSettings = footerNav?.findViewById<ImageView>(R.id.navHistory)
        
        // Get text views for color change
        val tvHome = footerNav?.findViewById<TextView>(R.id.tvHome)
        val tvSearch = footerNav?.findViewById<TextView>(R.id.tvSearch)
        val tvProfile = footerNav?.findViewById<TextView>(R.id.tvProfile)
        val tvSettings = footerNav?.findViewById<TextView>(R.id.tvHistory)
        
        // Colors
        val activeColor = getColor(R.color.nav_active)
        val inactiveColor = getColor(R.color.nav_inactive)
        
        // Helper function to update selected state
        fun updateSelection(selectedId: Int) {
            // Reset all to inactive (gray)
            navHome?.setColorFilter(inactiveColor)
            navSearch?.setColorFilter(inactiveColor)
            navProfile?.setColorFilter(inactiveColor)
            navSettings?.setColorFilter(inactiveColor)
            
            tvHome?.setTextColor(inactiveColor)
            tvSearch?.setTextColor(inactiveColor)
            tvProfile?.setTextColor(inactiveColor)
            tvSettings?.setTextColor(inactiveColor)
            
            // Highlight selected one (yellow)
            when (selectedId) {
                R.id.navHomeContainer -> {
                    navHome?.setColorFilter(activeColor)
                    tvHome?.setTextColor(activeColor)
                }
                R.id.navSearchContainer -> {
                    navSearch?.setColorFilter(activeColor)
                    tvSearch?.setTextColor(activeColor)
                }
                R.id.navProfileContainer -> {
                    navProfile?.setColorFilter(activeColor)
                    tvProfile?.setTextColor(activeColor)
                }
                R.id.navHistoryContainer -> {
                    navSettings?.setColorFilter(activeColor)
                    tvSettings?.setTextColor(activeColor)
                }
            }
        }
        
        // Set Home as selected by default
        updateSelection(R.id.navHomeContainer)
        
        // Home - Already in Dashboard
        footerNav?.findViewById<View>(R.id.navHomeContainer)?.setOnClickListener {
            updateSelection(R.id.navHomeContainer)
        }
        
        // Search/History navigation
        footerNav?.findViewById<View>(R.id.navSearchContainer)?.setOnClickListener {
            updateSelection(R.id.navSearchContainer)
            Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Scan/Camera button
        footerNav?.findViewById<View>(R.id.navScanContainer)?.setOnClickListener {
            Toast.makeText(this, "Scan feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Profile navigation
        footerNav?.findViewById<View>(R.id.navProfileContainer)?.setOnClickListener {
            updateSelection(R.id.navProfileContainer)
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        
        // Settings navigation
        footerNav?.findViewById<View>(R.id.navHistoryContainer)?.setOnClickListener {
            updateSelection(R.id.navHistoryContainer)
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun showSubscriptionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_subscription_card, null)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.apply {
            setBackgroundDrawable(null)
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0.7f)
            
            val params = attributes
            params?.apply {
                width = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                height = android.view.ViewGroup.LayoutParams.MATCH_PARENT
                gravity = android.view.Gravity.CENTER
            }
            attributes = params
            
            decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }

        dialogView.findViewById<TextView>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
