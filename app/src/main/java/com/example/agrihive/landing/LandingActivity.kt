package com.example.agrihive.landing

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.databinding.ActivityWelcomeBinding
import com.example.agrihive.login.LoginActivity
import com.example.agrihive.register.RegisterActivity

/**
 * Welcome Page — Bee-themed landing screen with "Join our Bee-utiful Community!" tagline,
 * Login and Register buttons.
 */
class LandingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Route to activity_register
        binding.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Route to activity_login
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
