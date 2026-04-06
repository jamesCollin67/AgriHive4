package com.example.agrihive.hivestreams

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.agrihive.databinding.ActivityAiScannerBinding
import java.io.File
import java.io.FileOutputStream

class AiScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiScannerBinding
    private val viewModel: AiScannerViewModel by viewModels()
    private var photoUri: Uri? = null
    private var cropUri: Uri? = null
    private var hiveName: String = "Hive"
    private var apiaryId: String? = null

    companion object {
        const val EXTRA_HIVE_NAME = "extra_hive_name"
        const val EXTRA_APIARY_ID = "extra_apiary_id"
    }

    // 1. Capture Photo Launcher
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri?.let { startCrop(it) }
        }
    }

    // 2. Pick from Gallery Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    // 3. Crop Launcher
    private val cropImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = result.data?.data ?: cropUri
            if (resultUri != null) {
                viewModel.analyzeImage(resultUri)
            } else {
                val bundle = result.data?.extras
                val bitmap = bundle?.getParcelable<Bitmap>("data")
                if (bitmap != null) {
                    viewModel.analyzeImage(saveBitmapToCache(bitmap))
                } else {
                    photoUri?.let { viewModel.analyzeImage(it) }
                }
            }
        } else {
            photoUri?.let { 
                Toast.makeText(this, "Proceeding with original image...", Toast.LENGTH_SHORT).show()
                viewModel.analyzeImage(it) 
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hiveName = intent.getStringExtra(EXTRA_HIVE_NAME)?.trim()?.takeIf { it.isNotEmpty() } ?: "Hive"
        apiaryId = intent.getStringExtra(EXTRA_APIARY_ID)

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable("photo_uri")
            cropUri = savedInstanceState.getParcelable("crop_uri")
        }

        setupClicks()
        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("photo_uri", photoUri)
        outState.putParcelable("crop_uri", cropUri)
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnTakePhoto.setOnClickListener {
            checkPermissionAndLaunchCamera()
        }
        
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun checkPermissionAndLaunchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        photoUri = createTempImageUri("raw_scan")
        photoUri?.let { takePhotoLauncher.launch(it) }
    }

    private fun startCrop(sourceUri: Uri) {
        photoUri = sourceUri
        cropUri = createTempImageUri("cropped_scan")
        
        val intent = Intent("com.android.camera.action.CROP")
        intent.setDataAndType(sourceUri, "image/*")
        intent.putExtra("crop", "true")
        intent.putExtra("aspectX", 1)
        intent.putExtra("aspectY", 1)
        intent.putExtra("outputX", 512)
        intent.putExtra("outputY", 512)
        intent.putExtra("scale", true)
        intent.putExtra("return-data", false)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropUri)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri("", cropUri)

        val resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission(packageName, cropUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        try {
            cropImageLauncher.launch(intent)
        } catch (e: Exception) {
            viewModel.analyzeImage(sourceUri)
        }
    }

    private fun applyLoadingDots(activeIndex: Int) {
        val gold = Color.parseColor("#F4B400")
        val muted = Color.parseColor("#9CAF9F")
        val dots = listOf(binding.loadingDot0, binding.loadingDot1, binding.loadingDot2)
        dots.forEachIndexed { i, v ->
            val active = i == activeIndex
            v.alpha = if (active) 1f else 0.35f
            v.backgroundTintList = ColorStateList.valueOf(if (active) gold else muted)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.layoutInitial.visibility = if (isLoading) View.GONE else View.VISIBLE
            binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.loadingMessage.observe(this) { message ->
            binding.tvLoadingMessage.text = message
        }

        viewModel.scanResult.observe(this) { result ->
            result?.let { (label, score) ->
                val intent = Intent(this, ScanResultActivity::class.java).apply {
                    putExtra(ScanResultActivity.EXTRA_DISEASE, label)
                    putExtra(ScanResultActivity.EXTRA_HEALTH_SCORE, score)
                    putExtra(ScanResultActivity.EXTRA_IMAGE_URI, viewModel.currentImageUri.toString())
                    putExtra(ScanResultActivity.EXTRA_HIVE_NAME, hiveName)
                    putExtra(ScanResultActivity.EXTRA_APIARY_ID, apiaryId)
                }
                startActivity(intent)
                viewModel.doneScan()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun createTempImageUri(prefix: String): Uri {
        val storageDir = externalCacheDir ?: cacheDir
        val tempFile = File(storageDir, "${prefix}_${System.currentTimeMillis()}.jpg").apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(this, "${packageName}.provider", tempFile)
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val storageDir = externalCacheDir ?: cacheDir
        val file = File(storageDir, "cropped_result_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return FileProvider.getUriForFile(this, "${packageName}.provider", file)
    }
}
