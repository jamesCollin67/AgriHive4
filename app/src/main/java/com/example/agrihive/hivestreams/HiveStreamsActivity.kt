package com.example.agrihive.hivestreams

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityApiaryDataStreamsBinding
import com.example.agrihive.notification.NotificationRepository
import com.example.agrihive.notification.NotificationType
import com.example.agrihive.weather.RainAlertNotification
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HiveStreamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiaryDataStreamsBinding
    private val viewModel: HiveStreamsViewModel by viewModels()
    private var apiaryId: String = ""
    private var apiaryName: String = "Hive"

    // Track last alerted state to avoid spamming notifications
    private var lastTempAlerted = false
    private var lastHumidityAlerted = false
    private var lastMoistureAlerted = false
    private var lastWeightAlerted = false

    companion object {
        const val EXTRA_APIARY_ID = "APIARY_ID"
        const val EXTRA_APIARY_NAME = "APIARY_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiaryDataStreamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiaryId   = intent.getStringExtra(EXTRA_APIARY_ID) ?: return finish()
        apiaryName = intent.getStringExtra(EXTRA_APIARY_NAME) ?: "Hive"

        setupUI()
        setupChart()
        observeViewModel()
        viewModel.startListening(apiaryId)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvApiaryName.text = apiaryName

        binding.btnCamera.setOnClickListener {
            startActivity(Intent(this, AiScannerActivity::class.java).apply {
                putExtra(AiScannerActivity.EXTRA_HIVE_NAME, apiaryName)
                putExtra(AiScannerActivity.EXTRA_APIARY_ID, apiaryId)
            })
        }

        binding.btnLiveReadings.setOnClickListener { showLiveReadings() }
        binding.btnAnalytics.setOnClickListener    { showWeightAnalytics() }
    }

    // ── MPAndroidChart setup ──────────────────────────────────────────────────
    private fun setupChart() {
        val chart = binding.weightLineChart
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)
            legend.textColor = Color.WHITE
            legend.textSize = 11f

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                gridColor = Color.parseColor("#33FFFFFF")
                axisLineColor = Color.parseColor("#55FFFFFF")
                granularity = 1f
                setDrawGridLines(true)
            }
            axisLeft.apply {
                textColor = Color.WHITE
                gridColor = Color.parseColor("#33FFFFFF")
                axisLineColor = Color.parseColor("#55FFFFFF")
                setDrawZeroLine(false)
            }
            axisRight.isEnabled = false
            setNoDataText("Waiting for weight readings...")
            setNoDataTextColor(Color.parseColor("#AAAAAA"))
        }
    }

    private fun updateChart(points: List<WeightPoint>) {
        if (points.isEmpty()) return
        val chart = binding.weightLineChart

        val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.weight) }
        val labels  = points.map { it.timeLabel }

        val dataSet = LineDataSet(entries, "Weight (kg)").apply {
            color = Color.parseColor("#F4B400")
            valueTextColor = Color.WHITE
            valueTextSize = 9f
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(Color.parseColor("#F4B400"))
            circleHoleColor = Color.parseColor("#1A3329")
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#F4B400")
            fillAlpha = 40
            setDrawValues(true)
        }

        chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chart.xAxis.labelCount = labels.size.coerceAtMost(6)
        chart.data = LineData(dataSet)
        chart.animateX(600)
        chart.invalidate()
    }

    // ── Tab switching ─────────────────────────────────────────────────────────
    private fun showLiveReadings() {
        binding.layoutLiveReadings.visibility = View.VISIBLE
        binding.layoutWeightAnalytics.visibility = View.GONE
        binding.btnLiveReadings.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent))
        binding.btnLiveReadings.setTextColor(ContextCompat.getColor(this, R.color.login_accent))
        binding.btnAnalytics.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        binding.btnAnalytics.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    private fun showWeightAnalytics() {
        binding.layoutLiveReadings.visibility = View.GONE
        binding.layoutWeightAnalytics.visibility = View.VISIBLE
        binding.btnAnalytics.setBackgroundColor(ContextCompat.getColor(this, R.color.white_10_percent))
        binding.btnAnalytics.setTextColor(ContextCompat.getColor(this, R.color.login_accent))
        binding.btnLiveReadings.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
        binding.btnLiveReadings.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }

    // ── ViewModel observers ───────────────────────────────────────────────────
    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.contentLayout.alpha = if (loading) 0.5f else 1.0f
        }

        viewModel.apiaryData.observe(this) { apiary ->
            apiary?.let {
                binding.viewLiveStatus.setBackgroundResource(
                    if (it.isConnected) R.drawable.bg_green_circle else R.drawable.bg_red_circle
                )
                // Online/Offline is now driven by sensorOnline LiveData (30s timeout)

                if (it.isConnected) {
                    binding.tvTempValue.text     = "%.1f".format(it.temperature)
                    binding.tvHumidityValue.text = "%.1f".format(it.humidity)
                    binding.tvWeightValue.text   = "%.1f".format(it.weight)
                    // tvMoistureValue (Hive Lid) is set in updateStatusLabels below
                    checkAndAlert(it)
                    updateStatusLabels(it)
                } else {
                    binding.tvTempValue.text     = "--"
                    binding.tvHumidityValue.text = "--"
                    binding.tvMoistureValue.text = "--"
                    binding.tvWeightValue.text   = "--"
                    resetStatusLabels()
                }

                binding.tvLocationValue.text = it.location
                binding.tvNodeIdValue.text   = "Node ID: ${it.nodeId}"
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                binding.tvLastUpdatedValue.text = "Last updated: ${sdf.format(Date(it.lastUpdate))}"
            } ?: resetValues()
        }

        viewModel.weightAnalytics.observe(this) { analytics ->
            analytics?.let {
                binding.tvAnalyticsCurrentWeight.text = "%.1fkg".format(it.currentWeight)
                binding.tvTrendStatus.text  = it.trendStatus
                binding.tvHarvestStatus.text = it.harvestStatus
                binding.tvTotalGain.text    = (if (it.totalGain >= 0) "+" else "") + "%.1fkg".format(it.totalGain)
                binding.tvAvgGain.text      = "%.2fkg/day".format(it.avgDailyGain)
                binding.tvPeakWeight.text   = "%.1fkg".format(it.peakWeight)
                binding.tvTrendStatus.setTextColor(
                    if (it.trendStatus.equals("Growing", ignoreCase = true))
                        ContextCompat.getColor(this, android.R.color.holo_green_light)
                    else Color.WHITE
                )
            }
        }

        // Render chart whenever history updates
        viewModel.weightHistory.observe(this) { points ->
            updateChart(points)
        }

        // Online / Offline status from 30s timeout
        viewModel.sensorOnline.observe(this) { online ->
            binding.viewLiveStatus.setBackgroundResource(
                if (online) R.drawable.bg_green_circle else R.drawable.bg_red_circle
            )
            binding.tvConnectionStatus.text = if (online) "Online" else "Offline"
            binding.tvConnectionStatus.setTextColor(
                if (online)
                    ContextCompat.getColor(this, android.R.color.holo_green_light)
                else
                    ContextCompat.getColor(this, android.R.color.holo_red_light)
            )
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }

    // ── Threshold alerts ──────────────────────────────────────────────────────
    private fun checkAndAlert(apiary: com.example.agrihive.addapiary.Apiary) {
        val repo = NotificationRepository(this)

        // Temperature alert
        val tempAlert = apiary.temperature > 0 &&
                (apiary.temperature < 34.0 || apiary.temperature > 36.0)
        if (tempAlert && !lastTempAlerted) {
            val msg = if (apiary.temperature < 34.0)
                "${apiary.name}: Temperature too cold (${apiary.temperature}°C). Optimal: 34–36°C"
            else
                "${apiary.name}: Temperature too hot (${apiary.temperature}°C). Optimal: 34–36°C"
            repo.addNotification("🌡️ Temperature Alert", msg, NotificationType.TEMPERATURE_ALERT)
            RainAlertNotification.showAdminReplyNotification(this, msg)
        }
        lastTempAlerted = tempAlert

        // Humidity alert
        val humAlert = apiary.humidity > 0 &&
                (apiary.humidity < 50.0 || apiary.humidity > 80.0)
        if (humAlert && !lastHumidityAlerted) {
            val msg = "${apiary.name}: Humidity out of range (${apiary.humidity}%). Optimal: 50–80%"
            repo.addNotification("💧 Humidity Alert", msg, NotificationType.SYSTEM)
            RainAlertNotification.showAdminReplyNotification(this, msg)
        }
        lastHumidityAlerted = humAlert

        // Hive Lid alert — open when moisture = 10.0
        val moistAlert = apiary.moisture >= 5.0 && apiary.isConnected
        if (moistAlert && !lastMoistureAlerted) {
            val msg = "${apiary.name}: Hive lid is OPEN! Check your hive immediately."
            repo.addNotification("🔓 Hive Lid Open", msg, NotificationType.FEEDING_ALERT)
            RainAlertNotification.showAdminReplyNotification(this, msg)
        }
        lastMoistureAlerted = moistAlert

        // Weight alert (possible theft or swarm)
        val weightAlert = apiary.weight in 0.1..4.9
        if (weightAlert && !lastWeightAlerted) {
            val msg = "${apiary.name}: Weight critically low (${apiary.weight}kg). Possible swarm or theft!"
            repo.addNotification("⚖️ Weight Alert", msg, NotificationType.SYSTEM)
            RainAlertNotification.showAdminReplyNotification(this, msg)
        }
        lastWeightAlerted = weightAlert
    }

    // ── Status label helpers ──────────────────────────────────────────────────
    private fun updateStatusLabels(apiary: com.example.agrihive.addapiary.Apiary) {
        val tempStatus = when {
            apiary.temperature <= 0  -> "No Data"
            apiary.temperature < 34.0 -> "Too Cold"
            apiary.temperature > 36.0 -> "Too Hot"
            else -> "Normal"
        }
        binding.tvTempStatus.text = tempStatus
        binding.tvTempStatus.setTextColor(statusColor(tempStatus))

        val humStatus = when {
            apiary.humidity <= 0    -> "No Data"
            apiary.humidity < 50.0  -> "Too Dry"
            apiary.humidity > 80.0  -> "Too Humid"
            else -> "Normal"
        }
        binding.tvHumidityStatus.text = humStatus
        binding.tvHumidityStatus.setTextColor(statusColor(humStatus))

        // Hive Lid — moisture: 10.0 = OPEN, 0.0 = CLOSED (set by RTDB listener)
        val lidIsOpen = apiary.moisture >= 5.0 && apiary.isConnected
        val lidWord = when {
            !apiary.isConnected    -> "--"
            lidIsOpen              -> "OPEN"
            else                   -> "CLOSED"
        }

        // Main value: the word OPEN or CLOSED
        binding.tvMoistureValue.text = lidWord
        binding.tvMoistureValue.setTextColor(
            when (lidWord) {
                "OPEN"   -> ContextCompat.getColor(this, android.R.color.holo_orange_light)
                "CLOSED" -> ContextCompat.getColor(this, android.R.color.holo_green_light)
                else     -> ContextCompat.getColor(this, android.R.color.darker_gray)
            }
        )

        // Status pill below
        binding.tvMoistureStatus.text = when (lidWord) {
            "OPEN"   -> "⚠ Check Hive"
            "CLOSED" -> "Secure"
            else     -> "No Data"
        }
        binding.tvMoistureStatus.setTextColor(
            if (lidIsOpen) android.graphics.Color.parseColor("#FF9800")  // bright orange
            else android.graphics.Color.parseColor("#4CAF50")            // bright green
        )

        // Orange card stroke when lid is open
        if (lidIsOpen) {
            binding.cardMoisture.strokeWidth = 2
            binding.cardMoisture.strokeColor = ContextCompat.getColor(this, android.R.color.holo_orange_light)
        } else {
            binding.cardMoisture.strokeWidth = 0
        }

        val weightStatus = when {
            apiary.weight <= 0   -> "No Data"
            apiary.weight < 5.0  -> "Low - Check Hive"
            else -> "Normal"
        }
        binding.tvWeightStatus.text = weightStatus
        binding.tvWeightStatus.setTextColor(
            if (weightStatus == "Normal") android.graphics.Color.parseColor("#4CAF50")
            else android.graphics.Color.parseColor("#FF5252")
        )
    }

    private fun statusColor(status: String): Int = when (status) {
        "Normal"  -> android.graphics.Color.parseColor("#4CAF50")  // bright green
        "No Data" -> android.graphics.Color.parseColor("#888888")  // grey
        else      -> android.graphics.Color.parseColor("#FF5252")  // bright red
    }

    private fun resetStatusLabels() {
        val noDataColor = android.graphics.Color.parseColor("#888888")
        listOf(binding.tvTempStatus, binding.tvHumidityStatus,
               binding.tvMoistureStatus, binding.tvWeightStatus).forEach {
            it.text = "No Data"
            it.setTextColor(noDataColor)
        }
    }

    private fun resetValues() {
        listOf(binding.tvTempValue, binding.tvHumidityValue,
               binding.tvWeightValue).forEach { it.text = "--" }
        binding.tvMoistureValue.text = "--"
        binding.tvMoistureValue.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        listOf(binding.tvTempStatus, binding.tvHumidityStatus,
               binding.tvMoistureStatus, binding.tvWeightStatus).forEach {
            it.text = "No Data"
            it.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        binding.cardMoisture.strokeWidth = 0
        // Show Offline when sensor disconnects
        binding.viewLiveStatus.setBackgroundResource(R.drawable.bg_red_circle)
        binding.tvConnectionStatus.text = "Offline"
        binding.tvConnectionStatus.setTextColor(
            ContextCompat.getColor(this, android.R.color.holo_red_light)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopListening(apiaryId)
    }
}
