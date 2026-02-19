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
        binding.btnCredit.setOnClickListener { navigateToDetail("credito") }
        binding.btnDebit.setOnClickListener { navigateToDetail("debito") }
        binding.btnPix.setOnClickListener { registrationViewModel.addPaymentByType("pix") }
        binding.btnCash.setOnClickListener { registrationViewModel.addPaymentByType("dinheiro") }

        binding.reloadPaymentMethodBtn.setOnClickListener {
            registrationViewModel.loadPaymentSelectionScreenContent()
        }

        binding.registrationPaymentMethodNextBtn.setOnClickListener {
            registrationViewModel.createOrder()
        }

        setupPaymentAdapter()
    }

    private fun navigateToDetail(type: String, paymentId: Long = -1L) {
        val bundle = Bundle().apply {
            putString("paymentType", type)
            putLong("paymentId", paymentId)
        }
        findNavController().navigate(R.id.action_paymentMethodFragment_to_paymentDetailFragment, bundle)
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
            binding.tvStatusMessage.text = "SALDO DE $formatted ${if (balance > 0) "PENDENTE" else "QUITADO"}"
            
            val color = if (balance > 0) ContextCompat.getColor(requireContext(), R.color.orange)
                        else ContextCompat.getColor(requireContext(), R.color.green)
            
            binding.tvRemainingValue.setTextColor(color)
            binding.tvStatusMessage.setTextColor(color)
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
        navigateToDetail(item.paymentMethod.paymentType ?: "credito", item.id)
    }
}
