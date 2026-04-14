package com.example.agrihive.payment

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private val viewModel: PaymentSuccessViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_success)

        val planName    = intent.getStringExtra("PLAN_NAME") ?: "Subscription"
        val planPrice   = intent.getDoubleExtra("PLAN_PRICE", 0.0)
        val referenceId = intent.getStringExtra("REFERENCE_ID") ?: "—"

        val sdf     = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val today   = sdf.format(Date())
        val validMs = System.currentTimeMillis() + (90L * 24 * 60 * 60 * 1000)
        val validUntil = sdf.format(Date(validMs))

        findViewById<TextView>(R.id.tv_ref_no).text          = referenceId
        findViewById<TextView>(R.id.tv_receipt_plan).text    = "AgriHive $planName"
        findViewById<TextView>(R.id.tv_receipt_amount).text  = "₱${planPrice.toInt()}"
        findViewById<TextView>(R.id.tv_receipt_method).text  = "PayMongo"
        findViewById<TextView>(R.id.tv_receipt_date).text    = today
        findViewById<TextView>(R.id.tv_receipt_valid).text   = validUntil

        findViewById<MaterialButton>(R.id.btn_back_dashboard).setOnClickListener {
            viewModel.onBackToDashboardClicked()
        }

        viewModel.navigateDashboard.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(
                    Intent(this, DashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                finish()
                viewModel.doneNavigateDashboard()
            }
        }
    }
}
