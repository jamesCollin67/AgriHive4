package com.example.agrihive.hivestreams

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.agrihive.databinding.ActivitySendReportBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Report issue with optional photo from the device camera or gallery (not the AI scanner flow).
 */
class SendReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendReportBinding
    private val viewModel: SendReportViewModel by viewModels()
    private var cameraPhotoUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraPhotoUri?.let { showAttachmentPreview(it) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { showAttachmentPreview(it) }
    }

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            launchDeviceCamera()
        } else {
            Toast.makeText(this, "Camera permission is needed to take a photo", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_APIARY_ID = "apiary_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnCaptureAttachment.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Add attachment")
                .setItems(arrayOf("Take photo", "Choose from gallery")) { _, which ->
                    when (which) {
                        0 -> checkCameraAndLaunch()
                        1 -> pickImageLauncher.launch("image/*")
                    }
                }
                .show()
        }

        binding.btnRemoveAttachment.setOnClickListener {
            binding.cardAttachmentPreview.visibility = View.GONE
            cameraPhotoUri = null
        }

        binding.btnSubmitReport.setOnClickListener {
            val description = binding.etIssueDescription.text?.toString()?.trim() ?: ""
            viewModel.onSubmitClicked(description)
        }

        observeViewModel()
    }

    private fun checkCameraAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
                launchDeviceCamera()
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchDeviceCamera() {
        cameraPhotoUri = createTempImageUri("report_attachment")
        cameraPhotoUri?.let { takePictureLauncher.launch(it) }
    }

    private fun showAttachmentPreview(uri: Uri) {
        binding.cardAttachmentPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.ivAttachmentPreview)
    }

    private fun createTempImageUri(prefix: String): Uri {
        val storageDir = externalCacheDir ?: cacheDir
        val tempFile = File(storageDir, "${prefix}_${System.currentTimeMillis()}.jpg").apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(this, "${packageName}.provider", tempFile)
    }

    private fun observeViewModel() {
        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.doneError()
            }
        }

        viewModel.navigateToReportSent.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                Toast.makeText(this, "Report submitted. Thanks for your feedback!", Toast.LENGTH_LONG).show()
                startActivity(android.content.Intent(this, ReportSentActivity::class.java))
                finish()
                viewModel.doneReportSentNavigation()
            }
        }
    }
}
