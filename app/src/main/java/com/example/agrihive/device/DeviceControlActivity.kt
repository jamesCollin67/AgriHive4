package com.example.agrihive.device

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityDeviceControlsBinding
import com.example.agrihive.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Device Controls Page — Hive temperature, cooling fan toggle, fan auto-control delay.
 */
class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlsBinding
    private var delayValue = 20 // Default delay in seconds
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var apiaryListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load saved delay
        val prefs = getSharedPreferences("AgriHiveDevicePrefs", Context.MODE_PRIVATE)
        delayValue = prefs.getInt("fan_delay", 20)

        setupClickListeners()
        updateUI()
        listenToHiveData()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Auto Control Switch
        binding.switchAuto.setOnCheckedChangeListener { _, isChecked ->
            updateStatus()
        }

        // Stepper for delay
        binding.btnDelayMinus.setOnClickListener {
            if (delayValue > 0) {
                delayValue--
                saveDelay()
                updateDelayText()
            }
        }

        binding.btnDelayPlus.setOnClickListener {
            delayValue++
            saveDelay()
            updateDelayText()
        }
    }

    private fun saveDelay() {
        val prefs = getSharedPreferences("AgriHiveDevicePrefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("fan_delay", delayValue).apply()
    }

    private fun listenToHiveData() {
        val uid = auth.currentUser?.uid ?: return
        
        // Listen to the user's apiaries to find any that are overheating
        apiaryListener = firestore.collection("apiaries")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, _ ->
                val apiaries = snapshot?.documents ?: return@addSnapshotListener
                
                // Find the one with highest temp or prioritize an overheating one
                val overheatingApiary = apiaries.find { (it.getDouble("temperature") ?: 0.0) > 36.0 }
                val displayApiary = overheatingApiary ?: apiaries.firstOrNull()
                
                displayApiary?.let { doc ->
                    val name = doc.getString("name") ?: "Hive"
                    val temp = doc.getDouble("temperature") ?: 0.0
                    
                    binding.tvTempLabel.text = "Current Temperature: $name"
                    binding.tvTemperature.text = "%.1f°C".format(temp)
                    
                    if (temp > 36.0) {
                        binding.tvTemperature.setTextColor(getColor(android.R.color.holo_red_light))
                        binding.tvStatus.text = "CRITICAL: Overheating (>36°C)"
                        binding.tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
                        
                        if (binding.switchAuto.isChecked) {
                            binding.tvStatusTitle.text = "Cooling Fan — ON ($name)"
                            binding.statusDot.setBackgroundResource(R.drawable.bg_red_circle)
                        }
                    } else {
                        binding.tvTemperature.setTextColor(getColor(R.color.login_accent))
                        binding.tvStatus.text = "Normal: 32°C – 36°C"
                        binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
                        updateStatus() // Back to standby/auto mode text
                    }
                }
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
            binding.statusDot.setBackgroundResource(R.drawable.bg_circle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        apiaryListener?.remove()
    }
}
