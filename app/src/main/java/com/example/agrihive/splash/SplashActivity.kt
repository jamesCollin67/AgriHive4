package com.example.agrihive.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.landing.LandingActivity

class SplashActivity : AppCompatActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val progressHoney = findViewById<View>(R.id.progressHoney)

        viewModel.progress.observe(this) { progress ->
            val maxWidth = resources.getDimensionPixelSize(R.dimen.progress_max_width)
            val width = (progress / 100f * maxWidth).toInt()

            val params = progressHoney.layoutParams
            params.width = width
            progressHoney.layoutParams = params
        }

        viewModel.navigate.observe(this) {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }
    }
}
