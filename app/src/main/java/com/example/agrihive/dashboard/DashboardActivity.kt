package com.example.agrihive.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.addapiary.AddApiaryActivity
import com.example.agrihive.profile.ProfileActivity
import com.example.agrihive.settings.SettingsActivity

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)

        // Add Apiary button
        findViewById<android.widget.Button>(R.id.btnAddApiary).setOnClickListener {
            viewModel.onAddApiaryClicked()
        }

        // Profile icon click
        findViewById<ImageView>(R.id.ivProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Settings icon click
        findViewById<ImageView>(R.id.ivSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Observe navigation
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

    private fun showSubscriptionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.activity_subscription_card, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<TextView>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
