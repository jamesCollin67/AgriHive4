package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivitySendReportBinding

/**
 * Send Report Page — Text field for issue description, camera attachment. (Spec)
 */
class SendReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendReportBinding
    private val viewModel: SendReportViewModel by viewModels()

    companion object {
        const val EXTRA_APIARY_ID = "apiary_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        observeViewModel()

        binding.btnCaptureAttachment.setOnClickListener {
            viewModel.onCaptureAttachmentClicked()
        }

        binding.btnSubmitReport.setOnClickListener {
            val description = binding.etIssueDescription.text?.toString()?.trim() ?: ""
            viewModel.onSubmitClicked(description)
        }
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneError()
            }
        }

        viewModel.navigateToCapture.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, CaptureIssueActivity::class.java))
                viewModel.doneCaptureNavigation()
            }
        }

        viewModel.navigateToReportSent.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                Toast.makeText(this, "Report submitted. Thanks for your feedback!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, ReportSentActivity::class.java))
                finish()
                viewModel.doneReportSentNavigation()
            }
        }
    }
}
