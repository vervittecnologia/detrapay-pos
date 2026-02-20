package com.detrapay.ui.registration.payment_method

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.SimulationPayment
import com.detrapay.data.model.remote.InstallmentFee
import com.detrapay.databinding.FragmentRegistrationPaymentDetailBinding
import com.detrapay.databinding.RegistrationPaymentInstallmentItemBinding
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.ImageUtils
import com.detrapay.ui.util.Mask
import java.util.Locale

class RegistrationPaymentDetailFragment : Fragment() {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationPaymentDetailBinding
    private var paymentType: String = ""
    private var selectedBrand: String = ""
    private var selectedFee: InstallmentFee? = null
    private var adapter: InstallmentsAdapter? = null
    private var editingPaymentId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationPaymentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        paymentType = arguments?.getString("paymentType") ?: "credito"
        editingPaymentId = arguments?.getLong("paymentId", -1L) ?: -1L
        
        registrationViewModel.clearFeesState()
        
        setupUI()
        setupObservers()
        loadBrandIcons()
        
        if (editingPaymentId != -1L) {
            loadEditingPayment()
        }
    }

    private fun loadEditingPayment() {
        registrationViewModel.getPaymentById(editingPaymentId)?.let { payment ->
            val cleanValue = payment.amountOriginal.replace("[R$.\\s]".toRegex(), "").replace(",", "")
            binding.etCardValue.setText(cleanValue)
            
            val brand = payment.paymentMethod.name.uppercase()
            when {
                brand.contains("VISA") -> binding.toggleGroupBrand.check(R.id.btnVisa)
                brand.contains("MASTERCARD") || brand.contains("MASTER") -> binding.toggleGroupBrand.check(R.id.btnMaster)
                brand.contains("ELO") -> binding.toggleGroupBrand.check(R.id.btnElo)
                else -> { /* No default brand to check */ }
            }
            
            // For editing, we trigger the fee calculation automatically to show the list
            val amount = Mask.toSafeDouble(payment.amountOriginal)
            if (amount > 0 && selectedBrand.isNotEmpty()) {
                registrationViewModel.calculateFees(amount, paymentType, selectedBrand)
            }
        }
    }

    private fun loadBrandIcons() {
        binding.btnVisa.setIconResource(R.drawable.ic_visa)
        binding.btnMaster.setIconResource(R.drawable.ic_mastercard)
        binding.btnElo.setIconResource(R.drawable.ic_elo)
        
        binding.btnVisa.iconTint = null
        binding.btnMaster.iconTint = null
        binding.btnElo.iconTint = null
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnClose.setOnClickListener { findNavController().popBackStack() }

        // Root view click to clear focus and hide keyboard
        val hideKeyboardAction = View.OnClickListener {
            binding.etCardValue.clearFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        binding.root.setOnClickListener(hideKeyboardAction)
        binding.contentContainer.setOnClickListener(hideKeyboardAction)

        // Large Value Input with Mask
        binding.etCardValue.addTextChangedListener(Mask.moneyMask(binding.etCardValue) { value ->
            // Reset installments if value changes?
            hideInstallments()
            binding.btnClearValue.visibility = if (value.isNotEmpty() && value != "0,00") View.VISIBLE else View.GONE
        })

        binding.btnClearValue.setOnClickListener {
            binding.etCardValue.setText("0")
        }

        // Set pending balance as initial value if not editing
        val remaining = registrationViewModel.remainingBalanceLiveData.value ?: 0.0
        if (editingPaymentId == -1L && remaining > 0) {
            val initialValue = Math.round(remaining * 100).toString()
            binding.etCardValue.setText(initialValue)
        } else if (editingPaymentId == -1L) {
            binding.etCardValue.setText("")
        }

        binding.etCardValue.post {
            binding.etCardValue.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etCardValue, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.btnConsultInstallments.isEnabled = selectedBrand.isNotEmpty()

        binding.toggleGroupBrand.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedBrand = when (checkedId) {
                    R.id.btnVisa -> "VISA"
                    R.id.btnMaster -> "MASTERCARD"
                    R.id.btnElo -> "ELO"
                    else -> ""
                }
                binding.btnConsultInstallments.isEnabled = selectedBrand.isNotEmpty()
                hideInstallments()
                
                // Hide keyboard when a brand is selected
                val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.hideSoftInputFromWindow(group.windowToken, 0)
                binding.etCardValue.clearFocus()
            }
        }

        binding.btnConsultInstallments.setOnClickListener {
            val amount = Mask.doubleValue(binding.etCardValue.text.toString())
            if (amount > 0 && selectedBrand.isNotEmpty()) {
                registrationViewModel.calculateFees(amount, paymentType, selectedBrand)
            }
        }

        binding.btnAlterarDados.setOnClickListener {
            hideInstallments()
        }

        binding.btnConfirm.setOnClickListener {
            confirmPayment()
        }

        setupAdapter()
    }

    private fun setupObservers() {
        registrationViewModel.calculateFeesState.observe(viewLifecycleOwner) { state ->
            state ?: return@observe
            when (state) {
                is UIState.Loading -> {
                    binding.loadingView.visibility = View.VISIBLE
                    binding.btnConsultInstallments.isEnabled = false
                }
                is UIState.Success -> {
                    binding.loadingView.visibility = View.GONE
                    binding.btnConsultInstallments.isEnabled = true
                    val installments = state.data?.data?.firstOrNull()?.installments ?: emptyList()
                    showInstallmentsList(installments)
                }
                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.btnConsultInstallments.isEnabled = true
                    // Show error toast or similar
                }
                is UIState.Idle -> {}
            }
        }
    }

    private fun showInstallmentsList(fees: List<InstallmentFee>) {
        binding.btnConsultInstallments.visibility = View.GONE
        binding.tvConsultHint.visibility = View.GONE
        binding.rvInstallments.visibility = View.VISIBLE
        binding.btnAlterarDados.visibility = View.VISIBLE
        
        adapter?.submitList(fees)
        
        // If editing, try to select the current installment
        if (editingPaymentId != -1L && selectedFee == null) {
            registrationViewModel.getPaymentById(editingPaymentId)?.let { payment ->
                fees.find { it.installmentNumber == payment.installment }?.let { fee ->
                    selectedFee = fee
                    adapter?.setSelected(fee)
                    binding.btnConfirm.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun hideInstallments() {
        binding.btnConsultInstallments.visibility = View.VISIBLE
        binding.tvConsultHint.visibility = View.VISIBLE
        binding.rvInstallments.visibility = View.GONE
        binding.btnAlterarDados.visibility = View.GONE
        binding.btnConfirm.visibility = View.INVISIBLE
        selectedFee = null
    }

    private fun setupAdapter() {
        adapter = InstallmentsAdapter(showRadioButton = true) { fee ->
            selectedFee = fee
            binding.btnConfirm.visibility = View.VISIBLE
        }
        binding.rvInstallments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstallments.adapter = adapter
        // Ensure the RecyclerView can scroll inside the ScrollView if needed
        binding.rvInstallments.isNestedScrollingEnabled = true
    }

    private fun confirmPayment() {
        val fee = selectedFee ?: return
        val amountOriginal = binding.etCardValue.text.toString()
        val total = Mask.toSafeDouble(fee.totalValue)
        val amountFinal = "%,.2f".format(Locale("pt", "BR"), total)

        // Find a matching payment method for metadata if possible, or create a virtual one
        val baseMethods = registrationViewModel.getPaymentMethodsByType(paymentType)
        val method = baseMethods.firstOrNull { it.maxInstallments == fee.installmentNumber } 
                     ?: baseMethods.firstOrNull() 
                     ?: PaymentMethod(0, "Pagamento", fee.installmentNumber, 0.0, paymentType)

        val payment = SimulationPayment(
            id = if (editingPaymentId != -1L) editingPaymentId else System.currentTimeMillis(),
            paymentMethod = method.copy(interestTax = 0.0, maxInstallments = fee.installmentNumber, name = "$selectedBrand ${method.name}"),
            amountOriginal = amountOriginal,
            amountFinal = amountFinal,
            installment = fee.installmentNumber
        )

        if (editingPaymentId != -1L) {
            registrationViewModel.updateSimulationPayment(payment)
        } else {
            registrationViewModel.addPayment(payment)
        }
        findNavController().popBackStack()
    }
}
