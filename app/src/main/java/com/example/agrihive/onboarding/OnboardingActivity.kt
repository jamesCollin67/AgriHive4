package com.example.agrihive.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityOnboardingBinding
import com.example.agrihive.landing.LandingActivity

/**
 * Onboarding - 3 swipeable intro screens with progress dots and Get Started button.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val slides = listOf(
        OnboardingSlide("Monitor in Real-Time", "Track temperature, humidity, and weight from your hives instantly with IoT sensors.", R.drawable.ic_temperature),
        OnboardingSlide("AI Disease Detection", "Identify Varroa mites, chalkbrood, and nosema with our smart image scanner.", R.drawable.ic_ai),
        OnboardingSlide("Smart Harvest Prediction", "Know when your honey is ready with data-driven harvest timing.", R.drawable.ic_bee)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingAdapter(slides)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                binding.btnGetStarted.text = if (position == slides.size - 1) "Get Started" else "Next"
            }
        })

        binding.btnGetStarted.setOnClickListener {
            if (binding.viewPager.currentItem == slides.size - 1) {
                getSharedPreferences("AgriHivePrefs", MODE_PRIVATE).edit()
                    .putBoolean("onboarding_complete", true).apply()
                startActivity(Intent(this, LandingActivity::class.java))
                finish()
            } else {
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
            }
        }

        binding.tvSkip.setOnClickListener {
            getSharedPreferences("AgriHivePrefs", MODE_PRIVATE).edit()
                .putBoolean("onboarding_complete", true).apply()
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }
    }

    private fun updateDots(position: Int) {
        listOf(binding.dot1, binding.dot2, binding.dot3).forEachIndexed { i, dot ->
            dot.setBackgroundResource(if (i == position) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        }
    }
}

data class OnboardingSlide(val title: String, val description: String, val iconRes: Int)
