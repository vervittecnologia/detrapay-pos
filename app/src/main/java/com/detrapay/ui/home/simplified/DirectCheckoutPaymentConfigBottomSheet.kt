package com.detrapay.ui.home.simplified

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.BottomSheetRegistrationPaymentConfigBinding
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.registration.payment_method.InstallmentsAdapter
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.PaymentTypeRules
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class DirectCheckoutPaymentConfigBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: SimplifiedReceivableListViewModel by viewModels({ requireParentFragment() })
    private var _binding: BottomSheetRegistrationPaymentConfigBinding? = null
    private val binding get() = _binding!!
    private val locale = Locale("pt", "BR")

    private var paymentType: String = OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT
    private var defaultAmount: Double = 0.0
    private var selectedInstallment: InstallmentFee? = null
    private var installmentsAdapter: InstallmentsAdapter? = null

    override fun getTheme(): Int = com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentType = requireArguments().getString(ARG_PAYMENT_TYPE).orEmpty()
        defaultAmount = requireArguments().getDouble(ARG_DEFAULT_AMOUNT, 0.0)
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
        val isCredit = isCreditPayment()
        binding.tvSheetTitle.setText(R.string.order_details_payment_config_sheet_title)
        binding.tvSheetSubtitle.text = getString(
            R.string.direct_checkout_pending_balance,
            formatCurrency(defaultAmount),
        )
        binding.tvAmountLabel.setText(R.string.order_details_payment_config_amount_label)

        binding.tvBrandLabel.isVisible = false
        binding.toggleGroupBrand.isVisible = false
        binding.groupCreditSection.isVisible = isCredit
        binding.cardSimpleSummary.isVisible = !isCredit
        binding.rvInstallments.isVisible = false
        binding.btnReset.isVisible = false
        binding.tvActionHint.isVisible = false

        binding.btnPrimaryAction.isVisible = isCredit
        binding.btnPrimaryAction.text = getString(R.string.direct_checkout_view_installments)
        binding.btnPrimaryAction.isEnabled = false
        updateButtonVisualState(binding.btnPrimaryAction, false)

        binding.btnConfirm.isVisible = true
        binding.btnConfirm.setText(R.string.order_details_payment_config_confirm)
        binding.btnConfirm.isEnabled = !isCredit
        updateButtonVisualState(binding.btnConfirm, !isCredit)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearValue.setOnClickListener { binding.etPaymentValue.setText("0") }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.etPaymentValue.setOnEditorActionListener { _, actionId, _ ->
            if (isCredit &&
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
            if (isCredit) {
                resetCreditInstallmentsState()
            }
            updateAmountDependentUi()
        })

        if (isCredit) {
            binding.btnPrimaryAction.setOnClickListener { requestInstallments() }
        } else {
            renderSimpleSummary()
        }

        binding.btnConfirm.setOnClickListener { confirmPaymentConfig() }
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
                        showError(getString(R.string.direct_checkout_installments_error))
                        return@observe
                    }
                    showInstallments(installments)
                }
                is UIState.Error -> {
                    binding.loadingOverlay.isVisible = false
                    updateAmountDependentUi()
                    showError(state.message ?: getString(R.string.direct_checkout_installments_error))
                }
                is UIState.Idle -> {
                    binding.loadingOverlay.isVisible = false
                }
            }
        }
    }

    private fun preloadAmount() {
        if (defaultAmount > 0) {
            binding.etPaymentValue.setText(Math.round(defaultAmount * 100).toString())
        }
        focusAmountInput()
        updateAmountDependentUi()
    }

    private fun requestInstallments() {
        hideKeyboard()
        val amount = currentAmount()
        if (amount <= 0.0) {
            showError(getString(R.string.order_details_add_payment_invalid_amount))
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

        if (isCreditPayment()) {
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
        binding.tvSummaryTitle.setText(R.string.order_details_payment_config_summary_title)
        binding.tvSummaryLine1Label.setText(R.string.order_details_payment_config_summary_value)
        binding.tvSummaryLine1Value.text = formatCurrency(amount)
        binding.tvSummaryLine2Label.setText(R.string.order_details_payment_config_summary_fee)
        binding.tvSummaryLine2Value.text = formatCurrency(0.0)
        binding.tvSummaryLine3Label.setText(R.string.order_details_payment_config_summary_total)
        binding.tvSummaryLine3Value.text = formatCurrency(amount)
    }

    private fun confirmPaymentConfig() {
        val amount = currentAmount()
        if (amount <= 0.0) {
            showError(getString(R.string.order_details_add_payment_invalid_amount))
            return
        }

        val installments = if (isCreditPayment()) {
            selectedInstallment?.installmentNumber ?: run {
                showError(getString(R.string.direct_checkout_select_installment))
                return
            }
        } else {
            1
        }

        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(
                RESULT_PAYMENT_TYPE to paymentType,
                RESULT_AMOUNT to amount,
                RESULT_INSTALLMENTS to installments,
            ),
        )
        dismiss()
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

    private fun isCreditPayment(): Boolean {
        return PaymentTypeRules.normalize(paymentType) == OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT
    }

    private fun formatCurrency(value: Double): String = "R$ %,.2f".format(locale, value)

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
        const val REQUEST_KEY = "direct_checkout_payment_config_request"
        const val RESULT_PAYMENT_TYPE = "payment_type"
        const val RESULT_AMOUNT = "amount"
        const val RESULT_INSTALLMENTS = "installments"

        private const val ARG_PAYMENT_TYPE = "paymentType"
        private const val ARG_DEFAULT_AMOUNT = "defaultAmount"

        fun newInstance(
            paymentType: String,
            defaultAmount: Double,
        ): DirectCheckoutPaymentConfigBottomSheet {
            return DirectCheckoutPaymentConfigBottomSheet().apply {
                arguments = bundleOf(
                    ARG_PAYMENT_TYPE to paymentType,
                    ARG_DEFAULT_AMOUNT to defaultAmount,
                )
            }
        }
    }
}
