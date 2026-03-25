package com.example.agrihive.subscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.example.agrihive.payment.PaymentDetailsActivity
import com.example.agrihive.payment.PaymentCallback
import com.example.agrihive.payment.PaymentRequest
import com.example.agrihive.payment.PaymentServiceFactory

class SubscriptionActivity : AppCompatActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        // Load plan from intent FIRST before setupViews
        val planName = intent.getStringExtra("PLAN_NAME")
        val planPrice = intent.getDoubleExtra("PLAN_PRICE", -1.0)
        val billingType = intent.getStringExtra("BILLING_TYPE") ?: "Monthly payment"

        if (planName != null && planPrice != -1.0) {
            val plan = SubscriptionPlan(
                id = "custom_${planName.lowercase()}",
                name = planName,
                description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
                apiaryTier = ApiaryTier.TIER_1_2,
                billingType = if (billingType.contains("month", true)) BillingType.MONTHLY else BillingType.QUARTERLY,
                price = planPrice
            )
            viewModel.setSelectedPlan(plan)
        } else {
            viewModel.setSelectedPlan(SubscriptionViewModel.getDefaultPlan())
        }

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_continue).setOnClickListener {
            val currentPlan = viewModel.selectedPlan.value
            val planName = currentPlan?.name ?: "Subscription"
            val planPrice = currentPlan?.price ?: 0.0
            
            activityLogViewModel.addLog(LogType.SUBSCRIPTION, "Selected $planName")
            
            val intent = Intent(this, PaymentDetailsActivity::class.java).apply {
                putExtra("PLAN_NAME", planName)
                putExtra("PLAN_PRICE", planPrice)
            }
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.selectedPlan.observe(this) { plan ->
            plan?.let {
                val continueText = "Continue with ${it.name} — ₱${String.format("%.0f", it.price)}  →"
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_continue).text = continueText
            }
        }

        viewModel.paymentSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}
