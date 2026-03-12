package com.detrapay.ui.order_details

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus.CANCELLED
import com.detrapay.data.model.OrderReceivableItemStatus.REFUNDED
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.canBeDeleted
import com.detrapay.databinding.ActivityOrderDetailsBinding
import com.detrapay.ui.home.HomeActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import com.detrapay.ui.util.PaymentTypeRules
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
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
    private var isLoadingPaymentMethods = false

    private val viewModel: OrderDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderParam = getOrderFromIntent()
        orderId = orderParam?.id ?: intent.getIntExtra("orderId", -1)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (orderId <= 0) {
            Toast.makeText(this, getString(R.string.order_details_invalid_order), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val isSuccess = intent.getBooleanExtra("isSuccess", false)
        if (isSuccess && orderParam != null) {
            showSuccess(orderId)
        } else {
            viewModel.loadScreenContent(orderId)
        }

        setupHeaderActions()
        setupObservers()
        setupErrorBtn()
        setupSuccessActions()
        setupFinishAction()
        setupAddPaymentAction()
        setupBottomSheetListeners()
        setupBackNavigation()
    }

    override fun onSupportNavigateUp(): Boolean {
        handleExitRequest()
        return true
    }

    private fun showSuccess(orderId: Int) {
        binding.successView.visibility = View.VISIBLE
        binding.contentView.visibility = View.GONE
        binding.tvSuccessMessage.text = getString(R.string.order_details_success_message, orderId)
    }

    private fun setupSuccessActions() {
        binding.btnNewOrder.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnPrintReceipt.setOnClickListener {
            Toast.makeText(this, getString(R.string.order_details_printing_receipt), Toast.LENGTH_SHORT).show()
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
                            binding.errorTxtView.text = getString(R.string.order_details_load_error)
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
                is UIState.Loading -> {
                    isLoadingPaymentMethods = true
                    binding.addPaymentButton.isEnabled = false
                }
                is UIState.Success -> {
                    isLoadingPaymentMethods = false
                    binding.addPaymentButton.isEnabled = true
                    val methods = status.data.orEmpty()
                    if (methods.isEmpty()) {
                        Toast.makeText(
                            this,
                            getString(R.string.order_details_add_payment_empty_methods),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showPaymentMethodPicker()
                    }
                }
                is UIState.Error -> {
                    isLoadingPaymentMethods = false
                    binding.addPaymentButton.isEnabled = true
                    Toast.makeText(
                        this,
                        status.message ?: getString(R.string.order_details_add_payment_load_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is UIState.Idle -> {
                    isLoadingPaymentMethods = false
                    binding.addPaymentButton.isEnabled = true
                }
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
                                getString(R.string.order_details_pix_pending_confirmation),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (paymentData != null) {
                            viewModel.loadScreenContent(orderId)
                            Toast.makeText(
                                this@OrderDetailsActivity,
                                getString(R.string.order_details_payment_success_toast),
                                Toast.LENGTH_SHORT
                            ).show()
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
            DebugConstants.DEBUG_SPLIT_DEVICE_ID
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

        binding.orderTitleTextView.text = "Pedido #${order.id}"
        binding.orderCaptionTextView.text = getString(R.string.order_details_payments_subtitle)
        binding.vehicleValueValue.text = "R$ $originalAmount"

        binding.saveSalesmanButton.visibility = View.GONE
    }

    private fun setupFinishAction() {
        binding.finishServiceBtn.setOnClickListener {
            handleExitRequest()
        }
    }

    private fun setupAddPaymentAction() {
        binding.addPaymentButton.setOnClickListener {
            if (isLoadingPaymentMethods || hasDialogWithTag(PAYMENT_METHOD_PICKER_TAG)) {
                return@setOnClickListener
            }
            viewModel.loadPaymentMethods()
        }
    }

    private fun setupBottomSheetListeners() {
        supportFragmentManager.setFragmentResultListener(
            OrderDetailsPaymentMethodPickerBottomSheet.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val selectedType = bundle.getString(OrderDetailsPaymentMethodPickerBottomSheet.RESULT_PAYMENT_TYPE)
                ?: return@setFragmentResultListener
            openPaymentConfig(selectedType)
        }
        supportFragmentManager.setFragmentResultListener(
            OrderDetailsPaymentConfigBottomSheet.REQUEST_PENDING_ADDED,
            this,
        ) { _, _ ->
            showPendingAdditionSuccess = true
        }
        supportFragmentManager.setFragmentResultListener(
            OrderDetailsPaymentConfigBottomSheet.REQUEST_MANUAL_PAYMENT_CONFIRMED,
            this,
        ) { _, bundle ->
            confirmSelectedManualPayment(
                bundle.getDouble(OrderDetailsPaymentConfigBottomSheet.RESULT_AMOUNT)
            )
        }
    }

    private fun showPaymentMethodPicker() {
        if (hasDialogWithTag(PAYMENT_METHOD_PICKER_TAG)) {
            return
        }
        val types = viewModel.availablePaymentTypes()
        if (types.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.order_details_add_payment_empty_methods),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        OrderDetailsPaymentMethodPickerBottomSheet.newInstance(types)
            .show(supportFragmentManager, PAYMENT_METHOD_PICKER_TAG)
    }

    private fun openPaymentConfig(type: String) {
        if (hasDialogWithTag(PAYMENT_CONFIG_TAG)) {
            return
        }
        OrderDetailsPaymentConfigBottomSheet.newInstance(
            paymentType = type,
            defaultAmount = defaultPendingAmount(),
        ).show(supportFragmentManager, PAYMENT_CONFIG_TAG)
    }

    private fun openManualPaymentConfig(receivable: OrderReceivableItem) {
        if (hasDialogWithTag(PAYMENT_CONFIG_TAG)) {
            return
        }
        OrderDetailsPaymentConfigBottomSheet.newManualConfirmationInstance(
            paymentType = resolvedPaymentType(receivable),
            defaultAmount = receivable.amountOriginal,
        ).show(supportFragmentManager, PAYMENT_CONFIG_TAG)
    }

    private fun hasDialogWithTag(tag: String): Boolean {
        val fragment = supportFragmentManager.findFragmentByTag(tag)
        return (fragment as? DialogFragment)?.dialog?.isShowing == true || fragment?.isAdded == true
    }

    private fun defaultPendingAmount(): Double {
        val order = currentOrder ?: return 0.0
        val registeredAmount = paidReceivables(order).sumOf { it.amountOriginal }
        val remaining = order.originalAmount - registeredAmount
        val suggestedValue = if (remaining > 0.0) remaining else order.originalAmount
        return suggestedValue.coerceAtLeast(0.0)
    }

    private fun setupHeaderActions() {
        binding.backButton.setOnClickListener {
            handleExitRequest()
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            handleExitRequest()
        }
    }

    private fun handleExitRequest() {
        if (binding.successView.visibility == View.VISIBLE) {
            navigateToHome()
            return
        }

        showExitConfirmationDialog()
    }

    private fun showExitConfirmationDialog() {
        val balance = currentBalance()
        val message = when {
            balance > 0.0 -> getString(
                R.string.order_details_exit_dialog_pending_message,
                formatCurrency(abs(balance))
            )
            balance < 0.0 -> getString(
                R.string.order_details_exit_dialog_excess_message,
                formatCurrency(abs(balance))
            )
            else -> getString(R.string.order_details_exit_dialog_settled_message)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.order_details_exit_dialog_title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.order_details_exit_dialog_confirm) { _, _ ->
                navigateToHome()
            }
            .show()
    }

    private fun currentBalance(): Double {
        val order = currentOrder ?: return 0.0
        val totalReceived = paidReceivables(order).sumOf(::receivedAmountForSummary)
        return order.originalAmount - totalReceived
    }

    private fun setupPayments(order: Order) {
        paymentsAdapter = OrderDetailsPaymentsRecyclerViewAdapter(order.receivables, this)
        val recyclerView: RecyclerView = binding.receivablesRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = paymentsAdapter
        binding.emptyPaymentsContainer.visibility =
            if (order.receivables.isEmpty()) View.VISIBLE else View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun setupBalanceSummary(order: Order) {
        val totalReceived = paidReceivables(order).sumOf(::receivedAmountForSummary)
        val balance = currentBalance()

        binding.registeredAmountLabelTextView.setText(R.string.order_details_received_amount)
        binding.registeredAmountValueTextView.text = formatCurrency(totalReceived)

        val (labelRes, colorRes, displayAmount) = when {
            balance > 0 -> Triple(
                R.string.order_details_balance_pending,
                R.color.primary_500,
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
        binding.balanceStatusIcon.setImageResource(
            when {
                balance > 0 -> R.drawable.ic_status_overdue_small
                balance < 0 -> R.drawable.ic_status_pending_small
                else -> R.drawable.ic_status_paid_small
            }
        )
        binding.balanceHintTextView.text = when {
            balance > 0 -> getString(R.string.order_details_pending_message)
            balance < 0 -> getString(R.string.order_details_excess_amount_hint)
            else -> getString(R.string.order_details_settled_message)
        }
        binding.balanceHintTextView.visibility = View.VISIBLE
        binding.finishServiceBtn.text = when {
            balance > 0 -> getString(R.string.order_details_finish_pending_cta)
            balance < 0 -> getString(R.string.order_details_finish_excess_cta)
            else -> getString(R.string.order_details_finish_settled_cta)
        }
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
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(supportFragmentManager, error)
        }
    }

    override fun onItemClick(receivable: OrderReceivableItem) {
        selectedReceivable = receivable
        if (isManualPayment(receivable)) {
            openManualPaymentConfig(receivable)
        } else {
            openPaymentDialog()
        }
    }

    private fun confirmSelectedManualPayment(amountFinal: Double) {
        val receivable = selectedReceivable ?: return
        if (!isManualPayment(receivable)) {
            openPaymentDialog()
            return
        }
        viewModel.payOrder(receivable, buildManualPaymentData(amountFinal))
    }

    private fun buildManualPaymentData(amountFinal: Double): PaymentData {
        val now = Date()
        return PaymentData(
            date = SimpleDateFormat("dd/MM/yyyy", locale).format(now),
            time = SimpleDateFormat("HH:mm:ss", locale).format(now),
            amountFinal = amountFinal,
        )
    }

    private fun paidReceivables(order: Order): List<OrderReceivableItem> {
        return order.receivables.filter { it.status == com.detrapay.data.model.OrderReceivableItemStatus.PAID }
    }

    private fun receivedAmountForSummary(receivable: OrderReceivableItem): Double {
        return when (resolvedPaymentType(receivable)) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CASH,
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT -> receivable.amountFinal
            else -> receivable.amountOriginal
        }
    }

    private fun isManualPayment(receivable: OrderReceivableItem): Boolean {
        return PaymentTypeRules.isDirectNoFeePaymentType(resolvedPaymentType(receivable))
    }

    private fun resolvedPaymentType(receivable: OrderReceivableItem): String {
        val normalizedType = normalizePaymentType(receivable.paymentMethod.paymentType)
        if (normalizedType.isNotBlank()) {
            return normalizedType
        }
        return normalizePaymentType(receivable.paymentMethod.name)
    }

    private fun normalizePaymentType(rawType: String?): String {
        return PaymentTypeRules.normalize(rawType)
    }

    override fun onDeletePendingClick(receivable: OrderReceivableItem) {
        if (!receivable.canBeDeleted()) {
            Toast.makeText(this, getString(R.string.order_details_delete_not_allowed), Toast.LENGTH_SHORT).show()
            return
        }

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

    companion object {
        private const val PAYMENT_METHOD_PICKER_TAG = "OrderDetailsPaymentMethodPickerBottomSheet"
        private const val PAYMENT_CONFIG_TAG = "OrderDetailsPaymentConfigBottomSheet"
    }
}
