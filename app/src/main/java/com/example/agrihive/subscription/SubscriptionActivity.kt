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
            val initialCard = when {
                planName.equals("Starter", true) -> cardStarter
                planName.equals("Pro", true) -> cardPro
                planName.equals("Enterprise", true) -> cardEnterprise
                else -> cardPro
            }
            selectedCard = initialCard
            applyCardSelection(initialCard)
        } else {
            selectedCard = cardPro
            viewModel.setSelectedPlan(planPro)
            applyCardSelection(cardPro)
        }

        viewModel.setPaymentMethod(PaymentMethod.GCASH)

        // Payment method is selected on the PaymentDetailsActivity — no chip group here
        setupPlanCardClicks()
        setupBillingToggle()
        setupViews()
        observeViewModel()
    }

    private var isMonthly = false
    private var selectedCard: MaterialCardView? = null

    // Price/period views — stored as fields so they're accessible from card clicks too
    private lateinit var tvStarterPrice: android.widget.TextView
    private lateinit var tvStarterPeriod: android.widget.TextView
    private lateinit var tvProPrice: android.widget.TextView
    private lateinit var tvProPeriod: android.widget.TextView
    private lateinit var tvEnterprisePrice: android.widget.TextView

    private fun selectPlanCard(card: MaterialCardView) {
        selectedCard = card
        val price: Double
        val billing: BillingType
        val id: String
        val basePlan: SubscriptionPlan
        when (card) {
            cardStarter -> {
                price = if (isMonthly) 183.0 else 550.0
                billing = if (isMonthly) BillingType.MONTHLY else BillingType.QUARTERLY
                id = if (isMonthly) "plan_starter_1mo" else "plan_starter_3mo"
                basePlan = planStarter
            }
            cardPro -> {
                price = if (isMonthly) 250.0 else 750.0
                billing = if (isMonthly) BillingType.MONTHLY else BillingType.QUARTERLY
                id = if (isMonthly) "plan_pro_1mo" else "plan_pro_3mo"
                basePlan = planPro
            }
            else -> {
                price = if (isMonthly) 333.0 else 999.0
                billing = if (isMonthly) BillingType.MONTHLY else BillingType.QUARTERLY
                id = if (isMonthly) "plan_enterprise_1mo" else "plan_enterprise_3mo"
                basePlan = planEnterprise
            }
        }
        viewModel.setSelectedPlan(basePlan.copy(price = price, billingType = billing, id = id))
        applyCardSelection(card)
    }

    private fun setupPlanCardClicks() {
        cardStarter.setOnClickListener { selectPlanCard(cardStarter) }
        cardPro.setOnClickListener { selectPlanCard(cardPro) }
        cardEnterprise.setOnClickListener { selectPlanCard(cardEnterprise) }
    }

    private fun setupBillingToggle() {
        tvStarterPrice    = findViewById(R.id.tv_starter_price)
        tvStarterPeriod   = findViewById(R.id.tv_starter_period)
        tvProPrice        = findViewById(R.id.tv_pro_price)
        tvProPeriod       = findViewById(R.id.tv_pro_period)
        tvEnterprisePrice = findViewById(R.id.tv_enterprise_price)

        val btnMonthly   = findViewById<android.widget.TextView>(R.id.btn_monthly)
        val btnQuarterly = findViewById<android.widget.TextView>(R.id.btn_quarterly)

        val gold    = ContextCompat.getColor(this, R.color.fab_yellow)
        val muted   = ContextCompat.getColor(this, R.color.text_secondary)
        val white10 = ContextCompat.getColor(this, R.color.white_10_percent)

        btnMonthly.setOnClickListener {
            if (isMonthly) return@setOnClickListener
            isMonthly = true
            // Toggle highlight
            btnMonthly.setTextColor(gold)
            btnMonthly.setBackgroundResource(R.drawable.bg_login_input)
            btnMonthly.backgroundTintList = android.content.res.ColorStateList.valueOf(white10)
            btnQuarterly.setTextColor(muted)
            btnQuarterly.setBackgroundResource(android.R.color.transparent)
            btnQuarterly.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            // Update price labels
            tvStarterPrice.text    = "₱183"
            tvStarterPeriod.text   = "/ month"
            tvProPrice.text        = "₱250"
            tvProPeriod.text       = "/ month"
            tvEnterprisePrice.text = "₱333"
            // Re-apply selection with updated billing
            selectedCard?.let { selectPlanCard(it) }
        }

        btnQuarterly.setOnClickListener {
            if (!isMonthly) return@setOnClickListener
            isMonthly = false
            // Toggle highlight
            btnQuarterly.setTextColor(gold)
            btnQuarterly.setBackgroundResource(R.drawable.bg_login_input)
            btnQuarterly.backgroundTintList = android.content.res.ColorStateList.valueOf(white10)
            btnMonthly.setTextColor(muted)
            btnMonthly.setBackgroundResource(android.R.color.transparent)
            btnMonthly.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            // Update price labels
            tvStarterPrice.text    = "₱550"
            tvStarterPeriod.text   = "/ 3 months"
            tvProPrice.text        = "₱750"
            tvProPeriod.text       = "/ 3 months"
            tvEnterprisePrice.text = "₱999"
            // Re-apply selection with updated billing
            selectedCard?.let { selectPlanCard(it) }
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
