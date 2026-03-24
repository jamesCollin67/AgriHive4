package com.example.agrihive.device

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivityDeviceControlsBinding

/**
 * Device Controls Page — Hive temperature, cooling fan toggle, fan auto-control delay. (Spec)
 */
class DeviceControlActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceControlsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }
}
