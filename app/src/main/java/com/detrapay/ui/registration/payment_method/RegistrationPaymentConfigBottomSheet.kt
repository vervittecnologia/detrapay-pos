package com.detrapay.ui.registration.payment_method

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.BottomSheetRegistrationPaymentConfigBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask
import com.detrapay.ui.util.PaymentTypeRules
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale
import kotlin.math.abs

private enum class PaymentConfigMode {
    CREDIT,
    DEBIT,
    SIMPLE_QUOTE,
    DIRECT,
}

private data class SimplePaymentQuote(
    val amountOriginal: Double,
    val feeAmount: Double,
    val amountFinal: Double,
    val paymentMethod: PaymentMethod,
)

class RegistrationPaymentConfigBottomSheet : BottomSheetDialogFragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private var _binding: BottomSheetRegistrationPaymentConfigBinding? = null
    private val binding get() = _binding!!

    private val locale = Locale("pt", "BR")
    private var paymentType: String = "credito"
    private var editingPaymentId: Long = -1L
    private var mode: PaymentConfigMode = PaymentConfigMode.SIMPLE_QUOTE
    private var selectedFee: InstallmentFee? = null
    private var simpleQuote: SimplePaymentQuote? = null
    private var adapter: InstallmentsAdapter? = null

    override fun getTheme(): Int = com.google.android.material.R.style.ThemeOverlay_MaterialComponents_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentType = requireArguments().getString(ARG_PAYMENT_TYPE) ?: "credito"
        editingPaymentId = requireArguments().getLong(ARG_PAYMENT_ID, -1L)
        mode = when (PaymentTypeRules.normalize(paymentType)) {
            "credito" -> PaymentConfigMode.CREDIT
            "debito" -> PaymentConfigMode.DEBIT
            "dinheiro", "store_credit" -> PaymentConfigMode.DIRECT
            else -> PaymentConfigMode.SIMPLE_QUOTE
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
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        )
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRegistrationPaymentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registrationViewModel.clearFeesState()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registrationViewModel.clearFeesState()
        setupAdapter()
        setupUi()
        setupObservers()
        setupInitialValue()
        if (editingPaymentId != -1L) {
            loadEditingPayment()
        }
    }

    private fun setupUi() {
        binding.tvSheetTitle.text = "Confirmar pagamento"
        binding.tvSheetSubtitle.text =
            balanceSubtitle(registrationViewModel.remainingBalanceLiveData.value ?: 0.0)
        binding.tvAmountLabel.text = "Valor a pagar"

        val usesBrand = false
        binding.toggleGroupBrand.isVisible = usesBrand
        binding.tvBrandLabel.isVisible = usesBrand
        binding.groupCreditSection.isVisible = mode == PaymentConfigMode.CREDIT
        binding.cardSimpleSummary.isVisible = false
        binding.rvInstallments.isVisible = false
        binding.btnReset.visibility = View.GONE
        binding.tvActionHint.isVisible = mode != PaymentConfigMode.DIRECT
        binding.btnPrimaryAction.isVisible = mode != PaymentConfigMode.DIRECT
        binding.btnConfirm.isVisible = mode == PaymentConfigMode.DIRECT

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnClearValue.setOnClickListener { binding.etPaymentValue.setText("0") }
        binding.root.setOnClickListener { hideKeyboard() }
        binding.etPaymentValue.setOnEditorActionListener { _, actionId, _ ->
            if (mode != PaymentConfigMode.DIRECT &&
                (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE)
            ) {
                if (binding.btnPrimaryAction.isEnabled) {
                    binding.btnPrimaryAction.performClick()
                }
                true
            } else {
                false
            }
        }
        binding.etPaymentValue.addTextChangedListener(Mask.moneyMask(binding.etPaymentValue) { value ->
            binding.btnClearValue.isVisible = value.isNotBlank() && value != "0,00"
            invalidateQuote()
        })

        binding.btnPrimaryAction.setOnClickListener {
            hideKeyboard()
            val amount = Mask.doubleValue(binding.etPaymentValue.text.toString())
            if (amount <= 0.0) {
                showError("Informe um valor maior que zero.")
                return@setOnClickListener
            }
            registrationViewModel.calculateFees(amount, paymentType)
        }

        binding.btnReset.setOnClickListener {
            invalidateQuote()
        }

        binding.btnConfirm.setOnClickListener {
            when (mode) {
                PaymentConfigMode.CREDIT -> confirmCreditPayment()
                PaymentConfigMode.DEBIT, PaymentConfigMode.SIMPLE_QUOTE -> confirmSimplePayment()
                PaymentConfigMode.DIRECT -> confirmDirectPayment()
            }
        }

        updatePrimaryActionLabels()
        updatePrimaryActionState()
    }

    private fun setupObservers() {
        registrationViewModel.calculateFeesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.loadingOverlay.isVisible = true
                    binding.btnPrimaryAction.isEnabled = false
                }
                is UIState.Success -> {
                    binding.loadingOverlay.isVisible = false
                    binding.btnPrimaryAction.isEnabled = true
                    val brandFees = state.data?.data.orEmpty().firstOrNull()
                    val installments = brandFees?.installments.orEmpty()
                    if (installments.isEmpty()) {
                        showError("Não foi possível obter as taxas para esse pagamento.")
                        return@observe
                    }
                    when (mode) {
                        PaymentConfigMode.CREDIT -> showInstallments(installments)
                        PaymentConfigMode.DEBIT, PaymentConfigMode.SIMPLE_QUOTE -> showSimpleQuote(installments)
                        PaymentConfigMode.DIRECT -> Unit
                    }
                }
                is UIState.Error -> {
                    binding.loadingOverlay.isVisible = false
                    binding.btnPrimaryAction.isEnabled = true
                    showError(state.message ?: "Erro ao calcular taxas.")
                }
                is UIState.Idle -> {
                    binding.loadingOverlay.isVisible = false
                }
                null -> Unit
            }
        }
    }

    private fun setupAdapter() {
        adapter = InstallmentsAdapter(showRadioButton = true) { fee ->
            selectedFee = fee
            renderCreditSelection(fee)
        }
        binding.rvInstallments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstallments.adapter = adapter
    }

    private fun setupInitialValue() {
        val remaining = registrationViewModel.remainingBalanceLiveData.value ?: 0.0
        if (editingPaymentId == -1L && remaining > 0) {
            binding.etPaymentValue.setText(Math.round(remaining * 100).toString())
        }
        updatePrimaryActionState()
        focusAmountInput(selectAll = true)
    }

    private fun loadEditingPayment() {
        val payment = registrationViewModel.getPaymentById(editingPaymentId) ?: return
        val cleanValue = payment.amountOriginal.replace("[R$.\\s]".toRegex(), "").replace(",", "")
        binding.etPaymentValue.setText(cleanValue)
        focusAmountInput(selectAll = true)
        if (mode != PaymentConfigMode.CREDIT && mode != PaymentConfigMode.DEBIT) {
            binding.btnConfirm.isEnabled = Mask.toSafeDouble(payment.amountOriginal) > 0
            binding.btnConfirm.text = "Efetuar pagamento"
            updateButtonVisualState(binding.btnConfirm, binding.btnConfirm.isEnabled)
        }

        restoreSavedQuote(payment)
    }

    private fun showInstallments(fees: List<InstallmentFee>) {
        binding.rvInstallments.isVisible = true
        binding.cardSimpleSummary.isVisible = false
        binding.btnReset.visibility = View.GONE
        adapter?.submitList(fees)
        binding.tvActionHint.isVisible = false
        binding.btnPrimaryAction.isVisible = false
        binding.btnConfirm.isVisible = true
        binding.scrollContent.post {
            binding.scrollContent.smoothScrollTo(0, binding.groupCreditSection.top)
        }

        if (editingPaymentId != -1L) {
            val payment = registrationViewModel.getPaymentById(editingPaymentId)
            val existing = fees.firstOrNull { it.installmentNumber == payment?.installment }
            if (existing != null) {
                selectedFee = existing
                adapter?.setSelected(existing)
                renderCreditSelection(existing)
                return
            }
        }

        binding.btnConfirm.isEnabled = false
        binding.btnConfirm.text = getString(R.string.registration_payment_detail_save)
        updateButtonVisualState(binding.btnConfirm, false)
        updatePrimaryActionState()
    }

    private fun renderCreditSelection(fee: InstallmentFee) {
        binding.cardSimpleSummary.isVisible = false
        binding.btnConfirm.isEnabled = true
        binding.btnConfirm.text = getString(R.string.registration_payment_detail_save)
        binding.tvActionHint.isVisible = false
        updateButtonVisualState(binding.btnConfirm, true)
    }

    private fun showSimpleQuote(fees: List<InstallmentFee>) {
        val selected = fees.firstOrNull { it.installmentNumber == 1 } ?: fees.firstOrNull()
        if (selected == null) {
            showError("Não foi possível calcular o valor final.")
            return
        }
        val amountOriginal = Mask.doubleValue(binding.etPaymentValue.text.toString())
        val amountFinal = Mask.toSafeDouble(selected.totalValue)
        val feeAmount = (amountFinal - amountOriginal).coerceAtLeast(0.0)
        val paymentMethod = resolvePaymentMethod(selected.installmentNumber) ?: run {
            showError("Método de pagamento indisponível.")
            return
        }

        simpleQuote = SimplePaymentQuote(
            amountOriginal = amountOriginal,
            feeAmount = feeAmount,
            amountFinal = amountFinal,
            paymentMethod = paymentMethod,
        )

        binding.cardSimpleSummary.isVisible = true
        binding.rvInstallments.isVisible = false
        binding.btnReset.visibility = View.VISIBLE
        binding.tvSummaryTitle.text = if (mode == PaymentConfigMode.DEBIT) "Resumo do débito" else "Resumo do pagamento"
        binding.tvSummaryLine1Label.text = "Valor informado"
        binding.tvSummaryLine1Value.text = formatCurrency(amountOriginal)
        binding.tvSummaryLine2Label.text = "Taxa"
        binding.tvSummaryLine2Value.text = formatCurrency(feeAmount)
        binding.tvSummaryLine3Label.text = "Valor final"
        binding.tvSummaryLine3Value.text = formatCurrency(amountFinal)
        binding.tvActionHint.text = "Revise taxa e valor final antes de adicionar o pagamento."
        binding.btnConfirm.isEnabled = true
        binding.btnConfirm.isVisible = true
        binding.btnConfirm.text = "Efetuar pagamento"
        binding.btnPrimaryAction.isVisible = false
        binding.tvActionHint.isVisible = true
        updateButtonVisualState(binding.btnConfirm, true)
        updatePrimaryActionState()
    }

    private fun restoreSavedQuote(payment: SimulationPayment) {
        val amount = Mask.toSafeDouble(payment.amountOriginal)
        if (amount <= 0.0) return

        val cachedFees = registrationViewModel.getCachedFees(amount, paymentType)
            ?.data
            .orEmpty()
            .firstOrNull()
            ?.installments
            .orEmpty()

        when (mode) {
            PaymentConfigMode.CREDIT -> {
                val feesToRender = if (cachedFees.isNotEmpty()) cachedFees else listOf(savedInstallmentFee(payment))
                showInstallments(feesToRender)
            }
            PaymentConfigMode.DEBIT, PaymentConfigMode.SIMPLE_QUOTE -> {
                val feesToRender = if (cachedFees.isNotEmpty()) cachedFees else listOf(savedInstallmentFee(payment))
                showSimpleQuote(feesToRender)
            }
            PaymentConfigMode.DIRECT -> Unit
        }
    }

    private fun savedInstallmentFee(payment: SimulationPayment): InstallmentFee {
        val totalValue = Mask.toSafeDouble(payment.amountFinal)
        val installmentNumber = payment.installment.coerceAtLeast(1)
        val installmentValue = totalValue / installmentNumber
        val amountOriginal = Mask.toSafeDouble(payment.amountOriginal)
        val interestValue = (totalValue - amountOriginal).coerceAtLeast(0.0)

        return InstallmentFee(
            installmentNumber = installmentNumber,
            installmentValue = "%,.2f".format(locale, installmentValue),
            totalValue = "%,.2f".format(locale, totalValue),
            interestValue = "%,.2f".format(locale, interestValue),
            noInterest = interestValue <= 0.0,
        )
    }

    private fun invalidateQuote() {
        selectedFee = null
        simpleQuote = null
        binding.rvInstallments.isVisible = false
        binding.cardSimpleSummary.isVisible = false
        binding.btnReset.visibility = View.GONE
        binding.btnPrimaryAction.isVisible = mode != PaymentConfigMode.DIRECT
        binding.btnConfirm.isVisible = mode == PaymentConfigMode.DIRECT
        binding.tvActionHint.isVisible = mode != PaymentConfigMode.CREDIT && mode != PaymentConfigMode.DIRECT
        binding.tvActionHint.text = when (mode) {
            PaymentConfigMode.CREDIT -> ""
            PaymentConfigMode.DEBIT -> "Calcule a taxa antes de confirmar."
            PaymentConfigMode.SIMPLE_QUOTE -> "Calcule a taxa e o valor final antes de confirmar."
            PaymentConfigMode.DIRECT -> ""
        }
        if (mode == PaymentConfigMode.DIRECT) {
            val hasAmount = Mask.doubleValue(binding.etPaymentValue.text.toString()) > 0.0
            binding.btnConfirm.isEnabled = hasAmount
            binding.btnConfirm.text = "Efetuar pagamento"
            updateButtonVisualState(binding.btnConfirm, hasAmount)
        } else {
            binding.btnConfirm.isEnabled = false
            updateButtonVisualState(binding.btnConfirm, false)
        }
        updatePrimaryActionLabels()
        updatePrimaryActionState()
    }

    private fun updatePrimaryActionLabels() {
        binding.btnPrimaryAction.text = when (mode) {
            PaymentConfigMode.CREDIT -> "Ver parcelas"
            PaymentConfigMode.DEBIT -> "Calcular"
            PaymentConfigMode.SIMPLE_QUOTE -> "Calcular"
            PaymentConfigMode.DIRECT -> ""
        }
    }

    private fun updatePrimaryActionState() {
        val hasAmount = Mask.doubleValue(binding.etPaymentValue.text.toString()) > 0.0
        val isEnabled = when (mode) {
            PaymentConfigMode.CREDIT, PaymentConfigMode.DEBIT -> hasAmount
            PaymentConfigMode.SIMPLE_QUOTE -> hasAmount
            PaymentConfigMode.DIRECT -> false
        }
        binding.btnPrimaryAction.isEnabled = isEnabled
        if (mode != PaymentConfigMode.DIRECT) {
            updateButtonVisualState(binding.btnPrimaryAction, isEnabled)
        }
    }

    private fun confirmCreditPayment() {
        val fee = selectedFee ?: return
        val method = resolvePaymentMethod(fee.installmentNumber) ?: run {
            showError("Método de pagamento indisponível.")
            return
        }
        val payment = SimulationPayment(
            id = if (editingPaymentId != -1L) editingPaymentId else System.currentTimeMillis(),
            paymentMethod = method.copy(
                interestTax = 0.0,
                    installments = fee.installmentNumber,
            ),
            amountOriginal = binding.etPaymentValue.text.toString(),
            amountFinal = "%,.2f".format(locale, Mask.toSafeDouble(fee.totalValue)),
            installment = fee.installmentNumber,
        )
        persistPayment(payment)
    }

    private fun confirmSimplePayment() {
        val quote = simpleQuote ?: return
        val payment = SimulationPayment(
            id = if (editingPaymentId != -1L) editingPaymentId else System.currentTimeMillis(),
            paymentMethod = quote.paymentMethod.copy(
                interestTax = 0.0,
                    installments = 1,
            ),
            amountOriginal = "%,.2f".format(locale, quote.amountOriginal),
            amountFinal = "%,.2f".format(locale, quote.amountFinal),
            installment = 1,
        )
        persistPayment(payment)
    }

    private fun confirmDirectPayment() {
        val amount = Mask.doubleValue(binding.etPaymentValue.text.toString())
        if (amount <= 0.0) {
            showError("Informe um valor maior que zero.")
            return
        }
        val method = resolvePaymentMethod(1) ?: run {
            showError("Método de pagamento indisponível.")
            return
        }
        val payment = SimulationPayment(
            id = if (editingPaymentId != -1L) editingPaymentId else System.currentTimeMillis(),
            paymentMethod = method.copy(
                interestTax = 0.0,
                    installments = 1,
            ),
            amountOriginal = "%,.2f".format(locale, amount),
            amountFinal = "%,.2f".format(locale, amount),
            installment = 1,
        )
        persistPayment(payment)
    }

    private fun persistPayment(payment: SimulationPayment) {
        if (editingPaymentId != -1L) {
            registrationViewModel.updateSimulationPayment(payment)
        } else {
            registrationViewModel.addPayment(payment)
        }
        dismiss()
    }

    private fun resolvePaymentMethod(installments: Int): PaymentMethod? {
        val methods = registrationViewModel.getPaymentMethodsByType(paymentType)
        return methods.firstOrNull { it.installments == installments }
            ?: methods.firstOrNull { it.installments == 1 }
            ?: methods.firstOrNull()
    }

    private fun paymentTypeLabel(type: String): String = when (type.lowercase()) {
        "credito", "credit" -> "crédito"
        "debito", "debit" -> "débito"
        "pix" -> "PIX"
        "dinheiro", "cash" -> "dinheiro"
        "store_credit" -> "crédito loja"
        else -> type.lowercase()
    }

    private fun formatCurrency(value: Double): String = "R$ %,.2f".format(locale, value)

    private fun balanceSubtitle(balance: Double): String {
        val label = when {
            balance > 0 -> "Saldo pendente"
            balance < 0 -> "Excedente"
            else -> "Quitado"
        }
        return "$label: ${formatCurrency(abs(balance))}"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        binding.etPaymentValue.clearFocus()
    }

    private fun focusAmountInput(selectAll: Boolean) {
        binding.etPaymentValue.post {
            binding.etPaymentValue.requestFocus()
            binding.etPaymentValue.setSelectAllOnFocus(true)
            if (selectAll) {
                binding.etPaymentValue.text?.let { text ->
                    binding.etPaymentValue.setSelection(0, text.length)
                }
            }
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.etPaymentValue, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun updateButtonVisualState(button: com.google.android.material.button.MaterialButton, isEnabled: Boolean) {
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
        private const val ARG_PAYMENT_TYPE = "paymentType"
        private const val ARG_PAYMENT_ID = "paymentId"

        fun newInstance(paymentType: String, paymentId: Long = -1L): RegistrationPaymentConfigBottomSheet {
            return RegistrationPaymentConfigBottomSheet().apply {
                arguments = bundleOf(
                    ARG_PAYMENT_TYPE to paymentType,
                    ARG_PAYMENT_ID to paymentId,
                )
            }
        }
    }
}



