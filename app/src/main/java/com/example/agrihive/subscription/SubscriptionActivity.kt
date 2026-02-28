package com.example.agrihive.subscription

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R

class SubscriptionActivity : AppCompatActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.acitivity_subscription)

        setupViews()
        observeViewModel()

        // Load default plan if not passed from Sensor Subscription
        val planName = intent.getStringExtra("PLAN_NAME")
        val planPrice = intent.getDoubleExtra("PLAN_PRICE", 899.00)
        val billingType = intent.getStringExtra("BILLING_TYPE") ?: "Monthly payment"

        if (planName != null) {
            val plan = SubscriptionPlan(
                id = "custom_plan",
                name = planName,
                description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
                apiaryTier = ApiaryTier.TIER_1_2,
                billingType = if (billingType == "Monthly payment") BillingType.MONTHLY else BillingType.QUARTERLY,
                price = planPrice
            )
            viewModel.setSelectedPlan(plan)
        } else {
            viewModel.setSelectedPlan(SubscriptionViewModel.getDefaultPlan())
        }
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Payment Method Selection
        findViewById<LinearLayout>(R.id.layoutGcash).setOnClickListener {
            processPayment(PaymentMethod.GCASH)
        }

        findViewById<LinearLayout>(R.id.layoutPaymaya).setOnClickListener {
            processPayment(PaymentMethod.PAYMAYA)
        }

        findViewById<LinearLayout>(R.id.layoutBdo).setOnClickListener {
            processPayment(PaymentMethod.BDO)
        }

        findViewById<LinearLayout>(R.id.layoutPaypal).setOnClickListener {
            processPayment(PaymentMethod.PAYPAL)
        }
    }

    private fun processPayment(method: PaymentMethod) {
        viewModel.setPaymentMethod(method)
        Toast.makeText(this, "Processing payment via ${method.displayName}...", Toast.LENGTH_SHORT).show()
        
        // In a real app, this would launch the payment gateway
        // For demo purposes, we'll simulate success after a delay
        viewModel.processPayment()
    }

    private fun observeViewModel() {
        viewModel.selectedPlan.observe(this) { plan ->
            // Plan details are already shown in the layout
            // You can update them dynamically if needed
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            // Could show a loading indicator
        }

        viewModel.paymentSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                // Navigate back to settings or dashboard
                finish()
            }
        }
    }
}
