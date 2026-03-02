package com.example.agrihive.hivestreams

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType

class SendReportActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnSubmit: Button
    private lateinit var btnCamera: ImageButton
    private lateinit var etMessage: EditText
    private var selectedImageUri: Uri? = null
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    // Activity result launcher for camera
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                Toast.makeText(this, "Photo attached successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_report)

        btnBack = findViewById(R.id.btnBack)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnCamera = findViewById(R.id.btnCamera)
        etMessage = findViewById(R.id.etMessage)

        btnBack.setOnClickListener {
            finish()
        }

        // Camera button - take picture
        btnCamera.setOnClickListener {
            takePhoto()
        }

        // Submit button - show confirmation dialog
        btnSubmit.setOnClickListener {
            showConfirmationDialog()
        }
    }

    private fun takePhoto() {
        // Create a temporary file for the image
        val photoUri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            java.io.File(cacheDir, "sensor_photo.jpg")
        )
        selectedImageUri = photoUri
        takePictureLauncher.launch(photoUri)
    }

    private fun showConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_report, null)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        
        btnConfirm.setOnClickListener {
            // Submit the report
            submitReport()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun submitReport() {
        val message = etMessage.text.toString()
        
        if (message.isBlank()) {
            Toast.makeText(this, "Please describe your issue before submitting.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Log the activity before navigating
        activityLogViewModel.addLog(LogType.DATA_ACTION, "Sent a report: $message")
        
        // Navigate to Report Sent screen
        val intent = Intent(this, ReportSentActivity::class.java)
        startActivity(intent)
        finish()
    }
}
