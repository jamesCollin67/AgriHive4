package com.example.agrihive.log

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
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
        
        // Ensure Repository is initialized in case we came here from a notification or deep link
        ActivityLogRepository.init(applicationContext)
        
        binding = ActivityActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ActivityLogViewModel.getInstance()
        adapter = ActivityLogAdapter()
        
        setupRecyclerView()
        setupObservers()

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Trigger load
        viewModel.loadFromFirebase()
    }

    private fun setupRecyclerView() {
        binding.rvActivityLog.apply {
            layoutManager = LinearLayoutManager(this@ActivityLogActivity)
            adapter = this@ActivityLogActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.activityLogs.observe(this) { logs ->
            adapter.submitList(logs)
            binding.emptyState.visibility = if (logs.isNullOrEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // You could add a progress bar here if one exists in your XML
        }
    }
}
