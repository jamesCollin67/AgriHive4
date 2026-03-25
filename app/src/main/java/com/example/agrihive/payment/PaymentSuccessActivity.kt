package com.example.agrihive.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton

class PaymentSuccessActivity : AppCompatActivity() {
    private val viewModel: PaymentSuccessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_success)

        findViewById<MaterialButton>(R.id.btn_back_dashboard).setOnClickListener {
            viewModel.onBackToDashboardClicked()
        }

        viewModel.navigateDashboard.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                viewModel.doneNavigateDashboard()
            }
        }
    }
}
