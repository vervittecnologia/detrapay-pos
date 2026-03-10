package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.OrderReceivableItemStatus.CANCELLED
import com.detrapay.data.model.OrderReceivableItemStatus.REFUNDED
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.databinding.ActivityOrderDetailsBinding
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.refund.RefundPaymentDialogFragment
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.DeviceUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class OrderDetailsActivity : AppCompatActivity(),
    OrderDetailsPaymentsRecyclerViewAdapter.OnItemClickListener {

    private lateinit var binding: ActivityOrderDetailsBinding
    private var orderParam: Order? = null
    private var orderId: Int = -1
    private var currentOrder: Order? = null
    private val locale: Locale = Locale("pt", "BR")
    private var paymentsAdapter: OrderDetailsPaymentsRecyclerViewAdapter? = null
    private var selectedReceivable: OrderReceivableItem? = null
    private var prePaymentRetryCount = 0
    private var showPendingAdditionSuccess = false
    private var showPendingDeletionSuccess = false

    private val viewModel: OrderDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderParam = getOrderFromIntent()
        orderId = orderParam?.id ?: intent.getIntExtra("orderId", -1)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (orderId <= 0) {
            Toast.makeText(this, "Pedido inválido para exibir detalhes.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isSuccess = intent.getBooleanExtra("isSuccess", false)
        if (isSuccess && orderParam != null) {
            showSuccess(orderId)
        } else {
            viewModel.loadScreenContent(orderId)
        }

        setupToolbar()
        setupObservers()
        setupErrorBtn()
        setupSuccessActions()
        setupFinishAction()
        setupAddPaymentAction()
    }

    override fun onSupportNavigateUp(): Boolean {
        navigateToHome()
        return true
    }

    private fun showSuccess(orderId: Int) {
        binding.successView.visibility = View.VISIBLE
        binding.contentView.visibility = View.GONE
        binding.tvSuccessMessage.text = "O pedido #$orderId foi finalizado com sucesso e o recibo foi enviado ao cliente."
    }

    private fun setupSuccessActions() {
        binding.btnNewOrder.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnPrintReceipt.setOnClickListener {
            // Logic for printing receipt if needed
            Toast.makeText(this, "Imprimindo comprovante...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.orderState.observe(this, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    showLoading()
                }

                is UIState.Success -> {
                    status.data?.let {
                        currentOrder = it
                        hideLoading()
                        runCatching {
                            setupOrderResume(it)
                            setupBalanceSummary(it)
                            setupPayments(it)
                            if (showPendingAdditionSuccess) {
                                Toast.makeText(
                                    this,
                                    getString(R.string.order_details_add_payment_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showPendingAdditionSuccess = false
                            }
                            if (showPendingDeletionSuccess) {
                                Toast.makeText(
                                    this,
                                    getString(R.string.order_details_delete_pending_payment_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showPendingDeletionSuccess = false
                            }
                            binding.contentView.visibility = View.VISIBLE
                        }.onFailure {
                            binding.contentView.visibility = View.GONE
                            binding.errorTxtView.text = "Ops! Nao foi possivel carregar os detalhes do pedido."
                            binding.errorView.visibility = View.VISIBLE
                        }

                    }
                }

                is UIState.Error -> {
                    showPendingAdditionSuccess = false
                    showPendingDeletionSuccess = false
                    validateErrorType(status.exception)
                    if (status.retryData != null) {
                        setupErrorBtnWithPaymentData(status.retryData)
                    } else {
                        setupErrorBtn()
                    }
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: getString(R.string.employees_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
                is UIState.Idle -> {}
            }
        })

        viewModel.paymentMethodsState.observe(this, Observer { status ->
            when (status) {
                is UIState.Success -> {
                    val methods = status.data.orEmpty()
                    if (methods.isEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.order_details_add_payment_empty_methods),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showPaymentMethodPicker(methods)
                    }
                }
                is UIState.Error -> {
                    Toast.makeText(
                        this,
                        status.message ?: getString(R.string.order_details_add_payment_load_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is UIState.Loading, is UIState.Idle -> Unit
            }
        })

    }

    private fun showLoading() {
        binding.contentView.visibility = View.GONE
        binding.errorView.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
        binding.loadingView.startShimmer()
    }

    private fun hideLoading() {
        binding.errorView.visibility = View.GONE
        binding.loadingView.apply {
            stopShimmer()
            visibility = View.GONE
        }
    }

    private fun openPaymentDialog() {
        selectedReceivable?.let {
            val paymentDialogFragment = PaymentDialogFragment(
                listener = object : PaymentDialogFragment.PaymentListener {
                    override fun onResult(paymentData: PaymentData?) {
                        if (paymentData?.pendingConfirmation == true) {
                            viewModel.loadScreenContent(orderId)
                            Toast.makeText(
                                this@OrderDetailsActivity,
                                "QR Code PIX gerado. Aguarde a confirmacao do pagamento.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (paymentData != null) {
                            viewModel.loadScreenContent(orderId)
                            Toast.makeText(this@OrderDetailsActivity, "Pagamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                orderId = orderId,
                receivableItem = it,
                serial = getSerialForPrePay()
            )
            paymentDialogFragment.show(this.supportFragmentManager, "PaymentDialogFragment")
        }
    }

    private fun setupErrorBtnWithPaymentData(retryData: Any) {
        binding.reloadOrderDetails.setOnClickListener {
            val retryDataModel = retryData as RetryDataModel
            viewModel.payOrder(retryDataModel.receivableItem, retryDataModel.paymentData)
        }
    }

    private fun getSerialForPrePay(): String {
        return if (BuildConfig.DEBUG) {
            "6001062507098048"
        } else {
            DeviceUtils.getSerialNumber()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupOrderResume(order: Order) {
        val originalAmount = "%,.2f".format(locale, order.originalAmount)

        val editOrderClickListener = View.OnClickListener {
            val reportIntent = Intent(
                this,
                OrderReportActivity::class.java
            )
            reportIntent.putExtra("order", order)

            startActivity(reportIntent)
        }
        binding.editOrderContainer.setOnClickListener(editOrderClickListener)
        binding.editOrderTxtView.setOnClickListener(editOrderClickListener)
        binding.editOrderImageView.setOnClickListener(editOrderClickListener)

        binding.orderTitleTextView.text = "Pedido ${order.id}"
        val customerInfo = runCatching {
            buildCustomerInfoLabel(order.customer.cpfCnpj, order.customer.name)
        }.getOrDefault("-")
        binding.customerInfoTextView.text = customerInfo

        binding.vehicleValueValue.text = "R$ $originalAmount"

        binding.saveSalesmanButton.visibility = View.GONE
    }

    private fun buildCustomerInfoLabel(cpfCnpj: String?, customerName: String?): String {
        val formattedDocument = formatCpfCnpj(cpfCnpj)
        val trimmedName = customerName?.trim().orEmpty()
        return if (trimmedName.isBlank()) {
            formattedDocument
        } else {
            "$formattedDocument - $trimmedName"
        }
    }

    private fun setupFinishAction() {
        binding.finishServiceBtn.setOnClickListener {
            navigateToHome()
        }
    }

    private fun setupAddPaymentAction() {
        binding.addPaymentButton.setOnClickListener {
            viewModel.loadPaymentMethods()
        }
    }

    private fun showPaymentMethodPicker(methods: List<PaymentMethod>) {
        val labels = methods.map(::paymentMethodLabel).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.order_details_add_payment_select_method)
            .setItems(labels) { _, which ->
                showAddAmountDialog(methods[which])
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddAmountDialog(paymentMethod: PaymentMethod) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.order_details_add_payment_amount_hint)
            setText(defaultAmountValue())
            setSelection(text.length)
        }
        val container = FrameLayout(this).apply {
            val horizontalPadding = (24 * resources.displayMetrics.density).toInt()
            val topPadding = (12 * resources.displayMetrics.density).toInt()
            setPadding(horizontalPadding, topPadding, horizontalPadding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                getString(
                    R.string.order_details_add_payment_amount_title,
                    paymentMethodLabel(paymentMethod)
                )
            )
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.order_details_add_payment_confirm, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = parseCurrencyInput(input.text.toString())
                if (amount <= 0.0) {
                    input.error = getString(R.string.order_details_add_payment_invalid_amount)
                    return@setOnClickListener
                }
                showPendingAdditionSuccess = true
                dialog.dismiss()
                viewModel.addPendingReceivable(paymentMethod, amount)
            }
        }
        dialog.show()
    }

    private fun defaultAmountValue(): String {
        val order = currentOrder ?: return ""
        val registeredAmount = order.receivables
            .filter { it.status != CANCELLED && it.status != REFUNDED }
            .sumOf { it.amountOriginal }
        val remaining = order.originalAmount - registeredAmount
        val suggestedValue = if (remaining > 0) remaining else order.originalAmount
        return if (suggestedValue > 0) "%,.2f".format(locale, suggestedValue) else ""
    }

    private fun parseCurrencyInput(rawValue: String): Double {
        return rawValue
            .trim()
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .replace("\\s".toRegex(), "")
            .toDoubleOrNull()
            ?: 0.0
    }

    private fun paymentMethodLabel(method: PaymentMethod): String {
        val installments = method.installments.coerceAtLeast(1)
        return if (installments > 1) {
            "${method.name} (${installments}x)"
        } else {
            method.name
        }
    }

    private fun formatCpfCnpj(cpfCnpj: String?): String {
        val document = cpfCnpj?.trim().orEmpty()
        return when (document.length) {
            11 -> "${document.substring(0, 3)}.${document.substring(3, 6)}.${document.substring(6, 9)}-${document.substring(9, 11)}"
            14 -> "${document.substring(0, 2)}.${document.substring(2, 5)}.${document.substring(5, 8)}/${document.substring(8, 12)}-${document.substring(12, 14)}"
            else -> if (document.isBlank()) "-" else document
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.navigationIcon =
            ContextCompat.getDrawable(this, R.drawable.ic_arrow_left)
        binding.toolbar.setNavigationOnClickListener {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun setupPayments(order: Order) {
        paymentsAdapter = OrderDetailsPaymentsRecyclerViewAdapter(order.receivables, this)
        val recyclerView: RecyclerView = binding.receivablesRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = paymentsAdapter
        binding.emptyPaymentsTextView.visibility =
            if (order.receivables.isEmpty()) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun setupBalanceSummary(order: Order) {
        val activeReceivables = order.receivables.filter { it.status != CANCELLED && it.status != REFUNDED }
        val registeredAmount = activeReceivables.sumOf { it.amountOriginal }
        val balance = order.originalAmount - registeredAmount

        binding.registeredAmountValueTextView.text = formatCurrency(registeredAmount)

        val (labelRes, colorRes, displayAmount) = when {
            balance > 0 -> Triple(
                R.string.order_details_balance_pending,
                R.color.orange,
                abs(balance)
            )
            balance < 0 -> Triple(
                R.string.order_details_balance_excess,
                R.color.red,
                abs(balance)
            )
            else -> Triple(
                R.string.order_details_balance_settled,
                R.color.green,
                0.0
            )
        }

        binding.balanceLabelTextView.setText(labelRes)
        binding.balanceLabelTextView.setTextColor(ContextCompat.getColor(this, colorRes))
        binding.balanceValueTextView.text = formatCurrency(displayAmount)
        binding.balanceValueTextView.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    private fun formatCurrency(value: Double): String {
        return "R$ %,.2f".format(locale, value)
    }

    private fun setupErrorBtn() {
        binding.reloadOrderDetails.setOnClickListener {
            viewModel.loadScreenContent(orderId)
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) SessionExpiredDialog().show(
            supportFragmentManager,
            "SessionExpiredDialog"
        )
    }

    override fun onItemClick(receivable: OrderReceivableItem) {
        selectedReceivable = receivable
        openPaymentDialog()
    }

    override fun onRefundClick(receivable: OrderReceivableItem) {
        val refundPaymentDialogFragment = RefundPaymentDialogFragment(listener = object : RefundPaymentDialogFragment.RefundPaymentListener {
            override fun onResult(refundPaymentData: RefundPaymentData?) {
                if (refundPaymentData != null) {
                    viewModel.refundItem(receivable, refundPaymentData)
                    Toast.makeText(this@OrderDetailsActivity, "Estorno de Pagamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@OrderDetailsActivity, "Falha ao realizar estorno", Toast.LENGTH_SHORT).show()
                }
            }
        }, receivable)
        refundPaymentDialogFragment.show(this.supportFragmentManager, "RefundPaymentDialogFragment")

    }

    override fun onDeletePendingClick(receivable: OrderReceivableItem) {
        AlertDialog.Builder(this)
            .setTitle(R.string.order_details_delete_pending_payment)
            .setMessage(R.string.order_details_delete_pending_payment_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.order_details_delete_pending_payment_confirm) { _, _ ->
                showPendingDeletionSuccess = true
                viewModel.cancelPendingItem(receivable)
            }
            .show()
    }

    private fun getOrderFromIntent(): Order? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("order", Order::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("order") as? Order
        }
    }
}
