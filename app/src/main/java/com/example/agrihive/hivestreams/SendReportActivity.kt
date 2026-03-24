package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivitySendReportBinding

/**
 * Send Report Page — Text field for issue description, camera attachment. (Spec)
 */
class SendReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendReportBinding

    companion object {
        const val EXTRA_APIARY_ID = "apiary_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.cardCamera?.setOnClickListener {
            startActivity(Intent(this, CaptureIssueActivity::class.java))
        }

        binding.btnSubmit.setOnClickListener {
            val description = binding.tilDescription.editText?.text?.toString()?.trim() ?: ""
            if (description.isBlank()) {
                Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Report submitted. Thanks for your feedback!", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, ReportSentActivity::class.java))
            finish()
        }
    }
}
