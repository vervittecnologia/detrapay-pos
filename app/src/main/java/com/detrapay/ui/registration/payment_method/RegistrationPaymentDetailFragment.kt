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

    private fun loadBrandIcons() {
        ImageUtils.loadImage(requireContext(), "VISA", binding.btnVisa)
        ImageUtils.loadImage(requireContext(), "MASTERCARD", binding.btnMaster)
        ImageUtils.loadImage(requireContext(), "ELO", binding.btnElo)
    }

    private fun loadEditingPayment() {
        registrationViewModel.getPaymentById(editingPaymentId)?.let { payment ->
            binding.etCardValue.setText(payment.amountOriginal.replace(".", "").replace(",", ""))
            // Assuming the brand is stored in the name or we can infer it
            val brand = payment.paymentMethod.name.uppercase()
            when {
                brand.contains("VISA") -> binding.toggleGroupBrand.check(R.id.btnVisa)
                brand.contains("MASTERCARD") || brand.contains("MASTER") -> binding.toggleGroupBrand.check(R.id.btnMaster)
                brand.contains("ELO") -> binding.toggleGroupBrand.check(R.id.btnElo)
                else -> { /* No default brand to check */ }
            }
            
            // Auto-trigger consult if values are present
            val amount = Mask.doubleValue(binding.etCardValue.text.toString())
            if (amount > 0 && selectedBrand.isNotEmpty()) {
                registrationViewModel.calculateFees(amount, paymentType, selectedBrand)
            }
        }
    }

    private fun setupUI() {
        // Large Value Input with Mask
        binding.etCardValue.addTextChangedListener(Mask.moneyMask(binding.etCardValue) { _ ->
            // Reset installments if value changes?
            hideInstallments()
        })

        // Set default value (always start empty/zeroed as requested)
        binding.etCardValue.setText("")
        binding.etCardValue.post {
            binding.etCardValue.requestFocus()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(binding.etCardValue, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        binding.toggleGroupBrand.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                selectedBrand = when (checkedId) {
                    R.id.btnVisa -> "VISA"
                    R.id.btnMaster -> "MASTERCARD"
                    R.id.btnElo -> "ELO"
                    else -> ""
                }
                hideInstallments()
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
                    showInstallmentsBottomSheet(installments)
                }
                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.btnConsultInstallments.isEnabled = true
                    // Show error toast or similar
                }
            }
        }
    }

    private fun showInstallmentsBottomSheet(fees: List<InstallmentFee>) {
        val bottomSheet = InstallmentsBottomSheet(fees) { fee ->
            selectedFee = fee
            showSelectedInstallment(fee)
        }
        bottomSheet.show(childFragmentManager, InstallmentsBottomSheet.TAG)
    }

    private fun showSelectedInstallment(fee: InstallmentFee) {
        binding.btnConsultInstallments.visibility = View.GONE
        binding.tvConsultHint.visibility = View.GONE
        binding.rvInstallments.visibility = View.VISIBLE
        binding.btnAlterarDados.visibility = View.VISIBLE
        binding.btnConfirm.visibility = View.VISIBLE

        adapter?.submitList(listOf(fee))
        
        // Allow clicking the selected item to re-open the bottom sheet
        binding.rvInstallments.setOnClickListener {
            binding.btnConsultInstallments.performClick()
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
        adapter = InstallmentsAdapter { fee ->
            selectedFee = fee
        }
        binding.rvInstallments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInstallments.adapter = adapter
    }

    private fun confirmPayment() {
        val fee = selectedFee ?: return
        val amountOriginal = binding.etCardValue.text.toString()
        val total = fee.totalValue.toDouble()
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
