package com.example.agrihive.sensorsubscription

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.payment.PaymentDetailsActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SensorSubscriptionActivity : AppCompatActivity() {

    private val viewModel: SensorSubscriptionViewModel by viewModels()
    private var selectedPlan: String = "Pro"
    private var selectedPrice: Double = 750.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        setupViews()
        updateSelectionUI()
    }

    private fun setupViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        val cardStarter = findViewById<MaterialCardView>(R.id.card_starter)
        val cardPro = findViewById<MaterialCardView>(R.id.card_pro)
        val cardEnterprise = findViewById<MaterialCardView>(R.id.card_enterprise)
        val btnContinue = findViewById<MaterialButton>(R.id.btn_continue)

        cardStarter.setOnClickListener {
            selectedPlan = "Starter"
            selectedPrice = 550.0
            updateSelectionUI()
        }

        cardPro.setOnClickListener {
            selectedPlan = "Pro"
            selectedPrice = 750.0
            updateSelectionUI()
        }

        cardEnterprise.setOnClickListener {
            selectedPlan = "Enterprise"
            selectedPrice = 999.0
            updateSelectionUI()
        }

        btnContinue.setOnClickListener {
            navigateToPayment(selectedPlan, selectedPrice)
        }
    }

    private fun updateSelectionUI() {
        val cardStarter = findViewById<MaterialCardView>(R.id.card_starter)
        val cardPro = findViewById<MaterialCardView>(R.id.card_pro)
        val cardEnterprise = findViewById<MaterialCardView>(R.id.card_enterprise)
        
        val dotStarter = findViewById<ImageView>(R.id.dot_starter)
        val dotPro = findViewById<ImageView>(R.id.dot_pro)
        val dotEnterprise = findViewById<ImageView>(R.id.dot_enterprise)

        val btnContinue = findViewById<MaterialButton>(R.id.btn_continue)

        // Reset all cards to blend with background
        resetCardUI(cardStarter, dotStarter)
        resetCardUI(cardPro, dotPro)
        resetCardUI(cardEnterprise, dotEnterprise)

        // Highlight selected card with its corresponding plan color
        when (selectedPlan) {
            "Starter" -> highlightCard(cardStarter, dotStarter, Color.parseColor("#F4B400"))
            "Pro" -> highlightCard(cardPro, dotPro, Color.parseColor("#66BB6A"))
            "Enterprise" -> highlightCard(cardEnterprise, dotEnterprise, Color.parseColor("#F4B400"))
        }

        btnContinue.text = "Continue with $selectedPlan — ₱${selectedPrice.toInt()}"
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
