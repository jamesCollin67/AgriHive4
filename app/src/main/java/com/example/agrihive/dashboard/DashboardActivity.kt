package com.example.agrihive.dashboard

import ApiaryAdapter
import android.content.Intent
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
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.agrihive.profile.ProfileActivity
import com.example.agrihive.settings.SettingsActivity

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var apiaryAdapter: ApiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)

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

        // Subscription popup
        viewModel.checkSubscription()
        viewModel.showSubscription.observe(this) { show ->
            if (show) {
                showSubscriptionDialog()
                viewModel.doneShowingSubscription()
            }
        }
    }

    private fun setupBottomNavigation() {
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navHistory = findViewById<ImageView>(R.id.navHistory)
        val navScan = findViewById<LinearLayout>(R.id.navScanContainer)
        val navProfile = findViewById<ImageView>(R.id.navProfile)
        val navSettings = findViewById<ImageView>(R.id.navSettings)

        // Get text views for color change
        val tvHome = findViewById<TextView>(R.id.tvHome)
        val tvHistory = findViewById<TextView>(R.id.tvHistory)
        val tvScan = findViewById<TextView>(R.id.tvScan)
        val tvProfile = findViewById<TextView>(R.id.tvProfile)
        val tvSettings = findViewById<TextView>(R.id.tvSettings)

        // Helper function to update selected state
        fun updateSelection(selectedId: Int) {
            // Reset all to unselected (gray)
            navHome.isSelected = false
            navHistory.isSelected = false
            navProfile.isSelected = false
            navSettings.isSelected = false

            tvHome.setTextColor(resources.getColor(android.R.color.black, theme))
            tvHistory.setTextColor(resources.getColor(android.R.color.black, theme))
            tvScan.setTextColor(resources.getColor(android.R.color.black, theme))
            tvProfile.setTextColor(resources.getColor(android.R.color.black, theme))
            tvSettings.setTextColor(resources.getColor(android.R.color.black, theme))

            // Highlight selected one (yellow)
            when (selectedId) {
                R.id.navHomeContainer -> {
                    navHome.isSelected = true
                    tvHome.setTextColor(resources.getColor(R.color.honey_dark, theme))
                }
                R.id.navHistoryContainer -> {
                    navHistory.isSelected = true
                    tvHistory.setTextColor(resources.getColor(R.color.honey_dark, theme))
                }
                R.id.navScanContainer -> {
                    tvScan.setTextColor(resources.getColor(R.color.honey_dark, theme))
                }
                R.id.navProfileContainer -> {
                    navProfile.isSelected = true
                    tvProfile.setTextColor(resources.getColor(R.color.honey_dark, theme))
                }
                R.id.navSettingsContainer -> {
                    navSettings.isSelected = true
                    tvSettings.setTextColor(resources.getColor(R.color.honey_dark, theme))
                }
            }
        }

        // Set Home as selected by default
        updateSelection(R.id.navHomeContainer)

        // Home - Already in Dashboard
        findViewById<LinearLayout>(R.id.navHomeContainer).setOnClickListener {
            updateSelection(R.id.navHomeContainer)
            // Already in Dashboard - do nothing
        }

        // History navigation
        findViewById<LinearLayout>(R.id.navHistoryContainer).setOnClickListener {
            updateSelection(R.id.navHistoryContainer)
            // TODO: Navigate to History screen
            Toast.makeText(this, "History coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Scan/Camera button
        navScan.setOnClickListener {
            updateSelection(R.id.navScanContainer)
            // TODO: Implement scan functionality
            Toast.makeText(this, "Scan feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Profile navigation
        findViewById<LinearLayout>(R.id.navProfileContainer).setOnClickListener {
            updateSelection(R.id.navProfileContainer)
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Settings navigation
        findViewById<LinearLayout>(R.id.navSettingsContainer).setOnClickListener {
            updateSelection(R.id.navSettingsContainer)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showSubscriptionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_subscription_card, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setGravity(android.view.Gravity.CENTER)

        dialogView.findViewById<TextView>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
