package com.detrapay.ui.order_details

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.BottomSheetRegistrationPaymentConfigBinding
import com.detrapay.ui.registration.payment_method.InstallmentsAdapter
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.PaymentTypeRules
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

private enum class OrderDetailsPaymentMode {
    CREDIT,
    SIMPLE_DIRECT,
}

private enum class OrderDetailsPaymentAction {
    ADD_PENDING,
    CONFIRM_MANUAL,
}

class OrderDetailsPaymentConfigBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: OrderDetailsViewModel by activityViewModels()
    private var _binding: BottomSheetRegistrationPaymentConfigBinding? = null
    private val binding get() = _binding!!
    private val locale = Locale("pt", "BR")

    private var paymentType: String = OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT
    private var mode: OrderDetailsPaymentMode = OrderDetailsPaymentMode.SIMPLE_DIRECT
    private var action: OrderDetailsPaymentAction = OrderDetailsPaymentAction.ADD_PENDING
    private var defaultAmount: Double = 0.0

    private var selectedInstallment: InstallmentFee? = null
    private var installmentsAdapter: InstallmentsAdapter? = null

    override fun getTheme(): Int = com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentType = requireArguments().getString(ARG_PAYMENT_TYPE).orEmpty()
        defaultAmount = requireArguments().getDouble(ARG_DEFAULT_AMOUNT, 0.0)
        action = OrderDetailsPaymentAction.valueOf(
            requireArguments().getString(ARG_ACTION) ?: OrderDetailsPaymentAction.ADD_PENDING.name
        )
        mode = if (PaymentTypeRules.normalize(paymentType) == OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT) {
            OrderDetailsPaymentMode.CREDIT
        } else {
            OrderDetailsPaymentMode.SIMPLE_DIRECT
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.behavior.isDraggable = false
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE,
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = BottomSheetRegistrationPaymentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clearFeesState()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupUi()
        setupObservers()
        preloadAmount()
    }

    private fun setupAdapter() {
        installmentsAdapter = InstallmentsAdapter(showRadioButton = true) { fee ->
            selectedInstallment = fee
            binding.btnConfirm.isEnabled = true
            updateButtonVisualState(binding.btnConfirm, true)
        }
        binding.rvInstallments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstallments.adapter = installmentsAdapter
    }

    private fun setupUi() {
        val isManualConfirmation = action == OrderDetailsPaymentAction.CONFIRM_MANUAL
        binding.tvSheetTitle.text = if (isManualConfirmation) {
            getString(R.string.order_details_manual_payment_sheet_title)
        } else {
            getString(R.string.order_details_payment_config_sheet_title)
        }
        binding.tvSheetSubtitle.text = paymentTypeSubtitle(paymentType)
        binding.tvAmountLabel.text = if (isManualConfirmation) {
            getString(R.string.order_details_manual_payment_amount_label)
        } else {
            getString(R.string.order_details_payment_config_amount_label)
        }

        val isCredit = mode == OrderDetailsPaymentMode.CREDIT
        binding.tvBrandLabel.isVisible = false
        binding.toggleGroupBrand.isVisible = false
        binding.groupCreditSection.isVisible = isCredit
        binding.cardSimpleSummary.isVisible = !isCredit
        binding.rvInstallments.isVisible = false
        binding.btnReset.isVisible = false
        binding.tvActionHint.isVisible = false

        binding.btnPrimaryAction.isVisible = isCredit
        binding.btnPrimaryAction.text = "Ver parcelas"
        binding.btnPrimaryAction.isEnabled = false
        updateButtonVisualState(binding.btnPrimaryAction, false)

        binding.btnConfirm.isVisible = true
        binding.btnConfirm.text = if (isManualConfirmation) {
            getString(R.string.order_details_pay_now)
        } else {
            getString(R.string.order_details_payment_config_confirm)
        }
        binding.btnConfirm.isEnabled = !isCredit
        updateButtonVisualState(binding.btnConfirm, !isCredit)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearValue.setOnClickListener { binding.etPaymentValue.setText("0") }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.etPaymentValue.setOnEditorActionListener { _, actionId, _ ->
            if (mode == OrderDetailsPaymentMode.CREDIT &&
                (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE)
            ) {
                if (binding.btnPrimaryAction.isEnabled) {
                    requestInstallments()
                }
                true
            } else {
                false
            }
        }
        binding.etPaymentValue.addTextChangedListener(Mask.moneyMask(binding.etPaymentValue) { _ ->
            if (mode == OrderDetailsPaymentMode.CREDIT) {
                resetCreditInstallmentsState()
            }
            updateAmountDependentUi()
        })

        if (isCredit) {
            binding.btnPrimaryAction.setOnClickListener { requestInstallments() }
        } else {
            renderSimpleSummary()
        }

        binding.btnConfirm.setOnClickListener { confirmPayment() }
    }

    private fun setupObservers() {
        viewModel.calculateFeesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.loadingOverlay.isVisible = true
                    binding.btnPrimaryAction.isEnabled = false
                    updateButtonVisualState(binding.btnPrimaryAction, false)
                }

                is UIState.Success -> {
                    binding.loadingOverlay.isVisible = false
                    updateAmountDependentUi()
                    val installments = state.data?.data.orEmpty().firstOrNull()?.installments.orEmpty()
                    if (installments.isEmpty()) {
                        showError("Nao foi possivel obter as parcelas para esse pagamento.")
                        return@observe
                    }
                    showInstallments(installments)
                }

                is UIState.Error -> {
                    binding.loadingOverlay.isVisible = false
                    updateAmountDependentUi()
                    showError(state.message ?: "Nao foi possivel calcular as parcelas.")
                }

                is UIState.Idle -> {
                    binding.loadingOverlay.isVisible = false
                }
            }
        }
    }

    private fun preloadAmount() {
        if (defaultAmount > 0) {
            val normalized = Math.round(defaultAmount * 100).toString()
            binding.etPaymentValue.setText(normalized)
        }
        focusAmountInput()
        updateAmountDependentUi()
    }

    private fun requestInstallments() {
        hideKeyboard()
        val amount = currentAmount()
        if (amount <= 0.0) {
            showError("Informe um valor maior que zero.")
            return
        }
        selectedInstallment = null
        binding.btnConfirm.isEnabled = false
        updateButtonVisualState(binding.btnConfirm, false)
        viewModel.calculateFees(amount, paymentType)
    }

    private fun showInstallments(fees: List<InstallmentFee>) {
        selectedInstallment = null
        binding.rvInstallments.isVisible = true
        binding.groupCreditSection.isVisible = true
        binding.btnPrimaryAction.isVisible = false
        binding.btnConfirm.isVisible = true
        binding.btnConfirm.isEnabled = false
        updateButtonVisualState(binding.btnConfirm, false)
        installmentsAdapter?.submitList(fees)
        installmentsAdapter?.clearSelection()
    }

    private fun updateAmountDependentUi() {
        val amount = currentAmount()
        binding.btnClearValue.isVisible = amount > 0.0

        if (mode == OrderDetailsPaymentMode.CREDIT) {
            val canCalculate = amount > 0.0
            if (!binding.rvInstallments.isVisible) {
                binding.btnPrimaryAction.isVisible = true
            }
            binding.btnPrimaryAction.isEnabled = canCalculate
            updateButtonVisualState(binding.btnPrimaryAction, canCalculate)
            return
        }

        binding.btnConfirm.isEnabled = amount > 0.0
        updateButtonVisualState(binding.btnConfirm, amount > 0.0)
        renderSimpleSummary()
    }

    private fun renderSimpleSummary() {
        val amount = currentAmount()
        binding.cardSimpleSummary.isVisible = true
        binding.tvSummaryTitle.text = getString(R.string.order_details_payment_config_summary_title)
        binding.tvSummaryLine1Label.text = getString(R.string.order_details_payment_config_summary_value)
        binding.tvSummaryLine1Value.text = formatCurrency(amount)
        binding.tvSummaryLine2Label.text = getString(R.string.order_details_payment_config_summary_fee)
        binding.tvSummaryLine2Value.text = formatCurrency(0.0)
        binding.tvSummaryLine3Label.text = getString(R.string.order_details_payment_config_summary_total)
        binding.tvSummaryLine3Value.text = formatCurrency(amount)
    }

    private fun confirmPayment() {
        val amount = currentAmount()
        if (amount <= 0.0) {
            showError("Informe um valor maior que zero.")
            return
        }

        if (mode == OrderDetailsPaymentMode.CREDIT) {
            val installment = selectedInstallment ?: run {
                showError("Selecione uma parcela para continuar.")
                return
            }
            val method = viewModel.resolvePaymentMethod(paymentType, installment.installmentNumber)
            if (method == null) {
                showError("Metodo de pagamento indisponivel para a parcela selecionada.")
                return
            }
            submitPendingPayment(method, amount)
            return
        }

        val method = viewModel.resolvePaymentMethod(paymentType, 1)
        if (method == null) {
            showError("Metodo de pagamento indisponivel.")
            return
        }
        if (action == OrderDetailsPaymentAction.CONFIRM_MANUAL) {
            confirmManualPayment(amount)
            return
        }
        submitPendingPayment(method, amount)
    }

    private fun confirmManualPayment(amount: Double) {
        parentFragmentManager.setFragmentResult(
            REQUEST_MANUAL_PAYMENT_CONFIRMED,
            bundleOf(RESULT_AMOUNT to amount)
        )
        dismiss()
    }

    private fun submitPendingPayment(paymentMethod: PaymentMethod, amount: Double) {
        parentFragmentManager.setFragmentResult(REQUEST_PENDING_ADDED, bundleOf())
        dismiss()
        
        viewModel.addPendingReceivable(paymentMethod, amount)
    }

    private fun resetCreditInstallmentsState() {
        selectedInstallment = null
        binding.rvInstallments.isVisible = false
        binding.btnPrimaryAction.isVisible = true
        binding.btnConfirm.isEnabled = false
        updateButtonVisualState(binding.btnConfirm, false)
        installmentsAdapter?.clearSelection()
    }

    private fun currentAmount(): Double = Mask.doubleValue(binding.etPaymentValue.text.toString())

    private fun formatCurrency(value: Double): String = "R$ %,.2f".format(locale, value)

    private fun paymentTypeSubtitle(type: String): String {
        return when (PaymentTypeRules.normalize(type)) {
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> "Cartao de credito"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> "Cartao de debito"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_PIX -> "Pix"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CASH -> "Dinheiro"
            OrderDetailsPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT -> "Credito loja"
            else -> "Pagamento"
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.etPaymentValue.clearFocus()
    }

    private fun focusAmountInput() {
        binding.etPaymentValue.post {
            binding.etPaymentValue.requestFocus()
            binding.etPaymentValue.setSelectAllOnFocus(true)
            binding.etPaymentValue.text?.let { text ->
                binding.etPaymentValue.setSelection(0, text.length)
            }
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.etPaymentValue, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateButtonVisualState(
        button: com.google.android.material.button.MaterialButton,
        isEnabled: Boolean,
    ) {
        val backgroundColor = if (isEnabled) {
            ContextCompat.getColor(requireContext(), R.color.primary_500)
        } else {
            ContextCompat.getColor(requireContext(), R.color.neutral_300)
        }
        val textColor = if (isEnabled) {
            ContextCompat.getColor(requireContext(), R.color.white)
        } else {
            ContextCompat.getColor(requireContext(), R.color.neutral_500)
        }
        button.alpha = 1.0f
        button.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        button.setTextColor(textColor)
    }

    companion object {
        const val REQUEST_PENDING_ADDED = "order_details_pending_added_request"
        const val REQUEST_MANUAL_PAYMENT_CONFIRMED = "order_details_manual_payment_confirmed_request"
        const val RESULT_AMOUNT = "resultAmount"
        private const val ARG_PAYMENT_TYPE = "paymentType"
        private const val ARG_DEFAULT_AMOUNT = "defaultAmount"
        private const val ARG_ACTION = "action"

        fun newInstance(
            paymentType: String,
            defaultAmount: Double,
            action: String = OrderDetailsPaymentAction.ADD_PENDING.name
        ): OrderDetailsPaymentConfigBottomSheet {
            return OrderDetailsPaymentConfigBottomSheet().apply {
                arguments = bundleOf(
                    ARG_PAYMENT_TYPE to paymentType,
                    ARG_DEFAULT_AMOUNT to defaultAmount,
                    ARG_ACTION to action,
                )
            }
        }

        fun newManualConfirmationInstance(
            paymentType: String,
            defaultAmount: Double
        ): OrderDetailsPaymentConfigBottomSheet {
            return newInstance(
                paymentType = paymentType,
                defaultAmount = defaultAmount,
                action = OrderDetailsPaymentAction.CONFIRM_MANUAL.name
            )
        }
    }
}
