package com.example.agrihive.addapiary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityAddApiaryBinding
import com.example.agrihive.dashboard.DashboardActivity

class AddApiaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddApiaryBinding
    private val viewModel: AddApiaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddApiaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAdd.setOnClickListener {

            val name = binding.inputName.text.toString()
            val temp = binding.inputTemp.text.toString()
            val hum = binding.inputHum.text.toString()
            val weight = binding.inputWeight.text.toString()
            val isActive = binding.mainSwitch.isChecked

            viewModel.addApiary(name, temp, hum, weight, isActive)
        }

        viewModel.addStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Apiary Added!", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()

            } else {
                Toast.makeText(this, "Failed to add Apiary", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
}
