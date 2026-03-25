package com.example.agrihive.addapiary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityAddApiaryBinding
import com.example.agrihive.utils.NetworkUtils

class AddApiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddApiaryBinding
    private val viewModel: AddApiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddApiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etApiaryName.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val nodeId = binding.etNodeId.text.toString().trim()

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveApiary(name, location, nodeId)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) "Saving..." else "+ Add Apiary"
        }

        viewModel.saveSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Apiary added successfully!", Toast.LENGTH_SHORT).show()
                finish()
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
