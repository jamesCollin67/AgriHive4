package com.example.agrihive.sensorsubscription

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.subscription.SubscriptionActivity

class SensorSubscriptionActivity : AppCompatActivity() {

    private val viewModel: SensorSubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_subscription)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Back button - goes back to settings
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Tier 1-2: Quarterly
        findViewById<View>(R.id.btnTier1Quarter).setOnClickListener {
            navigateToSubscription("1-2 Apiaries", 550.00, "3 months")
        }

        // Tier 1-2: Monthly
        findViewById<View>(R.id.btnTier1Monthly).setOnClickListener {
            navigateToSubscription("1-2 Apiaries", 183.00, "Monthly payment")
        }

        // Tier 3-5: Quarterly
        findViewById<View>(R.id.btnTier2Quarter).setOnClickListener {
            navigateToSubscription("3-5 Apiaries", 750.00, "3 months")
        }

        // Tier 3-5: Monthly
        findViewById<View>(R.id.btnTier2Monthly).setOnClickListener {
            navigateToSubscription("3-5 Apiaries", 250.00, "Monthly payment")
        }

        // Tier 5+: Quarterly
        findViewById<View>(R.id.btnTier3Quarter).setOnClickListener {
            navigateToSubscription("5+ Apiaries", 999.00, "3 months")
        }

        // Tier 5+: Monthly
        findViewById<View>(R.id.btnTier3Monthly).setOnClickListener {
            navigateToSubscription("5+ Apiaries", 333.00, "Monthly payment")
        }
    }

    private fun navigateToSubscription(planName: String, price: Double, billingType: String) {
        val intent = Intent(this, SubscriptionActivity::class.java)
        intent.putExtra("PLAN_NAME", planName)
        intent.putExtra("PLAN_PRICE", price)
        intent.putExtra("BILLING_TYPE", billingType)
        startActivity(intent)
    }

    private fun observeViewModel() {
        viewModel.pricingTiers.observe(this) { tiers ->
            // Pricing is already shown in the layout
            // You can dynamically update if needed
        }
    }
}
