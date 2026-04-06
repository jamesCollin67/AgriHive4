package com.example.agrihive.hivestreams

import android.content.Intent
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
        val hiveName = intent.getStringExtra(EXTRA_HIVE_NAME)?.trim()?.takeIf { it.isNotEmpty() } ?: "Hive"
        val apiaryId = intent.getStringExtra(EXTRA_APIARY_ID)

        binding.tvHiveContext.text = hiveName

        viewModel.setScanContext(hiveName, apiaryId, imageUriString)
        viewModel.setResult(disease, healthScore)

        setupUI(imageUriString)
        setupConfidenceList(
            viewModel.diseaseName.value.orEmpty(),
            viewModel.healthScore.value ?: 0
        )
        setupClicks()
        observeViewModel()
    }

    private fun setupUI(imageUriString: String?) {
        imageUriString?.let {
            Glide.with(this)
                .load(it.toUri())
                .into(binding.ivScanPreview)
        }
    }

    private fun setupConfidenceList(diseaseLabel: String, healthScoreValue: Int) {
        val confidenceData = mutableListOf<ConfidenceResult>()

        val mainColor = if (healthScoreValue < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
        confidenceData.add(
            ConfidenceResult(
                diseaseLabel.ifBlank { "Detected" },
                healthScoreValue,
                mainColor
            )
        )

        val secondary = (healthScoreValue * 0.4).toInt().coerceIn(0, 100)
        val tertiary = (healthScoreValue * 0.2).toInt().coerceIn(0, 100)
        confidenceData.add(ConfidenceResult("Other Bee Pathogens", secondary, "#FF9800".toColorInt()))
        confidenceData.add(ConfidenceResult("Environmental Stress", tertiary, "#9C27B0".toColorInt()))

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
        binding.tvViewSavedTreatments.setOnClickListener {
            startActivity(Intent(this, SavedTreatmentsActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.diseaseName.observe(this) {
            binding.tvDiseaseName.text = it
        }

        viewModel.healthScore.observe(this) { score ->
            binding.tvHealthScore.text = "$score/100"
            binding.pbHealthScore.progress = score

            val color = if (score < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
            binding.tvHealthScore.setTextColor(color)
            binding.tvDiseaseName.setTextColor(color)
            binding.pbHealthScore.progressTintList = ColorStateList.valueOf(color)
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
            } catch (_: Exception) {
            }
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
        const val EXTRA_HIVE_NAME = "extra_hive_name"
        const val EXTRA_APIARY_ID = "extra_apiary_id"
    }
}
