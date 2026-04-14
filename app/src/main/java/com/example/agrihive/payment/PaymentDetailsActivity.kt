package com.example.agrihive.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityPaymentDetailsBinding
import com.example.agrihive.subscription.PaymentMethod
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

class PaymentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentDetailsBinding
    private val viewModel: PaymentDetailsViewModel by viewModels()
    private var planName: String = "Subscription"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "Subscription"
        val planPrice = intent.getDoubleExtra(EXTRA_PLAN_PRICE, 550.0)
        viewModel.setPlanDetails(planPrice)

        binding.tvPlanName.text  = "AgriHive $planName Plan"
        binding.tvPlanPrice.text = "₱${planPrice.toInt()}"
        binding.tvTotalPrice.text = "₱${planPrice.toInt()}"

        // Pre-select method from intent if provided
        intent.getStringExtra(EXTRA_PAYMENT_METHOD)?.let { key ->
            runCatching {
                when (PaymentMethod.valueOf(key)) {
                    PaymentMethod.GCASH  -> viewModel.selectMethod(PaymentDetailsViewModel.Method.GCASH)
                    PaymentMethod.MAYA   -> viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYMAYA)
                    PaymentMethod.BDO    -> viewModel.selectMethod(PaymentDetailsViewModel.Method.BDO)
                    PaymentMethod.PAYPAL -> viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYPAL)
                }
            }
        }

        setupClicks()
        observeViewModel()
    }

    // ── Handle PayMongo redirect back into the app ────────────────────────────
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePayMongoReturn(intent)
    }

    override fun onResume() {
        super.onResume()
        // Also check the original intent (first launch via deep link)
        handlePayMongoReturn(intent)
    }

    private fun handlePayMongoReturn(intent: Intent?) {
        val uri = intent?.data ?: return
        when {
            uri.toString().startsWith(PayMongoService.DEEP_LINK_SUCCESS) -> {
                val refId = uri.getQueryParameter("ref") ?: UUID.randomUUID().toString()
                onPaymentSuccess(refId)
            }
            uri.toString().startsWith(PayMongoService.DEEP_LINK_FAILED) -> {
                Toast.makeText(this, "Payment was cancelled or failed. Please try again.", Toast.LENGTH_LONG).show()
                setLoadingState(false)
            }
        }
    }

    // ── UI setup ──────────────────────────────────────────────────────────────
    private fun setupClicks() {
        binding.btnBack.setOnClickListener { viewModel.onBackClicked() }
        binding.btnGcash.setOnClickListener   { viewModel.selectMethod(PaymentDetailsViewModel.Method.GCASH) }
        binding.btnPaymaya.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYMAYA) }
        binding.btnBdo.setOnClickListener     { viewModel.selectMethod(PaymentDetailsViewModel.Method.BDO) }
        binding.btnPaypal.setOnClickListener  { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYPAL) }
        binding.btnPay.setOnClickListener     { initiatePayMongoPayment() }
    }

    private fun observeViewModel() {
        viewModel.selectedMethod.observe(this) { method ->
            val price = viewModel.planPrice.value?.toInt() ?: 0
            val label = when (method) {
                PaymentDetailsViewModel.Method.GCASH    -> "GCash"
                PaymentDetailsViewModel.Method.PAYMAYA  -> "Maya"
                PaymentDetailsViewModel.Method.BDO      -> "BDO"
                PaymentDetailsViewModel.Method.PAYPAL   -> "PayPal"
            }
            binding.btnPay.text = "Pay ₱$price via $label"

            val active   = ContextCompat.getColor(this, R.color.login_accent)
            val inactive = ContextCompat.getColor(this, R.color.login_input_border)
            binding.btnGcash.strokeColor    = if (method == PaymentDetailsViewModel.Method.GCASH)   active else inactive
            binding.btnPaymaya.strokeColor  = if (method == PaymentDetailsViewModel.Method.PAYMAYA) active else inactive
            binding.btnBdo.strokeColor      = if (method == PaymentDetailsViewModel.Method.BDO)     active else inactive
            binding.btnPaypal.strokeColor   = if (method == PaymentDetailsViewModel.Method.PAYPAL)  active else inactive
        }

        viewModel.navigateBack.observe(this) { shouldBack ->
            if (shouldBack) { finish(); viewModel.doneBack() }
        }
    }

    // ── PayMongo payment initiation ───────────────────────────────────────────
    private fun initiatePayMongoPayment() {
        val price  = viewModel.planPrice.value ?: return
        val method = viewModel.selectedMethod.value ?: return
        setLoadingState(true)

        lifecycleScope.launch {
            val description = "AgriHive $planName Plan"
            val result: PayMongoService.Result = when (method) {
                PaymentDetailsViewModel.Method.GCASH ->
                    // Use Payment Link — works for GCash without needing redirect URL whitelisting
                    PayMongoService.createPaymentLink(price, description)
                PaymentDetailsViewModel.Method.PAYMAYA ->
                    // Use Payment Link — works for Maya without needing redirect URL whitelisting
                    PayMongoService.createPaymentLink(price, description)
                else ->
                    // BDO / PayPal → also use generic PayMongo payment link
                    PayMongoService.createPaymentLink(price, description)
            }

            setLoadingState(false)

            when (result) {
                is PayMongoService.Result.Success -> openCheckout(result.checkoutUrl)
                is PayMongoService.Result.Error   -> {
                    Toast.makeText(this@PaymentDetailsActivity,
                        "Payment error: ${result.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openCheckout(url: String) {
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, Uri.parse(url))
        } catch (e: Exception) {
            // Fallback to browser
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    // ── Payment confirmed ─────────────────────────────────────────────────────
    private fun onPaymentSuccess(referenceId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val price = viewModel.planPrice.value ?: 0.0

        // Save subscription record to Firestore
        val now = System.currentTimeMillis()
        val dueDate = now + (90L * 24 * 60 * 60 * 1000) // +90 days

        val subData = hashMapOf(
            "userId"      to uid,
            "plan"        to planName,
            "price"       to price,
            "referenceId" to referenceId,
            "status"      to "active",
            "pending"     to false,
            "purchased"   to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(now)),
            "due"         to java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(dueDate)),
            "paymentMethod" to "PayMongo",
            "createdAt"   to now
        )

        FirebaseFirestore.getInstance()
            .collection("subscriptions")
            .document(uid)
            .set(subData, SetOptions.merge())

        // Also log to activity_logs for the web admin panel
        FirebaseFirestore.getInstance()
            .collection("activity_logs")
            .add(mapOf(
                "action"    to "Payment Processed",
                "user"      to uid,
                "target"    to "$planName — ₱${price.toInt()}",
                "status"    to "Success",
                "timestamp" to com.google.firebase.Timestamp.now()
            ))

        startActivity(Intent(this, PaymentSuccessActivity::class.java).apply {
            putExtra("PLAN_NAME", planName)
            putExtra("PLAN_PRICE", price)
            putExtra("REFERENCE_ID", referenceId)
        })
        finish()
    }

    private fun setLoadingState(loading: Boolean) {
        binding.btnPay.isEnabled = !loading
        binding.btnPay.text = if (loading) "Processing…" else
            "Pay ₱${viewModel.planPrice.value?.toInt()} via ${
                when (viewModel.selectedMethod.value) {
                    PaymentDetailsViewModel.Method.GCASH   -> "GCash"
                    PaymentDetailsViewModel.Method.PAYMAYA -> "Maya"
                    PaymentDetailsViewModel.Method.BDO     -> "BDO"
                    PaymentDetailsViewModel.Method.PAYPAL  -> "PayPal"
                    null -> ""
                }
            }"
        binding.progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }

    companion object {
        const val EXTRA_PLAN_NAME      = "PLAN_NAME"
        const val EXTRA_PLAN_PRICE     = "PLAN_PRICE"
        const val EXTRA_PAYMENT_METHOD = "PAYMENT_METHOD"
    }
}
