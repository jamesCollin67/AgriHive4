package com.example.agrihive.register

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityRegisterBinding
import com.example.agrihive.login.LoginActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    // Bee farms data — mirrors AddApiaryViewModel
    private val beeFarms = listOf(
        Pair("Apis Prince Honeybee Farm", "Apis Prince Honeybee Farm, Greener's Farm, Taptap, Cebu City, Cebu"),
        Pair("GKG FARM CEBU PH",          "Cansiguiring, Carmen, Cebu")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDropdowns()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                firstName       = binding.firstName.text?.toString()?.trim() ?: "",
                lastName        = binding.lastName.text?.toString()?.trim() ?: "",
                email           = binding.email.text?.toString()?.trim() ?: "",
                password        = binding.password.text?.toString() ?: "",
                confirmPassword = binding.confirmPassword.text?.toString() ?: "",
                termsAccepted   = binding.terms.isChecked,
                farmName        = binding.farmName.text?.toString()?.trim() ?: "",
                farmLocation    = binding.farmLocation.text?.toString()?.trim() ?: ""
            )
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        viewModel.registerSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Registered successfully! Please verify your email.", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.registerError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.doneError()
            }
        }

        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                viewModel.doneNavigating()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.firstName.isEnabled = !isLoading
            binding.lastName.isEnabled = !isLoading
            binding.email.isEnabled = !isLoading
            binding.password.isEnabled = !isLoading
            binding.confirmPassword.isEnabled = !isLoading
            binding.farmName.isEnabled = !isLoading
            binding.farmLocation.isEnabled = !isLoading
            binding.terms.isEnabled = !isLoading
        }
    }

    private fun setupDropdowns() {
        val farmNameAcv     = binding.farmName as AutoCompleteTextView
        val farmLocationAcv = binding.farmLocation as AutoCompleteTextView

        val nameAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, beeFarms.map { it.first })
        farmNameAcv.setAdapter(nameAdapter)
        farmNameAcv.setOnClickListener { farmNameAcv.showDropDown() }
        farmNameAcv.setOnItemClickListener { _, _, position, _ ->
            farmLocationAcv.setText(beeFarms[position].second, false)
        }

        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, beeFarms.map { it.second })
        farmLocationAcv.setAdapter(locationAdapter)
        farmLocationAcv.setOnClickListener { farmLocationAcv.showDropDown() }
        farmLocationAcv.setOnItemClickListener { _, _, position, _ ->
            farmNameAcv.setText(beeFarms[position].first, false)
        }
    }
}
