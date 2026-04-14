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

        // Real probabilities from TFLite model
        val probLabels = intent.getStringArrayExtra(EXTRA_PROB_LABELS)
        val probScores = intent.getIntegerArrayListExtra(EXTRA_PROB_SCORES)
        val realProbabilities: List<Pair<String, Int>> = if (!probLabels.isNullOrEmpty() && !probScores.isNullOrEmpty()) {
            probLabels.zip(probScores)
        } else {
            emptyList()
        }

        binding.tvHiveContext.text = hiveName

        viewModel.setScanContext(hiveName, apiaryId, imageUriString)
        viewModel.setResult(disease, healthScore)

        setupUI(imageUriString)
        setupConfidenceList(realProbabilities)
        observeViewModel()
        setupClicks()
    }

    private fun setupUI(imageUriString: String?) {
        imageUriString?.let {
            Glide.with(this)
                .load(it.toUri())
                .into(binding.ivScanPreview)
        }
    }

    private fun setupConfidenceList(probabilities: List<Pair<String, Int>>) {
        val confidenceData = probabilities.mapIndexed { index, (label, score) ->
            val color = when (index) {
                0 -> if (score < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
                1 -> "#FF9800".toColorInt()
                2 -> "#9C27B0".toColorInt()
                3 -> "#1565C0".toColorInt()
                else -> "#607D8B".toColorInt()
            }
            // Clean up label for display
            val displayLabel = label
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            ConfidenceResult(displayLabel, score, color)
        }

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

            val (color, scoreLabel) = when {
                score >= 80 -> "#66BB6A" to "Healthy Hive"
                score >= 60 -> "#FF9800" to "Mild Concern"
                score >= 40 -> "#FF9800" to "Needs Attention"
                else        -> "#EF5350" to "Critical — Act Now"
            }
            binding.tvHealthScore.setTextColor(android.graphics.Color.parseColor(color))
            binding.tvDiseaseName.setTextColor(android.graphics.Color.parseColor(color))
            binding.pbHealthScore.progressTintList =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))

            // Show plain-language score label below the progress bar
            binding.tvHealthScoreLabel?.text = scoreLabel
            binding.tvHealthScoreLabel?.setTextColor(android.graphics.Color.parseColor(color))
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

        // Show "Saved ✓" feedback on the button while saving
        viewModel.isSaving.observe(this) { saving ->
            binding.btnSaveResult.text = if (saving) "Saving..." else "Save Result"
            binding.btnSaveResult.isEnabled = !saving
        }
    }

    companion object {
        const val EXTRA_DISEASE = "extra_disease"
        const val EXTRA_HEALTH_SCORE = "extra_health_score"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_HIVE_NAME = "extra_hive_name"
        const val EXTRA_APIARY_ID = "extra_apiary_id"
        const val EXTRA_PROB_LABELS = "extra_prob_labels"
        const val EXTRA_PROB_SCORES = "extra_prob_scores"
    }
}
