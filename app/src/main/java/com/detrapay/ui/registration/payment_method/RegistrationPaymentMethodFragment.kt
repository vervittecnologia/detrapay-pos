package com.detrapay.ui.registration.payment_method

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.R
import com.detrapay.data.model.Order
import com.detrapay.data.model.SimulationPayment
import com.detrapay.databinding.FragmentRegistrationOrderPaymentMethodBinding
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.order_details.OrderDetailsActivity
import com.detrapay.ui.registration.RegistrationViewModel
import com.detrapay.ui.state.UIState

class RegistrationPaymentMethodFragment : Fragment(), OnItemClickListener{

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentRegistrationOrderPaymentMethodBinding
    private var adapter: RegistrationPaymentMethodRecyclerViewAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationOrderPaymentMethodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupUI()
        registrationViewModel.loadPaymentSelectionScreenContent()
        binding.tvTotalValue.text = registrationViewModel.simulationTotalAmount()
    }

    private fun setupUI() {
        binding.btnCredit.setOnClickListener { openPaymentConfig("credito") }
        binding.btnDebit.setOnClickListener { openPaymentConfig("debito") }
        binding.btnStoreCredit.setOnClickListener { openPaymentConfig("store_credit") }
        binding.btnCash.setOnClickListener { openPaymentConfig("dinheiro") }

        binding.btnPix.isEnabled = false
        binding.btnPix.isClickable = false
        binding.btnPix.alpha = 0.5f

        binding.reloadPaymentMethodBtn.setOnClickListener {
            registrationViewModel.loadPaymentSelectionScreenContent()
        }

        binding.btnBack.setOnClickListener {
            registrationViewModel.navigateBack()
            findNavController().popBackStack()
        }

        binding.btnClose.setOnClickListener {
            (activity as? RegistrationActivity)?.showExitConfirmation()
        }

        binding.registrationPaymentMethodNextBtn.setOnClickListener {
            registrationViewModel.createOrder()
        }

        setupPaymentAdapter()
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
                is UIState.Idle -> {}
            }
        }

        registrationViewModel.paymentsLiveData.observe(viewLifecycleOwner) { payments ->
            adapter?.submitList(payments.toList())
            binding.llLaunchedContainer.visibility = if (payments.isEmpty()) View.GONE else View.VISIBLE
            binding.tvLaunchedCount.text = payments.size.toString()
            binding.tvWaitPayment.visibility = if (payments.isEmpty()) View.VISIBLE else View.GONE
        }

        registrationViewModel.remainingBalanceLiveData.observe(viewLifecycleOwner) { balance ->
            val formatted = "R$ %,.2f".format(java.util.Locale("pt", "BR"), balance)
            binding.tvRemainingValue.text = formatted

            val (label, colorRes) = when {
                balance > 0 -> "A pagar" to R.color.orange
                balance < 0 -> "Excedente" to R.color.red
                else -> "Quitado" to R.color.green
            }

            binding.tvRemainingLabel.text = label
            val color = ContextCompat.getColor(requireContext(), colorRes)
            binding.tvRemainingValue.setTextColor(color)
            binding.btnCredit.isEnabled = true
            binding.btnDebit.isEnabled = true
            binding.btnStoreCredit.isEnabled = true
            binding.btnCash.isEnabled = true

            binding.btnCredit.alpha = 1.0f
            binding.btnDebit.alpha = 1.0f
            binding.btnPix.isEnabled = false
            binding.btnPix.isClickable = false
            binding.btnPix.alpha = 0.5f
            binding.btnStoreCredit.alpha = 1.0f
            binding.btnCash.alpha = 1.0f
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
                is UIState.Idle -> {}
            }
        }
    }

    private fun setupPaymentAdapter() {
        adapter = RegistrationPaymentMethodRecyclerViewAdapter(registrationViewModel, this)
        binding.rvPaymentMethods.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPaymentMethods.adapter = adapter
    }

    private fun openDetailsScreen(order: Order) {
        val orderDetailsActivityIntent = Intent(
            requireContext(),
            OrderDetailsActivity::class.java
        )
        orderDetailsActivityIntent.putExtra("order", order)
        orderDetailsActivityIntent.putExtra("orderId", order.id)
        orderDetailsActivityIntent.putExtra("isSuccess", false)

        this.startActivity(orderDetailsActivityIntent)
        requireActivity().finish()
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
        openPaymentConfig(item.paymentMethod.paymentType ?: "credito", item.id)
    }
}
