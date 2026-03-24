package com.example.agrihive.hivestreams

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityApiaryDataStreamsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HiveStreamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiaryDataStreamsBinding
    private val viewModel: HiveStreamsViewModel by viewModels()

    companion object {
        const val EXTRA_APIARY_ID = "APIARY_ID"
        const val EXTRA_APIARY_NAME = "APIARY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiaryDataStreamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apiaryId = intent.getStringExtra(EXTRA_APIARY_ID) ?: return finish()
        val apiaryName = intent.getStringExtra(EXTRA_APIARY_NAME) ?: "Hive"

        setupUI(apiaryName)
        observeViewModel()

        viewModel.startListening(apiaryId)
    }

    private fun setupUI(apiaryName: String) {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvApiaryName.text = apiaryName
        
        // Tab Selection Logic
        binding.btnLiveReadings.setOnClickListener {
            showLiveReadings()
        }

        binding.btn_analytics.setOnClickListener {
            showWeightAnalytics()
        }
    }

    private fun showLiveReadings() {
        binding.layoutLiveReadings.visibility = View.VISIBLE
        binding.layoutWeightAnalytics.visibility = View.GONE
        
        // Update Tab Styles
        binding.btnLiveReadings.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent)) // Custom if exists, or just use #1AFFFFFF
        binding.btnLiveReadings.setTextColor(ContextCompat.getColor(this, R.color.login_accent)) // #F4B400
        
        binding.btn_analytics.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        binding.btn_analytics.setTextColor(ContextCompat.getColor(this, R.color.text_secondary)) // #9CAF9F
    }

    private fun showWeightAnalytics() {
        binding.layoutLiveReadings.visibility = View.GONE
        binding.layoutWeightAnalytics.visibility = View.VISIBLE
        
        // Update Tab Styles
        binding.btn_analytics.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent))
        binding.btn_analytics.setTextColor(ContextCompat.getColor(this, R.color.login_accent))
        
        binding.btnLiveReadings.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        binding.btnLiveReadings.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.contentLayout.alpha = if (isLoading) 0.5f else 1.0f
        }

        viewModel.apiaryData.observe(this) { apiary ->
            apiary?.let {
                // Live Status
                binding.viewLiveStatus.setBackgroundResource(
                    if (it.isConnected) R.drawable.bg_green_circle else R.drawable.bg_red_circle
                )

                // Sensor Values
                binding.tvTempValue.text = "%.1f".format(it.temperature)
                binding.tvHumidityValue.text = "%.1f".format(it.humidity)
                binding.tvMoistureValue.text = "%.1f".format(it.moisture)
                binding.tvWeightValue.text = "%.1f".format(it.weight)

                // Hive Info
                binding.tvLocationValue.text = it.location
                binding.tvNodeIdValue.text = "Node ID: ${it.nodeId}"
                
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                binding.tvLastUpdatedValue.text = "Last updated: ${sdf.format(Date(it.lastUpdate))}"

                // Status Labels
                updateStatusLabels(it)
            } ?: run {
                resetValues()
            }
        }

        viewModel.weightAnalytics.observe(this) { analytics ->
            analytics?.let {
                binding.tvAnalyticsCurrentWeight.text = "%.1fkg".format(it.currentWeight)
                binding.tvTrendStatus.text = it.trendStatus
                binding.tvHarvestStatus.text = it.harvestStatus
                
                binding.tvTotalGain.text = (if (it.totalGain >= 0) "+" else "") + "%.1fkg".format(it.totalGain)
                binding.tvAvgGain.text = "%.2fkg/day".format(it.avgDailyGain)
                binding.tvPeakWeight.text = "%.1fkg".format(it.peakWeight)
                
                // Color trend status
                if (it.trendStatus.equals("Growing", ignoreCase = true)) {
                    binding.tvTrendStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                } else {
                    binding.tvTrendStatus.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatusLabels(apiary: com.example.agrihive.addapiary.Apiary) {
        binding.tvTempStatus.text = "Normal"
        binding.tvHumidityStatus.text = "Normal"
        binding.tvWeightStatus.text = "Normal"
        
        binding.tvMoistureStatus.text = "Warning"
        binding.cardMoisture.strokeWidth = 2
        // Using common color resource if available, fallback to hardcoded if necessary
        binding.cardMoisture.strokeColor = ContextCompat.getColor(this, android.R.color.holo_orange_light)
    }

    private fun resetValues() {
        binding.tvTempValue.text = "0.0"
        binding.tvHumidityValue.text = "0.0"
        binding.tvMoistureValue.text = "0.0"
        binding.tvWeightValue.text = "0.0"
    }

    override fun onDestroy() {
        super.onDestroy()
        val apiaryId = intent.getStringExtra(EXTRA_APIARY_ID)
        apiaryId?.let { viewModel.stopListening(it) }
    }
}
