package com.example.agrihive.subscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.agrihive.R
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.log.LogType
import com.example.agrihive.payment.PaymentDetailsActivity
import com.google.android.material.card.MaterialCardView

class SubscriptionActivity : AppCompatActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    private lateinit var cardStarter: MaterialCardView
    private lateinit var cardPro: MaterialCardView
    private lateinit var cardEnterprise: MaterialCardView

    private val planStarter by lazy {
        SubscriptionPlan(
            id = "plan_starter_3mo",
            name = "Starter",
            description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
            apiaryTier = ApiaryTier.TIER_1_2,
            billingType = BillingType.QUARTERLY,
            price = 550.0
        )
    }
    private val planPro by lazy {
        SubscriptionPlan(
            id = "plan_pro_3mo",
            name = "Pro",
            description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
            apiaryTier = ApiaryTier.TIER_3_5,
            billingType = BillingType.QUARTERLY,
            price = 750.0
        )
    }
    private val planEnterprise by lazy {
        SubscriptionPlan(
            id = "plan_enterprise_3mo",
            name = "Enterprise",
            description = "Hive monitoring using sensors that measures temperature, humidity, and hive weight, with AI in-app for smart analysis.",
            apiaryTier = ApiaryTier.TIER_5_PLUS,
            billingType = BillingType.QUARTERLY,
            price = 999.0
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        cardStarter = findViewById(R.id.card_starter)
        cardPro = findViewById(R.id.card_pro)
        cardEnterprise = findViewById(R.id.card_enterprise)

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
            when {
                planName.equals("Starter", true) -> applyCardSelection(cardStarter)
                planName.equals("Pro", true) -> applyCardSelection(cardPro)
                planName.equals("Enterprise", true) -> applyCardSelection(cardEnterprise)
                else -> applyCardSelection(cardPro)
            }
        } else {
            viewModel.setSelectedPlan(planPro)
            applyCardSelection(cardPro)
        }

        viewModel.setPaymentMethod(PaymentMethod.GCASH)

        // Payment method is selected on the PaymentDetailsActivity — no chip group here
        setupPlanCardClicks()
        setupViews()
        observeViewModel()
    }

    private fun setupPlanCardClicks() {
        cardStarter.setOnClickListener {
            viewModel.setSelectedPlan(planStarter)
            applyCardSelection(cardStarter)
        }
        cardPro.setOnClickListener {
            viewModel.setSelectedPlan(planPro)
            applyCardSelection(cardPro)
        }
        cardEnterprise.setOnClickListener {
            viewModel.setSelectedPlan(planEnterprise)
            applyCardSelection(cardEnterprise)
        }
    }

    private fun applyCardSelection(selected: MaterialCardView) {
        val strokePx = (2 * resources.displayMetrics.density).toInt()
        val gold = ContextCompat.getColor(this, R.color.fab_yellow)
        listOf(cardStarter, cardPro, cardEnterprise).forEach { card ->
            if (card == selected) {
                card.strokeWidth = strokePx
                card.strokeColor = gold
            } else {
                card.strokeWidth = 0
            }
        }
    }

    private fun setupViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_continue).setOnClickListener {
            val currentPlan = viewModel.selectedPlan.value
            val name = currentPlan?.name ?: "Subscription"
            val price = currentPlan?.price ?: 0.0
            val paymentMethod = viewModel.selectedPaymentMethod.value ?: PaymentMethod.GCASH

            activityLogViewModel.addLog(LogType.SUBSCRIPTION, "Selected $name via ${paymentMethod.displayName}")

            startActivity(
                Intent(this, PaymentDetailsActivity::class.java).apply {
                    putExtra(PaymentDetailsActivity.EXTRA_PLAN_NAME, name)
                    putExtra(PaymentDetailsActivity.EXTRA_PLAN_PRICE, price)
                    putExtra(PaymentDetailsActivity.EXTRA_PAYMENT_METHOD, paymentMethod.name)
                }
            )
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
