package com.example.agrihive.notification

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R

/**
 * Activity to display all app notifications
 */
class NotificationActivity : AppCompatActivity() {

    private lateinit var repository: NotificationRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private lateinit var emptyView: TextView
    private lateinit var backButton: ImageView
    private lateinit var clearAllButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        repository = NotificationRepository(this)

        initViews()
        setupRecyclerView()
        loadNotifications()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.notifications_recycler_view)
        emptyView = findViewById(R.id.empty_notifications_text)
        backButton = findViewById(R.id.back_button)
        clearAllButton = findViewById(R.id.clear_all_button)

        backButton.setOnClickListener {
            finish()
        }

        clearAllButton.setOnClickListener {
            repository.clearAll()
            loadNotifications()
            Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            notifications = emptyList(),
            onItemClick = { notification ->
                repository.markAsRead(notification.id)
                loadNotifications()
            },
            onDeleteClick = { notification ->
                repository.deleteNotification(notification.id)
                loadNotifications()
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadNotifications() {
        val notifications = repository.getAllNotifications()
        
        if (notifications.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            clearAllButton.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            clearAllButton.visibility = View.VISIBLE
            adapter.updateNotifications(notifications)
        }
    }
}
