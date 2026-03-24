package com.example.agrihive.log

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivityActivityLogBinding

/**
 * Activity Log Page — Timestamped chronological feed of user and automated actions.
 */
class ActivityLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivityLogBinding
    private lateinit var viewModel: ActivityLogViewModel
    private lateinit var adapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ActivityLogViewModel::class.java]
        adapter = ActivityLogAdapter()
        
        binding.rvActivityLog.layoutManager = LinearLayoutManager(this)
        binding.rvActivityLog.adapter = adapter

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        viewModel.activityLogs.observe(this) { logs ->
            adapter.submitList(logs)
            // Note: The static UI has hardcoded entries. In a production app, 
            // the RecyclerView would replace those or be used instead.
        }

        viewModel.loadFromFirebase()
    }
}
