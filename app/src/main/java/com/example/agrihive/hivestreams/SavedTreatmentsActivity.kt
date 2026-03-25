package com.example.agrihive.hivestreams

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agrihive.databinding.ActivitySavedTreatmentsBinding

class SavedTreatmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedTreatmentsBinding
    private val viewModel: SavedTreatmentsViewModel by viewModels()
    private val adapter = SavedTreatmentsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedTreatmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClicks()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvSavedTreatments.layoutManager = LinearLayoutManager(this)
        binding.rvSavedTreatments.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { viewModel.onBackClicked() }
    }

    private fun observeViewModel() {
        viewModel.treatments.observe(this) { list ->
            adapter.submitList(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.totalScans.observe(this) { binding.tvTotalScans.text = it.toString() }
        viewModel.issuesFound.observe(this) { binding.tvIssuesFound.text = it.toString() }
        viewModel.healthyCount.observe(this) { binding.tvHealthyCount.text = it.toString() }

        viewModel.navigateBack.observe(this) { shouldGoBack ->
            if (shouldGoBack) {
                finish()
                viewModel.doneBack()
            }
        }
    }
}
