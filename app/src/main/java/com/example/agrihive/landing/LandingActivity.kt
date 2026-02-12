package com.example.agrihive.landing

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.login.LoginActivity
import com.example.agrihive.databinding.ActivityLandingPageBinding

class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLandingPageBinding
    private val viewModel: LandingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewBinding
        binding = ActivityLandingPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Button click
        binding.btnGetStarted.setOnClickListener {
            viewModel.onGetStartedClicked()
        }

        // Observe navigation LiveData
        viewModel.navigateToLogin.observe(this) { navigate ->
            if (navigate) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish() // close LandingActivity
                viewModel.doneNavigating() // reset state
            }
        }
    }
}
