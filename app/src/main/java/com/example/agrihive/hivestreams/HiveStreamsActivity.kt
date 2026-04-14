package com.example.agrihive.hivestreams

import android.content.Intent
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
    private var apiaryId: String = ""

    companion object {
        const val EXTRA_APIARY_ID = "APIARY_ID"
        const val EXTRA_APIARY_NAME = "APIARY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiaryDataStreamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiaryId = intent.getStringExtra(EXTRA_APIARY_ID) ?: return finish()
        val apiaryName = intent.getStringExtra(EXTRA_APIARY_NAME) ?: "Hive"

        setupUI(apiaryName)
        observeViewModel()

        viewModel.startListening(apiaryId)
    }

    private fun setupUI(apiaryName: String) {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvApiaryName.text = apiaryName
        
        // Fix: Route to AI Scanner UI
        binding.btnCamera.setOnClickListener {
            startActivity(
                Intent(this, AiScannerActivity::class.java).apply {
                    putExtra(AiScannerActivity.EXTRA_HIVE_NAME, apiaryName)
                    putExtra(AiScannerActivity.EXTRA_APIARY_ID, apiaryId)
                }
            )
        }
        
        // Tab Selection Logic
        binding.btnLiveReadings.setOnClickListener {
            showLiveReadings()
        }

        binding.btnAnalytics.setOnClickListener {
            showWeightAnalytics()
        }
    }

    private fun showLiveReadings() {
        binding.layoutLiveReadings.visibility = View.VISIBLE
        binding.layoutWeightAnalytics.visibility = View.GONE
        
        // Update Tab Styles
        binding.btnLiveReadings.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent))
        binding.btnLiveReadings.setTextColor(ContextCompat.getColor(this, R.color.login_accent))
        
        binding.btnAnalytics.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        binding.btnAnalytics.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun showWeightAnalytics() {
        binding.layoutLiveReadings.visibility = View.GONE
        binding.layoutWeightAnalytics.visibility = View.VISIBLE
        
        // Update Tab Styles
        binding.btnAnalytics.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent))
        binding.btnAnalytics.setTextColor(ContextCompat.getColor(this, R.color.login_accent))
        
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

                // Sensor Values — show "--" without units when node is disconnected
                if (it.isConnected) {
                    binding.tvTempValue.text = "%.1f".format(it.temperature)
                    binding.tvHumidityValue.text = "%.1f".format(it.humidity)
                    binding.tvMoistureValue.text = "%.1f".format(it.moisture)
                    binding.tvWeightValue.text = "%.1f".format(it.weight)
                } else {
                    binding.tvTempValue.text = "--"
                    binding.tvHumidityValue.text = "--"
                    binding.tvMoistureValue.text = "--"
                    binding.tvWeightValue.text = "--"
                }

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
        // Temperature: optimal range 34–36°C for honeybees
        val tempStatus = when {
            apiary.temperature <= 0 -> "No Data"
            apiary.temperature < 34.0 -> "Too Cold"
            apiary.temperature > 36.0 -> "Too Hot"
            else -> "Normal"
        }
        val tempColor = when (tempStatus) {
            "Normal" -> ContextCompat.getColor(this, android.R.color.holo_green_light)
            "No Data" -> ContextCompat.getColor(this, android.R.color.darker_gray)
            else -> ContextCompat.getColor(this, android.R.color.holo_red_light)
        }
        binding.tvTempStatus.text = tempStatus
        binding.tvTempStatus.setTextColor(tempColor)

        // Humidity: optimal 50–80%
        val humidityStatus = when {
            apiary.humidity <= 0 -> "No Data"
            apiary.humidity < 50.0 -> "Too Dry"
            apiary.humidity > 80.0 -> "Too Humid"
            else -> "Normal"
        }
        val humidityColor = when (humidityStatus) {
            "Normal" -> ContextCompat.getColor(this, android.R.color.holo_green_light)
            "No Data" -> ContextCompat.getColor(this, android.R.color.darker_gray)
            else -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
        }
        binding.tvHumidityStatus.text = humidityStatus
        binding.tvHumidityStatus.setTextColor(humidityColor)

        // Moisture: honey ready at ≤18%, warning above
        val moistureStatus = when {
            apiary.moisture <= 0 -> "No Data"
            apiary.moisture <= 18.0 -> "Harvest Ready"
            apiary.moisture <= 22.0 -> "Normal"
            else -> "Too Wet"
        }
        val moistureColor = when (moistureStatus) {
            "Harvest Ready" -> ContextCompat.getColor(this, android.R.color.holo_green_light)
            "Normal" -> ContextCompat.getColor(this, android.R.color.holo_blue_light)
            "No Data" -> ContextCompat.getColor(this, android.R.color.darker_gray)
            else -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
        }
        binding.tvMoistureStatus.text = moistureStatus
        binding.tvMoistureStatus.setTextColor(moistureColor)
        // Only show warning stroke when actually too wet
        if (moistureStatus == "Too Wet") {
            binding.cardMoisture.strokeWidth = 2
            binding.cardMoisture.strokeColor = ContextCompat.getColor(this, android.R.color.holo_orange_light)
        } else {
            binding.cardMoisture.strokeWidth = 0
        }

        // Weight: flag if suspiciously low (possible theft/swarm)
        val weightStatus = when {
            apiary.weight <= 0 -> "No Data"
            apiary.weight < 5.0 -> "Low"
            else -> "Normal"
        }
        binding.tvWeightStatus.text = weightStatus
        binding.tvWeightStatus.setTextColor(
            if (weightStatus == "Normal") ContextCompat.getColor(this, android.R.color.holo_green_light)
            else ContextCompat.getColor(this, android.R.color.darker_gray)
        )
    }

    private fun resetValues() {
        binding.tvTempValue.text = "--"
        binding.tvHumidityValue.text = "--"
        binding.tvMoistureValue.text = "--"
        binding.tvWeightValue.text = "--"
        // Clear status labels when no data
        binding.tvTempStatus.text = "No Data"
        binding.tvHumidityStatus.text = "No Data"
        binding.tvMoistureStatus.text = "No Data"
        binding.tvWeightStatus.text = "No Data"
        binding.tvTempStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        binding.tvHumidityStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        binding.tvMoistureStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        binding.tvWeightStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        binding.cardMoisture.strokeWidth = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        apiaryId.let { viewModel.stopListening(it) }
    }
}
