package com.example.agrihive.device

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityDeviceControlsBinding
import com.example.agrihive.R

/**
 * Device Controls Page — Hive temperature, cooling fan toggle, fan auto-control delay.
 */
class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlsBinding
    private var delayValue = 20 // Default delay in seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        updateUI()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Auto Control Switch
        binding.switchAuto.setOnCheckedChangeListener { _, isChecked ->
            updateStatus()
            // Here you would typically save the state to SharedPreferences or Firebase
        }

        // Notification Switch
        binding.switchNotif.setOnCheckedChangeListener { _, isChecked ->
            // Save notification preference
        }

        // Stepper for delay using the new IDs
        binding.btnDelayMinus.setOnClickListener {
            if (delayValue > 0) {
                delayValue--
                updateDelayText()
            }
        }

        binding.btnDelayPlus.setOnClickListener {
            delayValue++
            updateDelayText()
        }
    }

    private fun updateDelayText() {
        binding.tvDelayValue.text = "${delayValue}s"
    }

    private fun updateUI() {
        updateDelayText()
        updateStatus()
    }

    private fun updateStatus() {
        if (binding.switchAuto.isChecked) {
            binding.tvStatusTitle.text = "Cooling Fan — Auto Mode Active"
            binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot)
        } else {
            binding.tvStatusTitle.text = "Cooling Fan — Manual Mode"
            // Optionally change dot color for manual mode
        }
    }
}
