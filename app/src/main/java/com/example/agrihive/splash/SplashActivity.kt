package com.example.agrihive.splash

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivitySplashScreenBinding
import com.example.agrihive.landing.LandingActivity
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize ViewBinding
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.startSplash()
    }

    private fun observeViewModel() {
        // Animate progressHoney width
        viewModel.progress.observe(this) { progress ->
            binding.progressContainer.post {
                val containerWidth = binding.progressContainer.width
                if (containerWidth > 0) {
                    val newWidth = (containerWidth * (progress / 100f)).toInt()
                    val params: ViewGroup.LayoutParams = binding.progressHoney.layoutParams
                    params.width = newWidth
                    binding.progressHoney.layoutParams = params
                }
            }
        }

        // Navigate after progress complete
        viewModel.navigate.observe(this) { navigate ->
            if (navigate) { // always check true
                startActivity(Intent(this, LandingActivity::class.java))
                finish() // close splash
            }
        }


    }
}
