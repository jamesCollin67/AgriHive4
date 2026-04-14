package com.example.agrihive.addapiary

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivityEditApiaryBinding
import com.example.agrihive.utils.NetworkUtils

/**
 * Edit Apiary — allows the beekeeper to rename their apiary or delete it.
 * Launched from ApiaryAdapter via long-press on an apiary card.
 */
class EditApiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditApiaryBinding
    private val viewModel: EditApiaryViewModel by viewModels()

    private var apiaryId: String = ""
    private var currentName: String = ""
    private var currentLocation: String = ""

    companion object {
        const val EXTRA_APIARY_ID       = "apiary_id"
        const val EXTRA_APIARY_NAME     = "apiary_name"
        const val EXTRA_APIARY_LOCATION = "apiary_location"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditApiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiaryId        = intent.getStringExtra(EXTRA_APIARY_ID) ?: return finish()
        currentName     = intent.getStringExtra(EXTRA_APIARY_NAME) ?: ""
        currentLocation = intent.getStringExtra(EXTRA_APIARY_LOCATION) ?: ""

        // Pre-fill fields with current values — use post to ensure views are ready
        binding.root.post {
            binding.etApiaryName.setText(currentName)
            binding.etLocation.setText(currentLocation)
        }

        setupClicks()
        observeViewModel()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name     = binding.etApiaryName.text?.toString()?.trim() ?: ""
            val location = binding.etLocation.text?.toString()?.trim() ?: ""

            if (name.isBlank()) {
                binding.tilApiaryName.error = "Apiary name cannot be empty"
                return@setOnClickListener
            }
            binding.tilApiaryName.error = null

            viewModel.updateApiary(apiaryId, name, location)
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Apiary?")
                .setMessage("This will permanently delete \"$currentName\" and all its data. This cannot be undone.")
                .setPositiveButton("Yes, Delete") { _, _ ->
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection. Please try again.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    viewModel.deleteApiary(apiaryId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSave.isEnabled   = !loading
            binding.btnDelete.isEnabled = !loading
            binding.btnSave.text = if (loading) "Saving..." else "Save Changes"
        }

        viewModel.updateSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Apiary updated successfully", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
        }

        viewModel.deleteSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Apiary deleted", Toast.LENGTH_SHORT).show()
                navigateToDashboard()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }
}
