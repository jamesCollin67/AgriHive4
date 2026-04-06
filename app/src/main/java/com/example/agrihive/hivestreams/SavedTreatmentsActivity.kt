package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.agrihive.databinding.ActivitySavedTreatmentsBinding

class SavedTreatmentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedTreatmentsBinding
    private val viewModel: SavedTreatmentsViewModel by viewModels()
    private val adapter = SavedTreatmentsAdapter { item ->
        startActivity(
            Intent(this, SavedDiagnosisDetailActivity::class.java).apply {
                putExtra(SavedDiagnosisDetailActivity.EXTRA_DISEASE_NAME, item.diseaseName)
                putExtra(SavedDiagnosisDetailActivity.EXTRA_HIVE_NAME, item.hiveName)
                putExtra(SavedDiagnosisDetailActivity.EXTRA_HEALTH_SCORE, item.healthScore)
                putExtra(SavedDiagnosisDetailActivity.EXTRA_SYMPTOMS, item.symptoms)
                putExtra(SavedDiagnosisDetailActivity.EXTRA_TREATMENTS, item.description)
                putExtra(SavedDiagnosisDetailActivity.EXTRA_IMAGE_URL, item.imageUrl)
            }
        )
    }

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
        
        binding.cardTotalScans.setOnClickListener {
            viewModel.filterAll()
        }
        
        binding.cardIssuesFound.setOnClickListener {
            viewModel.filterIssues()
        }
        
        binding.cardHealthy.setOnClickListener {
            viewModel.filterHealthy()
        }
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
