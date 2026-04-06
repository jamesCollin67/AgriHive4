package com.example.agrihive.hivestreams

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.agrihive.databinding.ActivitySavedDiagnosisDetailBinding

/**
 * Read-only diagnosis view for a row from [SavedTreatmentsActivity].
 */
class SavedDiagnosisDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedDiagnosisDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedDiagnosisDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val disease = intent.getStringExtra(EXTRA_DISEASE_NAME).orEmpty().ifBlank { "Unknown" }
        val hive = intent.getStringExtra(EXTRA_HIVE_NAME).orEmpty().ifBlank { "Hive" }
        val score = intent.getIntExtra(EXTRA_HEALTH_SCORE, 0).coerceIn(0, 100)
        var symptoms = intent.getStringExtra(EXTRA_SYMPTOMS).orEmpty()
        var treatments = intent.getStringExtra(EXTRA_TREATMENTS).orEmpty()

        if (symptoms.isBlank()) {
            symptoms = DiagnosisCopy.symptomsFor(disease)
        }
        if (treatments.isBlank()) {
            treatments = DiagnosisCopy.treatmentsFor(disease)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.tvHiveName.text = hive
        binding.tvDiseaseName.text = disease
        binding.tvSymptoms.text = symptoms
        binding.tvTreatments.text = treatments
        binding.tvHealthScore.text = "$score/100"
        binding.pbHealthScore.progress = score

        val (severityLabel, chipBg, accent) = DiagnosisCopy.severityBadge(score)
        binding.tvSeverity.text = severityLabel
        binding.tvSeverity.setTextColor(accent.toColorInt())
        val chipBgInt = chipBg.toColorInt()
        binding.tvSeverity.backgroundTintList = ColorStateList.valueOf(
            Color.argb(50, Color.red(chipBgInt), Color.green(chipBgInt), Color.blue(chipBgInt))
        )

        val scoreColor = if (score < 50) "#EF5350".toColorInt() else "#66BB6A".toColorInt()
        binding.tvDiseaseName.setTextColor(scoreColor)
        binding.tvHealthScore.setTextColor(scoreColor)
        binding.pbHealthScore.progressTintList = ColorStateList.valueOf(scoreColor)

        binding.btnViewSavedList.setOnClickListener {
            startActivity(Intent(this, SavedTreatmentsActivity::class.java))
            finish()
        }
    }

    companion object {
        const val EXTRA_DISEASE_NAME = "extra_disease_name"
        const val EXTRA_HIVE_NAME = "extra_hive_name"
        const val EXTRA_HEALTH_SCORE = "extra_health_score"
        const val EXTRA_SYMPTOMS = "extra_symptoms"
        const val EXTRA_TREATMENTS = "extra_treatments"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
}
