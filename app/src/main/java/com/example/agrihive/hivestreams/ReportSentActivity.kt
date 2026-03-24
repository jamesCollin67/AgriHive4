package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivityReportSentBinding

/**
 * Report Sent Page — Green checkmark confirmation. (Spec)
 */
class ReportSentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportSentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportSentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnDone?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }
}
