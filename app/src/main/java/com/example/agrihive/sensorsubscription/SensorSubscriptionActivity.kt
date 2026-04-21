package com.example.agrihive.sensorsubscription

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.agrihive.R
import com.example.agrihive.payment.PaymentDetailsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SensorSubscriptionActivity : AppCompatActivity() {

    private val viewModel: SensorSubscriptionViewModel by viewModels()
    private var selectedPlan: String = "Basic"
    private var selectedPrice: Double = 0.0
    private var isMonthly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setupViews()
        setupBillingToggle()
        updateSelectionUI()
    }

    private fun setupViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val cardBasic = findViewById<MaterialCardView>(R.id.card_basic)
        val cardStarter = findViewById<MaterialCardView>(R.id.card_starter)
        val cardPro = findViewById<MaterialCardView>(R.id.card_pro)
        val cardEnterprise = findViewById<MaterialCardView>(R.id.card_enterprise)
        val btnContinue = findViewById<MaterialButton>(R.id.btn_continue)

        cardBasic.setOnClickListener {
            selectedPlan = "Basic"
            selectedPrice = 0.0
            updateSelectionUI()
        }

        cardStarter.setOnClickListener {
            selectedPlan = "Starter"
            selectedPrice = if (isMonthly) 183.0 else 550.0
            updateSelectionUI()
        }

        cardPro.setOnClickListener {
            selectedPlan = "Pro"
            selectedPrice = if (isMonthly) 250.0 else 750.0
            updateSelectionUI()
        }

        cardEnterprise.setOnClickListener {
            selectedPlan = "Enterprise"
            selectedPrice = if (isMonthly) 333.0 else 999.0
            updateSelectionUI()
        }

        btnContinue.setOnClickListener {
            if (selectedPlan == "Basic") {
                // Basic is free — no payment needed, just finish
                android.widget.Toast.makeText(this, "You're on the Basic plan. Upgrade anytime!", android.widget.Toast.LENGTH_SHORT).show()
                finish()
            } else {
                navigateToPayment(selectedPlan, selectedPrice)
            }
        }
    }

    private fun setupBillingToggle() {
        val tvStarterPrice    = findViewById<TextView>(R.id.tv_starter_price)
        val tvStarterPeriod   = findViewById<TextView>(R.id.tv_starter_period)
        val tvProPrice        = findViewById<TextView>(R.id.tv_pro_price)
        val tvProPeriod       = findViewById<TextView>(R.id.tv_pro_period)
        val tvEnterprisePrice = findViewById<TextView>(R.id.tv_enterprise_price)

        val btnMonthly   = findViewById<TextView>(R.id.btn_monthly)
        val btnQuarterly = findViewById<TextView>(R.id.btn_quarterly)

        val gold    = ContextCompat.getColor(this, R.color.fab_yellow)
        val muted   = ContextCompat.getColor(this, R.color.text_secondary)
        val white10 = ContextCompat.getColor(this, R.color.white_10_percent)

        btnMonthly.setOnClickListener {
            if (isMonthly) return@setOnClickListener
            isMonthly = true
            btnMonthly.setTextColor(gold)
            btnMonthly.setBackgroundResource(R.drawable.bg_login_input)
            btnMonthly.backgroundTintList = ColorStateList.valueOf(white10)
            btnQuarterly.setTextColor(muted)
            btnQuarterly.setBackgroundResource(android.R.color.transparent)
            btnQuarterly.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            tvStarterPrice.text    = "₱183"
            tvStarterPeriod.text   = "/ month"
            tvProPrice.text        = "₱250"
            tvProPeriod.text       = "/ month"
            tvEnterprisePrice.text = "₱333"
            // Update selected plan price if a paid plan is selected
            when (selectedPlan) {
                "Starter"    -> { selectedPrice = 183.0; updateSelectionUI() }
                "Pro"        -> { selectedPrice = 250.0; updateSelectionUI() }
                "Enterprise" -> { selectedPrice = 333.0; updateSelectionUI() }
            }
        }

        btnQuarterly.setOnClickListener {
            if (!isMonthly) return@setOnClickListener
            isMonthly = false
            btnQuarterly.setTextColor(gold)
            btnQuarterly.setBackgroundResource(R.drawable.bg_login_input)
            btnQuarterly.backgroundTintList = ColorStateList.valueOf(white10)
            btnMonthly.setTextColor(muted)
            btnMonthly.setBackgroundResource(android.R.color.transparent)
            btnMonthly.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            tvStarterPrice.text    = "₱550"
            tvStarterPeriod.text   = "/ 3 months"
            tvProPrice.text        = "₱750"
            tvProPeriod.text       = "/ 3 months"
            tvEnterprisePrice.text = "₱999"
            // Update selected plan price if a paid plan is selected
            when (selectedPlan) {
                "Starter"    -> { selectedPrice = 550.0; updateSelectionUI() }
                "Pro"        -> { selectedPrice = 750.0; updateSelectionUI() }
                "Enterprise" -> { selectedPrice = 999.0; updateSelectionUI() }
            }
        }
    }

    private fun updateSelectionUI() {
        val cardBasic = findViewById<MaterialCardView>(R.id.card_basic)
        val cardStarter = findViewById<MaterialCardView>(R.id.card_starter)
        val cardPro = findViewById<MaterialCardView>(R.id.card_pro)
        val cardEnterprise = findViewById<MaterialCardView>(R.id.card_enterprise)

        val dotBasic = findViewById<ImageView>(R.id.dot_basic)
        val dotStarter = findViewById<ImageView>(R.id.dot_starter)
        val dotPro = findViewById<ImageView>(R.id.dot_pro)
        val dotEnterprise = findViewById<ImageView>(R.id.dot_enterprise)

        val btnContinue = findViewById<MaterialButton>(R.id.btn_continue)

        resetCardUI(cardBasic, dotBasic)
        resetCardUI(cardStarter, dotStarter)
        resetCardUI(cardPro, dotPro)
        resetCardUI(cardEnterprise, dotEnterprise)

        when (selectedPlan) {
            "Basic"      -> highlightCard(cardBasic, dotBasic, Color.parseColor("#78909C"))
            "Starter"    -> highlightCard(cardStarter, dotStarter, Color.parseColor("#F4B400"))
            "Pro"        -> highlightCard(cardPro, dotPro, Color.parseColor("#66BB6A"))
            "Enterprise" -> highlightCard(cardEnterprise, dotEnterprise, Color.parseColor("#F4B400"))
        }

        btnContinue.text = when (selectedPlan) {
            "Basic" -> "Continue with Basic — Free"
            else    -> "Continue with $selectedPlan — ₱${selectedPrice.toInt()}"
        }
    }

    private fun resetCardUI(card: MaterialCardView, dot: ImageView) {
        card.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT))
        card.strokeWidth = 0
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#1A3329")))
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        
        // Dot appears as an outline (border circle)
        dot.alpha = 1f
        dot.setImageResource(R.drawable.bg_dot_inactive)
        dot.imageTintList = ColorStateList.valueOf(Color.parseColor("#2D4A3E"))
    }

    private fun highlightCard(card: MaterialCardView, dot: ImageView, color: Int) {
        card.setStrokeColor(ColorStateList.valueOf(color))
        card.strokeWidth = 6
        card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#243B33")))
        card.cardElevation = 0f
        card.maxCardElevation = 0f
        
        // Selected dot is filled with accent color
        dot.alpha = 1f
        dot.setImageResource(R.drawable.bg_dot_active)
        dot.imageTintList = ColorStateList.valueOf(color)
    }

    private fun navigateToPayment(planName: String, price: Double) {
        val intent = Intent(this, PaymentDetailsActivity::class.java).apply {
            putExtra("PLAN_NAME", planName)
            putExtra("PLAN_PRICE", price)
        }
        startActivity(intent)
    }
}
