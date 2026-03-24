package com.example.agrihive.camera

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.agrihive.databinding.ActivityAiScannerBinding
import java.io.File

class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiScannerBinding
    private val viewModel: CameraViewModel by viewModels()
    private var photoUri: Uri? = null
    private var apiaryId: String = ""

    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { viewModel.uploadAndAnalyze(it, apiaryId) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.uploadAndAnalyze(it, apiaryId) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiaryId = intent.getStringExtra("APIARY_ID") ?: ""

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnTakePhoto.setOnClickListener {
            photoUri = createTempImageUri()
            photoUri?.let { takePhotoLauncher.launch(it) }
        }

        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnTakePhoto.isEnabled = !isLoading
            binding.btnGallery.isEnabled = !isLoading
        }

        viewModel.scanResult.observe(this) { result ->
            result?.let {
                Toast.makeText(this, "Scan Result: $it", Toast.LENGTH_LONG).show()
                // You could navigate to a result detail screen here
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createTempImageUri(): Uri {
        val tempFile = File.createTempFile("scan_", ".jpg", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(this, "${packageName}.provider", tempFile)
    }
}
