package com.detrapay.ui.registration.payment_method

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.FragmentRegistrationOrderPaymentMethodBinding
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Mask

class RegistrationPaymentMethodFragment : Fragment(), OnItemClickListener {

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationOrderPaymentMethodBinding
    private var adapter: RegistrationPaymentMethodRecyclerViewAdapter? = null
    private var pendingScrollPaymentId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRegistrationOrderPaymentMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupObservers()
        setupMethodPickerResultListener()
        bindHeaderData()
        registrationViewModel.loadPaymentSelectionScreenContent()
        binding.tvTotalValue.text = registrationViewModel.simulationTotalAmount()
    }

    private fun setupUi() {
        binding.reloadPaymentMethodBtn.setOnClickListener {
            registrationViewModel.loadPaymentSelectionScreenContent()
        }

        binding.btnBack.setOnClickListener {
            registrationViewModel.navigateBack()
            findNavController().popBackStack()
        }

        binding.tvCancelAndBack.setOnClickListener {
            registrationViewModel.navigateBack()
            findNavController().popBackStack()
        }

        binding.btnClose.setOnClickListener {
            (activity as? RegistrationActivity)?.showExitConfirmation()
        }

        binding.btnAddPayment.setOnClickListener {
            showPaymentMethodPicker()
        }

        binding.registrationPaymentMethodNextBtn.setOnClickListener {
            registrationViewModel.createOrder()
        }

        setupPaymentAdapter()
    }

    private fun setupMethodPickerResultListener() {
        childFragmentManager.setFragmentResultListener(
            RegistrationPaymentMethodPickerBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val type = bundle.getString(RegistrationPaymentMethodPickerBottomSheet.RESULT_PAYMENT_TYPE)
                ?: return@setFragmentResultListener
            openPaymentConfig(type)
        }
    }

    private fun showPaymentMethodPicker() {
        val types = availablePaymentTypes()
        if (types.isEmpty()) return

        RegistrationPaymentMethodPickerBottomSheet.newInstance(types)
            .show(childFragmentManager, "RegistrationPaymentMethodPickerBottomSheet")
    }

    private fun availablePaymentTypes(): List<String> {
        val mapping = listOf(
            RegistrationPaymentMethodPickerBottomSheet.TYPE_CREDIT,
            RegistrationPaymentMethodPickerBottomSheet.TYPE_DEBIT,
            RegistrationPaymentMethodPickerBottomSheet.TYPE_PIX,
            RegistrationPaymentMethodPickerBottomSheet.TYPE_CASH,
            RegistrationPaymentMethodPickerBottomSheet.TYPE_STORE_CREDIT,
        )

        return mapping.filter { type ->
            registrationViewModel.getPaymentMethodsByType(type).isNotEmpty()
        }
    }

    private fun bindHeaderData() {
        val orderId = registrationViewModel.currentOrderId()
        binding.tvOrderTitle.text = if (orderId != null) {
            getString(R.string.registration_payment_order_title_with_id, orderId)
        } else {
            getString(R.string.registration_payment_order_title)
        }
        binding.tvOrderSubtitle.text = getString(R.string.order_details_header_caption)

        val customer = registrationViewModel.simulationCustomer()
        if (customer == null) {
            binding.tvCustomerName.text = getString(R.string.registration_payment_customer_fallback)
            binding.tvCustomerInfo.text = getString(R.string.profile_not_informed)
            return
        }

        val formattedPhone = registrationViewModel.formatWhatsAppTargetPhone(customer.whatsapp)
        binding.tvCustomerName.text = customer.name
        binding.tvCustomerInfo.text = if (formattedPhone.isNotBlank()) {
            "${customer.cpfCnpj} • $formattedPhone"
        } else {
            customer.cpfCnpj
        }
    }

    private fun openPaymentConfig(type: String, paymentId: Long = -1L) {
        RegistrationPaymentConfigBottomSheet.newInstance(type, paymentId)
            .show(childFragmentManager, "RegistrationPaymentConfigBottomSheet")
    }

    private fun setupObservers() {
        registrationViewModel.registrationState.observe(viewLifecycleOwner) { state ->
            if (state.currentScreen == 2) {
                findNavController().popBackStack()
            }
        }

        registrationViewModel.paymentSelectionInitialState.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UIState.Success -> {
                    binding.loadingView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.btnAddPayment.isEnabled = availablePaymentTypes().isNotEmpty()
                    binding.btnAddPayment.alpha = if (binding.btnAddPayment.isEnabled) 1f else 0.45f
                }

                is UIState.Error -> {
                    binding.loadingView.visibility = View.GONE
                    binding.errorView.visibility = View.VISIBLE
                }

                is UIState.Loading -> {
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }

                is UIState.Idle -> Unit
            }
        }

        registrationViewModel.paymentsLiveData.observe(viewLifecycleOwner) { payments ->
            adapter?.submitList(payments.toList()) {
                scrollToSavedPaymentIfNeeded(payments)
            }
            val hasPayments = payments.isNotEmpty()
            binding.llLaunchedContainer.visibility = if (hasPayments) View.VISIBLE else View.GONE
            binding.emptyPaymentsContainer.visibility = if (hasPayments) View.GONE else View.VISIBLE
            binding.tvPaidValue.text = formatCurrency(
                payments.sumOf { payment -> Mask.doubleValue(payment.amountOriginal) },
            )
        }

        registrationViewModel.lastSavedPaymentId.observe(viewLifecycleOwner) { paymentId ->
            if (paymentId == null) return@observe
            pendingScrollPaymentId = paymentId
            scrollToSavedPaymentIfNeeded(adapter?.currentList.orEmpty())
        }

        registrationViewModel.remainingBalanceLiveData.observe(viewLifecycleOwner) { balance ->
            val paidAmount = (registrationViewModel.simulationSimulation()?.totalPrice ?: 0.0) - balance
            if (paidAmount >= 0) {
                binding.tvPaidValue.text = formatCurrency(paidAmount)
            }

            binding.tvRemainingLabel.text = getString(R.string.registration_payment_debt_label)
            binding.tvRemainingValue.text = formatCurrency(kotlin.math.abs(balance))
        }

        registrationViewModel.paymentSelectionCreateOrderState.observe(viewLifecycleOwner) { status ->
            when (status) {
                is UIState.Success -> {
                    binding.registrationPaymentMethodNextBtn.isEnabled = false
                    binding.registrationLoading.visibility = View.GONE
                    status.data?.let { openDetailsScreen(it.order) }
                }

                is UIState.Error -> {
                    binding.registrationPaymentMethodNextBtn.isEnabled = true
                    binding.registrationErrorTextView.text = status.message
                    binding.registrationErrorTextView.visibility = View.VISIBLE
                    binding.registrationLoading.visibility = View.GONE
                }

                is UIState.Loading -> {
                    binding.registrationPaymentMethodNextBtn.isEnabled = false
                    binding.registrationErrorTextView.visibility = View.GONE
                    binding.registrationLoading.visibility = View.VISIBLE
                }

                is UIState.Idle -> Unit
            }
        }
    }

    private fun setupPaymentAdapter() {
        adapter = RegistrationPaymentMethodRecyclerViewAdapter(registrationViewModel, this)
        binding.rvPaymentMethods.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaymentMethods.adapter = adapter
    }

    private fun formatCurrency(value: Double): String {
        return "R$ %,.2f".format(java.util.Locale("pt", "BR"), value)
    }

    private fun scrollToSavedPaymentIfNeeded(payments: List<SimulationPayment>) {
        val paymentId = pendingScrollPaymentId ?: return
        val index = payments.indexOfFirst { it.id == paymentId }
        if (index == -1) return

        binding.rvPaymentMethods.post {
            binding.rvPaymentMethods.smoothScrollToPosition(index)
        }
        pendingScrollPaymentId = null
        registrationViewModel.consumeLastSavedPaymentId()
    }

    private fun openDetailsScreen(order: Order) {
        (requireActivity() as RegistrationActivity).finishWithCreatedOrder(order)
    }

    override fun onAdd(item: SimulationPayment) {
        registrationViewModel.addPayment(item)
    }

    override fun onDelete(item: SimulationPayment) {
        registrationViewModel.removePayment(item)
    }

    override fun onItemUpdated(newItem: SimulationPayment) {
        registrationViewModel.updateSimulationPayment(newItem)
    }

    override fun onItemClicked(item: SimulationPayment) {
        openPaymentConfig(item.paymentMethod.paymentType ?: RegistrationPaymentMethodPickerBottomSheet.TYPE_CREDIT, item.id)
    }
}
