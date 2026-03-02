package com.example.agrihive.device

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType

class DeviceControlActivity : AppCompatActivity() {

    private val viewModel: DeviceControlViewModel by viewModels()
    private lateinit var prefs: SharedPreferences
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    // Fan time options in seconds
    private val fanTimeOptions = listOf(10, 20, 30, 60, 120, 300) // 10s, 20s, 30s, 1min, 2min, 5min
    private val fanTimeLabels = listOf("10 secs", "20 secs", "30 secs", "1 min", "2 mins", "5 mins")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Cooling Fan Notification Switch
        val coolingFanSwitch = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchCoolingFan)
        coolingFanSwitch.isChecked = prefs.getBoolean("cooling_fan_notification", true)
        coolingFanSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCoolingFanNotification(isChecked)
            prefs.edit().putBoolean("cooling_fan_notification", isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications silenced"
            // Log the cooling fan notification change
            activityLogViewModel.addLog(
                LogType.SYSTEM,
                if (isChecked) "Cooling fan notifications enabled" else "Cooling fan notifications disabled"
            )
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Fan Auto-Control Spinner
        val spinnerFanTime = findViewById<android.widget.Spinner>(R.id.spinnerFanTime)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fanTimeLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFanTime.adapter = adapter

        // Set saved selection
        val savedTimeIndex = prefs.getInt("fan_auto_time_index", 3) // Default to 1 min
        spinnerFanTime.setSelection(savedTimeIndex)

        spinnerFanTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTime = fanTimeOptions[position]
                val selectedLabel = fanTimeLabels[position]
                viewModel.setFanAutoTime(selectedTime)
                prefs.edit().putInt("fan_auto_time_index", position).apply()
                prefs.edit().putInt("fan_auto_time_seconds", selectedTime).apply()
                // Log the fan auto control time change
                activityLogViewModel.addLog(
                    LogType.SYSTEM,
                    "Fan auto-control time set to $selectedLabel"
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Simulated temperature display (in real app, this would come from sensor)
        val temperature = prefs.getFloat("current_hive_temperature", 37.0f)
        findViewById<android.widget.TextView>(R.id.tvTemperature).text = "${temperature.toInt()} °C"
    }

    private fun observeViewModel() {
        // Observe temperature updates
        viewModel.currentTemperature.observe(this) { temp ->
            findViewById<android.widget.TextView>(R.id.tvTemperature).text = "${temp.toInt()} °C"
        }

        // Observe notification state
        viewModel.notificationEnabled.observe(this) { enabled ->
            findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switchCoolingFan).isChecked = enabled
        }
    }
}
