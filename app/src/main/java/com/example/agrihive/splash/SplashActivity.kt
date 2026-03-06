package com.example.agrihive.splash

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivitySplashScreenBinding
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.landing.LandingActivity
import com.example.agrihive.utils.NetworkAlertDialog
import com.example.agrihive.utils.NetworkUtils
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check internet connection first
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoInternetDialog()
            return
        }

        startSplashProcess()
    }

    private fun startSplashProcess() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize ViewBinding
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.startSplash()
    }

    private fun showNoInternetDialog() {
        // Initialize ViewBinding first so we can show dialog properly
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        NetworkAlertDialog.show(
            context = this,
            onTryAgain = {
                // Check internet again
                if (NetworkUtils.isNetworkAvailable(this)) {
                    // Internet is now available - restart activity to get fresh state
                    finish()
                    startActivity(Intent(this, SplashActivity::class.java))
                } else {
                    // Still no internet - show dialog again
                    showNoInternetDialog()
                }
            },
            onCancel = {
                // Close the app
                finishAffinity()
            }
        )
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

        // Navigate after progress complete - always go to Login/Landing
        viewModel.navigate.observe(this) { destination ->
            destination?.let {
                // Always go to Login screen (Landing) after splash
                // Users should login every time the app starts
                val intent = Intent(this, LandingActivity::class.java)
                startActivity(intent)
                finish()
                viewModel.doneNavigating()
            }
        }
    }
}
