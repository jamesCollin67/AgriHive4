package com.example.agrihive.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.example.agrihive.R
import com.example.agrihive.databinding.ActivityPaymentDetailsBinding
import com.example.agrihive.subscription.PaymentMethod
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class PaymentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentDetailsBinding
    private val viewModel: PaymentDetailsViewModel by viewModels()
    private var planNameForPayment: String = "Subscription"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        planNameForPayment = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "Subscription"
        val planPrice = intent.getDoubleExtra(EXTRA_PLAN_PRICE, 550.0)
        viewModel.setPlanDetails(planPrice)

        binding.tvPlanName.text = "AgriHive $planNameForPayment Plan"
        binding.tvPlanPrice.text = "₱${planPrice.toInt()}"
        binding.tvTotalPrice.text = "₱${planPrice.toInt()}"

        intent.getStringExtra(EXTRA_PAYMENT_METHOD)?.let { key ->
            runCatching {
                when (PaymentMethod.valueOf(key)) {
                    PaymentMethod.GCASH -> viewModel.selectMethod(PaymentDetailsViewModel.Method.GCASH)
                    PaymentMethod.MAYA -> viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYMAYA)
                    PaymentMethod.BDO -> viewModel.selectMethod(PaymentDetailsViewModel.Method.BDO)
                    PaymentMethod.PAYPAL -> viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYPAL)
                }
            }
        }

        setupClicks()
        observeViewModel()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { viewModel.onBackClicked() }
        binding.btnGcash.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.GCASH) }
        binding.btnPaymaya.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYMAYA) }
        binding.btnBdo.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.BDO) }
        binding.btnPaypal.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYPAL) }
        binding.btnPay.setOnClickListener { launchCheckoutFlow() }
    }

    private fun launchCheckoutFlow() {
        val price = viewModel.planPrice.value ?: return
        val method = when (viewModel.selectedMethod.value) {
            PaymentDetailsViewModel.Method.GCASH -> PaymentMethod.GCASH
            PaymentDetailsViewModel.Method.PAYMAYA -> PaymentMethod.MAYA
            PaymentDetailsViewModel.Method.BDO -> PaymentMethod.BDO
            PaymentDetailsViewModel.Method.PAYPAL -> PaymentMethod.PAYPAL
            null -> PaymentMethod.GCASH
        }
        val request = PaymentRequest(
            amount = price,
            description = "AgriHive $planNameForPayment",
            orderId = "AGH_${UUID.randomUUID()}"
        )
        val service = PaymentServiceFactory.getPaymentService(method)
        val payIntent = service.getPaymentIntent(this, request)
        if (payIntent != null && payIntent.resolveActivity(packageManager) != null) {
            try {
                startActivity(payIntent)
                Toast.makeText(this, getString(R.string.payment_open_app_instruction), Toast.LENGTH_LONG).show()
                showAfterPaymentDialog()
                return
            } catch (_: Exception) { }
        }
        val webUrl = getString(R.string.payment_web_checkout_base_url).trim()
        if (webUrl.startsWith("http")) {
            try {
                val uri = Uri.parse(webUrl).buildUpon()
                    .appendQueryParameter("amount", price.toString())
                    .appendQueryParameter("plan", planNameForPayment)
                    .appendQueryParameter("orderId", request.orderId)
                    .appendQueryParameter("method", method.name)
                    .build()
                CustomTabsIntent.Builder().build().launchUrl(this, uri)
                Toast.makeText(this, getString(R.string.payment_web_checkout_opened), Toast.LENGTH_LONG).show()
                showAfterPaymentDialog()
                return
            } catch (_: Exception) { }
        }
        Toast.makeText(
            this,
            PaymentServiceFactory.getAppNotInstalledMessage(method),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showAfterPaymentDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.payment_completed_title))
            .setMessage(getString(R.string.payment_completed_message))
            .setPositiveButton(getString(R.string.payment_completed_yes)) { _, _ ->
                startActivity(Intent(this, PaymentSuccessActivity::class.java))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun observeViewModel() {
        viewModel.selectedMethod.observe(this) { method ->
            val price = viewModel.planPrice.value?.toInt() ?: 0
            val methodName = when (method) {
                PaymentDetailsViewModel.Method.GCASH -> "GCash"
                PaymentDetailsViewModel.Method.PAYMAYA -> "PayMaya"
                PaymentDetailsViewModel.Method.BDO -> "BDO"
                PaymentDetailsViewModel.Method.PAYPAL -> "PayPal"
            }
            binding.btnPay.text = "Pay ₱$price via $methodName"

            val active = ContextCompat.getColor(this, R.color.login_accent)
            val inactive = ContextCompat.getColor(this, R.color.login_input_border)
            binding.btnGcash.strokeColor = if (method == PaymentDetailsViewModel.Method.GCASH) active else inactive
            binding.btnPaymaya.strokeColor = if (method == PaymentDetailsViewModel.Method.PAYMAYA) active else inactive
            binding.btnBdo.strokeColor = if (method == PaymentDetailsViewModel.Method.BDO) active else inactive
            binding.btnPaypal.strokeColor = if (method == PaymentDetailsViewModel.Method.PAYPAL) active else inactive
        }

        viewModel.navigateBack.observe(this) { shouldBack ->
            if (shouldBack) {
                finish()
                viewModel.doneBack()
            }
        }
    }

    companion object {
        const val EXTRA_PLAN_NAME = "PLAN_NAME"
        const val EXTRA_PLAN_PRICE = "PLAN_PRICE"
        const val EXTRA_PAYMENT_METHOD = "PAYMENT_METHOD"
    }
}
