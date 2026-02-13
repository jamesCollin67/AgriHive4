package com.example.agrihive.dashboard

import ApiaryAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.addapiary.AddApiaryActivity
import android.widget.ImageView
import com.example.agrihive.profile.ProfileActivity
import com.example.agrihive.settings.SettingsActivity
import com.example.agrihive.hivestreams.HiveStreamsActivity

class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var apiaryAdapter: ApiaryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_page)

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerApiaries)

        apiaryAdapter = ApiaryAdapter { apiary ->

            val intent = Intent(this, HiveStreamsActivity::class.java)
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
        // Bottom Navigation Click Listeners
        findViewById<ImageView>(R.id.navHome).setOnClickListener {
            // Already in Dashboard (Home)
        }

        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<ImageView>(R.id.navSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }


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