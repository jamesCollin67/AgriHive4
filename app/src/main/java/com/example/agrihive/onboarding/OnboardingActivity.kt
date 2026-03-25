package com.example.agrihive.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityWelcomeBinding
import com.example.agrihive.landing.LandingActivity

/**
 * Onboarding - 3 swipeable intro screens with progress dots and Get Started button.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val continueAction = {
            getSharedPreferences("AgriHivePrefs", MODE_PRIVATE).edit()
                .putBoolean("onboarding_complete", true).apply()
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }

        binding.btnLogin.setOnClickListener { continueAction() }
        binding.btnRegister.setOnClickListener { continueAction() }
    }
}
