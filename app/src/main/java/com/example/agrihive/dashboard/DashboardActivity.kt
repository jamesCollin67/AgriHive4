package com.example.agrihive.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agrihive.R
import com.example.agrihive.addapiary.AddApiaryActivity
import com.example.agrihive.camera.CameraActivity
import com.example.agrihive.databinding.ActivityDashboardBinding
import com.example.agrihive.hivestreams.HiveStreamsActivity
import com.example.agrihive.notification.NotificationActivity
import com.example.agrihive.settings.SettingsActivity

/**
 * Dashboard Activity - Main screen showing apiary list and stats
 * MVVM Architecture with Firebase backend
 * Design matches the provided image with dark theme and gold accents
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var adapter: ApiaryAdapter
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize binding
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        setupRecyclerView()

        // Set today's date in header
        val sdf = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        binding.tvDate.text = sdf.format(java.util.Date())

        // Setup click listeners
        setupClickListeners()
        
        // Setup bottom navigation
        setupBottomNavigation()

        // Swipe to refresh
        binding.swipeRefresh.setColorSchemeColors(
            android.graphics.Color.parseColor("#F4B400")
        )
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(
            android.graphics.Color.parseColor("#1A3329")
        )
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadApiaries()
        }
        viewModel.isLoading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }

        // Observe ViewModel
        observeViewModel()

        // Load data from Firebase
        viewModel.loadApiaries()
    }

    private fun setupRecyclerView() {
        adapter = ApiaryAdapter(
            onApiaryClick = { apiary ->
                val intent = Intent(this, HiveStreamsActivity::class.java).apply {
                    putExtra("APIARY_ID", apiary.id)
                    putExtra("APIARY_NAME", apiary.name)
                }
                startActivity(intent)
            },
            onApiaryLongClick = { apiary ->
                // Long press → Edit or Delete apiary
                val intent = Intent(this, com.example.agrihive.addapiary.EditApiaryActivity::class.java).apply {
                    putExtra(com.example.agrihive.addapiary.EditApiaryActivity.EXTRA_APIARY_ID, apiary.id)
                    putExtra(com.example.agrihive.addapiary.EditApiaryActivity.EXTRA_APIARY_NAME, apiary.name)
                    putExtra(com.example.agrihive.addapiary.EditApiaryActivity.EXTRA_APIARY_LOCATION, apiary.location)
                }
                startActivity(intent)
            }
        )
        binding.rvApiaries.layoutManager = LinearLayoutManager(this)
        binding.rvApiaries.adapter = adapter
    }

    private fun setupClickListeners() {
        // Add Apiary button (Empty State)
        binding.btnAddApiary.setOnClickListener {
            startActivity(Intent(this, AddApiaryActivity::class.java))
        }

        // Add Apiary button (Top FAB)
        binding.btnAddTop.setOnClickListener {
            startActivity(Intent(this, AddApiaryActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_apiaries
        // Swallow reselection — do nothing when tapping the already-active tab
        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apiaries -> true // already here
                R.id.nav_alerts -> {
                    startActivity(
                        Intent(this, NotificationActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    true
                }
                R.id.nav_settings -> {
                    startActivity(
                        Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        // Observe user name for greeting (e.g., "Hello, Tfgf")
        viewModel.userName.observe(this) { name ->
            binding.tvGreeting.text = "Hello, $name"
        }

        // Observe Statistics for the header cards
        viewModel.totalApiaries.observe(this) { count ->
            binding.tvTotalCount.text = count.toString()
        }

        viewModel.onlineCount.observe(this) { count ->
            binding.tvOnlineCount.text = count.toString()
        }

        viewModel.alertsCount.observe(this) { count ->
            binding.tvAlertsCount.text = count.toString()
        }

        // Tapping the Alerts card navigates to the first hive with an active alert
        binding.cardStatAlerts.setOnClickListener {
            val alertedApiary = viewModel.apiaries.value?.firstOrNull { apiary ->
                apiary.moisture > 18.0 ||
                (apiary.temperature > 0 && (apiary.temperature < 34.0 || apiary.temperature > 36.0)) ||
                apiary.weight in 0.1..4.9
            }
            if (alertedApiary != null) {
                startActivity(Intent(this, com.example.agrihive.hivestreams.HiveStreamsActivity::class.java).apply {
                    putExtra("APIARY_ID", alertedApiary.id)
                    putExtra("APIARY_NAME", alertedApiary.name)
                })
            }
        }

        viewModel.harvestReadyCount.observe(this) { count ->
            binding.tvHarvestCount.text = count.toString()
        }

        // Observe unread notifications for bottom navigation badge
        viewModel.unreadNotificationsCount.observe(this) { count ->
            val badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_alerts)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
                badge.backgroundColor = ContextCompat.getColor(this, R.color.honey_primary)
                badge.badgeTextColor = ContextCompat.getColor(this, android.R.color.black)
            } else {
                badge.isVisible = false
            }
        }

        // Observe apiaries list and toggle empty state visibility
        viewModel.apiaries.observe(this) { apiaries ->
            adapter.submitList(apiaries)
            if (apiaries.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.swipeRefresh.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.swipeRefresh.visibility = View.VISIBLE
            }
        }

        // Observe errors from Firebase
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // Observe subscription expiry — show dialog with option to dismiss
        viewModel.subscriptionExpired.observe(this) { expired ->
            if (expired == true) {
                viewModel.clearSubscriptionExpired()
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("⚠️ Subscription Expired")
                    .setMessage("Your subscription has expired. Renew now to continue monitoring your hives with real-time sensor data.")
                    .setPositiveButton("Renew Now") { _, _ ->
                        startActivity(
                            Intent(this, com.example.agrihive.sensorsubscription.SensorSubscriptionActivity::class.java)
                        )
                    }
                    .setNegativeButton("Remind Me Later", null)
                    .setCancelable(true)
                    .show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure correct item is selected when returning to this activity
        binding.bottomNavigation.selectedItemId = R.id.nav_apiaries
        // Update notification count badge in case some were read in the NotificationActivity
        viewModel.updateNotificationCount()
    }
}
