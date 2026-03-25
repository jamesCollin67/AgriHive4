package com.example.agrihive.payment

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.agrihive.databinding.ActivityPaymentDetailsBinding

class PaymentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentDetailsBinding
    private val viewModel: PaymentDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get plan details from intent
        val planName = intent.getStringExtra("PLAN_NAME") ?: "Starter"
        val planPrice = intent.getDoubleExtra("PLAN_PRICE", 550.0)
        viewModel.setPlanDetails(planPrice)

        binding.tvPlanName.text = "AgriHive $planName Plan"
        binding.tvPlanPrice.text = "₱${planPrice.toInt()}"
        binding.tvTotalPrice.text = "₱${planPrice.toInt()}"

        setupClicks()
        observeViewModel()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { viewModel.onBackClicked() }
        binding.btnGcash.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.GCASH) }
        binding.btnPaymaya.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYMAYA) }
        binding.btnBdo.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.BDO) }
        binding.btnPaypal.setOnClickListener { viewModel.selectMethod(PaymentDetailsViewModel.Method.PAYPAL) }
        binding.btnPay.setOnClickListener { viewModel.onPayClicked() }
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

            val active = ContextCompat.getColor(this, com.example.agrihive.R.color.login_accent)
            val inactive = ContextCompat.getColor(this, com.example.agrihive.R.color.login_input_border)
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

        viewModel.navigateToSuccess.observe(this) { shouldNavigate ->
            if (shouldNavigate) {
                startActivity(Intent(this, PaymentSuccessActivity::class.java))
                viewModel.doneSuccess()
            }
        }
    }
}
