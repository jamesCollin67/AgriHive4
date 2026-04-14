package com.example.agrihive.notification

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Activity to display all app notifications
 */
class NotificationActivity : AppCompatActivity() {

    private lateinit var repository: NotificationRepository
    private lateinit var adapter: NotificationAdapter
    
    private lateinit var rvNotifications: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var tvUnreadCount: TextView
    private lateinit var tvBadgeCount: TextView
    private lateinit var unreadBadge: View
    private lateinit var backButton: View
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        repository = NotificationRepository(this)

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadNotifications()
    }

    private fun initViews() {
        rvNotifications = findViewById(R.id.rvNotifications)
        emptyState = findViewById(R.id.emptyState)
        tvUnreadCount = findViewById(R.id.tvUnreadCount)
        tvBadgeCount = findViewById(R.id.tvBadgeCount)
        unreadBadge = findViewById(R.id.unreadBadge)
        backButton = findViewById(R.id.flBell)
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        
        // Bell icon is decorative in the Alerts screen, no action needed
        // backButton.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            notifications = emptyList(),
            onItemClick = { notification ->
                repository.markAsRead(notification.id)
                loadNotifications()
                // Handle navigation based on notification type if needed
            },
            onDeleteClick = { notification ->
                repository.deleteNotification(notification.id)
                loadNotifications()
            }
        )
        
        rvNotifications.layoutManager = LinearLayoutManager(this)
        rvNotifications.adapter = adapter
    }

    private fun loadNotifications() {
        val notifications = repository.getAllNotifications()
        adapter.updateNotifications(notifications)
        
        val unreadCount = repository.getUnreadCount()
        
        // Update UI components
        emptyState.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        tvUnreadCount.text = if (unreadCount == 1) "1 unread" else "$unreadCount unread"
        
        if (unreadCount > 0) {
            tvBadgeCount.text = unreadCount.toString()
            tvBadgeCount.visibility = View.VISIBLE
            unreadBadge.visibility = View.VISIBLE
        } else {
            tvBadgeCount.visibility = View.GONE
            unreadBadge.visibility = View.GONE
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_alerts
        // Swallow reselection — do nothing when tapping the already-active tab
        bottomNavigationView.setOnItemReselectedListener { /* no-op */ }
        bottomNavigationView.setOnItemSelectedListener { item ->
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
                R.id.nav_alerts -> true // already here
                R.id.nav_settings -> {
                    startActivity(
                        Intent(this, SettingsActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                    )
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
        // Ensure correct item is selected when returning to this activity
        bottomNavigationView.selectedItemId = R.id.nav_alerts
    }
}
