package com.example.agrihive.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.databinding.ActivitySplashBinding
import com.example.agrihive.landing.LandingActivity
import com.example.agrihive.onboarding.OnboardingActivity
import com.example.agrihive.utils.NetworkAlertDialog
import com.example.agrihive.utils.NetworkUtils
import com.google.firebase.FirebaseApp

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
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

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.startSplash()
    }

    private fun showNoInternetDialog() {
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NetworkAlertDialog.show(
            context = this,
            onTryAgain = {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    finish()
                    startActivity(Intent(this, SplashActivity::class.java))
                } else {
                    showNoInternetDialog()
                }
            },
            onCancel = {
                finishAffinity()
            }
        )
    }

    private fun observeViewModel() {
        viewModel.navigate.observe(this) { destination ->
            destination?.let {
                val intent = when (it) {
                    "dashboard" -> {
                        val prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)
                        val rememberMe = prefs.getBoolean("remember_me", false)
                        if (rememberMe) {
                            Intent(this, DashboardActivity::class.java)
                        } else if (!prefs.getBoolean("onboarding_complete", false)) {
                            Intent(this, OnboardingActivity::class.java)
                        } else {
                            Intent(this, LandingActivity::class.java)
                        }
                    }
                    "landing" -> {
                        val prefs = getSharedPreferences("AgriHivePrefs", MODE_PRIVATE)
                        if (!prefs.getBoolean("onboarding_complete", false)) {
                            Intent(this, OnboardingActivity::class.java)
                        } else {
                            Intent(this, LandingActivity::class.java)
                        }
                    }
                    else -> Intent(this, LandingActivity::class.java)
                }
                startActivity(intent)
                finish()
                viewModel.doneNavigating()
            }
        }
    }
}
