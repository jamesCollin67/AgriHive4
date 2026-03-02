package com.example.agrihive.subscription

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType

class SubscriptionActivity : AppCompatActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

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
        
        // Log the subscription activity
        val planName = viewModel.selectedPlan.value?.name ?: "Subscription"
        activityLogViewModel.addLog(LogType.SUBSCRIPTION, "Selected $planName - ${method.displayName}")
        
        // Route to payment method
        val paymentUrl = when (method) {
            PaymentMethod.GCASH -> "https://www.gcash.com/"
            PaymentMethod.PAYMAYA -> "https://www.paymaya.com/"
            PaymentMethod.BDO -> "https://www.bdo.com.ph/"
            PaymentMethod.PAYPAL -> "https://www.paypal.com/"
        }
        
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Opening ${method.displayName}...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open ${method.displayName}. Please check if the app is installed.", Toast.LENGTH_LONG).show()
        }
    }

    private fun observeViewModel() {
        viewModel.selectedPlan.observe(this) { plan ->
            plan?.let {
                // Update the subscription details
                findViewById<TextView>(R.id.tvPlanName).text = it.name
                findViewById<TextView>(R.id.tvBillingType).text = it.billingType.displayName
                findViewById<TextView>(R.id.tvTotal).text = "P${String.format("%.2f", it.price)}"
            }
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            // Could show a loading indicator
        }

        viewModel.paymentSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_LONG).show()
                // Log the subscription payment
                val planName = viewModel.selectedPlan.value?.name ?: "Subscription Plan"
                val planPrice = viewModel.selectedPlan.value?.price ?: 0.0
                val paymentMethod = viewModel.selectedPaymentMethod.value?.name ?: "Payment"
                activityLogViewModel.addLog(
                    LogType.SUBSCRIPTION,
                    "Subscribed to $planName - P${String.format("%.2f", planPrice)} via $paymentMethod"
                )
                // Navigate back to settings or dashboard
                finish()
            }
        }
    }
}
