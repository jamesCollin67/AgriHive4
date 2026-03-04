package com.example.agrihive.log

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R

class ActivityLogActivity : AppCompatActivity() {

    private val viewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }
    private lateinit var adapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        // Initialize repository with app context for local storage
        ActivityLogRepository.init(applicationContext)

        setupViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Load fresh data when activity opens - this ensures each user sees only their own activity
        viewModel.loadFromFirebase()
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerActivityLog)
        adapter = ActivityLogAdapter()

        recyclerView.apply {
            this.adapter = this@ActivityLogActivity.adapter
            layoutManager = LinearLayoutManager(this@ActivityLogActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.activityLogs.observe(this) { logs ->
            adapter.submitList(logs)
            // Show/hide empty state
            findViewById<View>(R.id.tvEmpty)?.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<View>(R.id.progressBar)?.visibility = if (isLoading && adapter.itemCount == 0) View.VISIBLE else View.GONE
        }
    }
}
