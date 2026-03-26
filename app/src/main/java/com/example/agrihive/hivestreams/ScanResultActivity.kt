package com.example.agrihive.hivestreams

import android.content.res.ColorStateList
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
        setupConfidenceList(disease, healthScore)
        setupClicks()
        observeViewModel()
    }

    private fun setupUI(imageUriString: String?) {
        imageUriString?.let {
            Glide.with(this)
                .load(it.toUri())
                .into(binding.ivScanPreview)
        }
        
        binding.tvBrightness.text = "62.4%"
        binding.tvRedChannel.text = "51.2%"
        binding.tvGreenChannel.text = "48.7%"
        binding.tvBlueChannel.text = "44.1%"
        binding.tvVariance.text = "3.24%"
        binding.tvEdgeDensity.text = "28.5%"
    }

    private fun setupConfidenceList(disease: String?, score: Int?) {
        val displayScore = score ?: 0
        val confidenceData = mutableListOf<ConfidenceResult>()
        
        val mainColor = if (displayScore < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
        confidenceData.add(ConfidenceResult(viewModel.diseaseName.value ?: "Detected", displayScore, mainColor))
        
        confidenceData.add(ConfidenceResult("Other Bee Pathogens", (displayScore * 0.4).toInt(), "#FF9800".toColorInt()))
        confidenceData.add(ConfidenceResult("Environmental Stress", (displayScore * 0.2).toInt(), "#9C27B0".toColorInt()))
        
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
        }
        
        viewModel.healthScore.observe(this) {
            binding.tvHealthScore.text = "$it/100"
            binding.pbHealthScore.progress = it
            
            val color = if (it < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
            binding.tvHealthScore.setTextColor(color)
            binding.tvDiseaseName.setTextColor(color)
        }

        viewModel.symptoms.observe(this) {
            binding.tvSymptomsList.text = it
        }

        viewModel.treatments.observe(this) {
            binding.tvActionsList.text = it
        }

        viewModel.riskLevel.observe(this) {
            binding.tvRiskLevel.text = it
        }

        viewModel.riskColor.observe(this) { colorStr ->
            try {
                val colorInt = colorStr.toColorInt()
                binding.tvRiskLevel.setTextColor(colorInt)
                
                val alphaColor = Color.argb(
                    40, 
                    Color.red(colorInt), 
                    Color.green(colorInt), 
                    Color.blue(colorInt)
                )
                binding.tvRiskLevel.backgroundTintList = ColorStateList.valueOf(alphaColor)
            } catch (e: Exception) {}
        }

        viewModel.navigateToSaved.observe(this) { shouldOpen ->
            if (shouldOpen) {
                viewModel.doneNavigateToSaved()
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_DISEASE = "extra_disease"
        const val EXTRA_HEALTH_SCORE = "extra_health_score"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
