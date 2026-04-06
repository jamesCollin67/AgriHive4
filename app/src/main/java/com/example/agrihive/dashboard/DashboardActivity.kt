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

        // Setup click listeners
        setupClickListeners()
        
        // Setup bottom navigation
        setupBottomNavigation()

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
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apiaries -> true
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

        viewModel.harvestReadyCount.observe(this) { count ->
            binding.tvHarvestCount.text = count.toString()
        }

        // Observe unread notifications for bottom navigation badge
        viewModel.unreadNotificationsCount.observe(this) { count ->
            val badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_alerts)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
                badge.backgroundColor = ContextCompat.getColor(this, R.color.status_error)
                badge.badgeTextColor = ContextCompat.getColor(this, android.R.color.white)
            } else {
                badge.isVisible = false
            }
        }

        // Observe apiaries list and toggle empty state visibility
        viewModel.apiaries.observe(this) { apiaries ->
            adapter.submitList(apiaries)
            
            if (apiaries.isEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvApiaries.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvApiaries.visibility = View.VISIBLE
            }
        }

        // Observe errors from Firebase
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}
