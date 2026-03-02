package com.example.agrihive.addapiary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivityAddApiaryBinding
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType

class AddApiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddApiaryBinding
    private val viewModel: AddApiaryViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddApiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button - go to Dashboard
        binding.backBtn.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // ADD button
        binding.btnAdd.setOnClickListener {

            val name = binding.inputName.text.toString()
            val temp = binding.inputTemp.text.toString()
            val hum = binding.inputHum.text.toString()
            val weight = binding.inputWeight.text.toString()
            val isActive = binding.mainSwitch.isChecked

            viewModel.addApiary(name, temp, hum, weight, isActive)
        }

        // Observe add status
        viewModel.addStatus.observe(this) { success ->
            if (success) {
                // Get the name from the input field at the time of success
                val apiaryName = binding.inputName.text.toString().ifEmpty { "New Apiary" }
                // Log the apiary addition activity
                activityLogViewModel.addLog(LogType.DATA_ACTION, "Added new Apiary: $apiaryName")
                showSuccessDialog()
            } else {
                Toast.makeText(this, "Failed to add Apiary", Toast.LENGTH_SHORT).show()
            }
        }

        // CANCEL button
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showSuccessDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_apiary_added, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.show()

        dialogView.findViewById<android.widget.Button>(R.id.btnDismiss).setOnClickListener {
            dialog.dismiss()
            // Go back to Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}