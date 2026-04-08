package com.example.agrihive.addapiary

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityAddApiaryBinding
import com.example.agrihive.utils.NetworkUtils

class AddApiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddApiaryBinding
    private val viewModel: AddApiaryViewModel by viewModels()

    private var farms: List<BeeFarm> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddApiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val name     = binding.etApiaryName.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val farmName = binding.etFarmName.text.toString().trim()
            val nodeId   = binding.etNodeId.text.toString().trim()

            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveApiary(name, location, farmName, nodeId)
        }
    }

    private fun observeViewModel() {
        viewModel.beeFarms.observe(this) { farmList ->
            farms = farmList
            setupDropdowns(farmList)
        }

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

    private fun setupDropdowns(farmList: List<BeeFarm>) {
        val locationAcv = binding.etLocation as AutoCompleteTextView
        val farmNameAcv = binding.etFarmName as AutoCompleteTextView

        val locationAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            farmList.map { it.address }
        )
        locationAcv.setAdapter(locationAdapter)
        locationAcv.setOnClickListener { locationAcv.showDropDown() }
        locationAcv.setOnItemClickListener { _, _, position, _ ->
            farmNameAcv.setText(farmList[position].name, false)
        }

        val farmNameAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            farmList.map { it.name }
        )
        farmNameAcv.setAdapter(farmNameAdapter)
        farmNameAcv.setOnClickListener { farmNameAcv.showDropDown() }
        farmNameAcv.setOnItemClickListener { _, _, position, _ ->
            locationAcv.setText(farmList[position].address, false)
        }
    }
}
