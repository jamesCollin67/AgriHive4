package com.example.agrihive.hivestreams

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.agrihive.databinding.ActivityScanResultBinding

class ScanResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanResultBinding
    private val viewModel: ScanResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val disease = intent.getStringExtra(EXTRA_DISEASE)
        val healthScore = intent.getIntExtra(EXTRA_HEALTH_SCORE, -1).takeIf { it >= 0 }
        val imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI)

        viewModel.setResult(disease, healthScore)

        setupUI(imageUriString)
        setupConfidenceList()
        setupClicks()
        observeViewModel()
    }

    private fun setupUI(imageUriString: String?) {
        imageUriString?.let {
            Glide.with(this)
                .load(it.toUri())
                .into(binding.ivScanPreview)
        }
        
        // Populate static analysis features as seen in Image 1
        binding.tvBrightness.text = "50.0%"
        binding.tvRedChannel.text = "45.0%"
        binding.tvGreenChannel.text = "42.0%"
        binding.tvBlueChannel.text = "38.0%"
        binding.tvVariance.text = "5.00%"
        binding.tvEdgeDensity.text = "35.0%"
    }

    private fun setupConfidenceList() {
        val confidenceData = listOf(
            ConfidenceResult("American Foulbrood", 17, "#EF5350".toColorInt()),
            ConfidenceResult("Healthy Colony", 17, "#66BB6A".toColorInt()),
            ConfidenceResult("Varroa Mite Infestation", 14, "#EF5350".toColorInt()),
            ConfidenceResult("European Foulbrood", 14, "#FF9800".toColorInt()),
            ConfidenceResult("Chalkbrood Disease", 13, "#FF9800".toColorInt()),
            ConfidenceResult("Nosema Infection", 13, "#9C27B0".toColorInt()),
            ConfidenceResult("Sacbrood Virus", 12, "#FBC02D".toColorInt())
        )
        
        binding.rvConfidence.apply {
            layoutManager = LinearLayoutManager(this@ScanResultActivity)
            adapter = ConfidenceAdapter(confidenceData)
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSaveResult.setOnClickListener { viewModel.onSaveClicked() }
        binding.btnScanAgain.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        viewModel.diseaseName.observe(this) { 
            binding.tvDiseaseName.text = it
            // Change color to red if it's AFB as per Image 2
            if (it == "American Foulbrood") {
                binding.tvDiseaseName.setTextColor("#EF5350".toColorInt())
            }
        }
        viewModel.healthScore.observe(this) {
            binding.tvHealthScore.text = "$it/100"
            binding.pbHealthScore.progress = it
        }

        viewModel.navigateToSaved.observe(this) { shouldOpen ->
            if (shouldOpen) {
                // Logic to navigate to saved treatments
                viewModel.doneNavigateToSaved()
            }
        }
    }

    companion object {
        const val EXTRA_DISEASE = "extra_disease"
        const val EXTRA_HEALTH_SCORE = "extra_health_score"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
