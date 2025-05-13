package com.detrapay.ui.registration.payment_method

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        registrationViewModel.loadPaymentSelectionScreenContent()
        binding.totalAmountValueTxtView.text = registrationViewModel.simulationTotalAmount()

        binding.reloadPaymentMethodBtn.setOnClickListener {
            registrationViewModel.loadPaymentSelectionScreenContent()
        }

        binding.registrationPaymentMethodNextBtn.setOnClickListener {
            registrationViewModel.createOrder()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeObservers()
    }

    private fun removeObservers() {
        registrationViewModel.paymentSelectionInitialState.removeObservers(viewLifecycleOwner)
    }

    private fun setupObservers() {
        registrationViewModel.paymentSelectionInitialState.observe(viewLifecycleOwner, { status ->
            when (status) {
                is UIState.Success<RegistrationPaymentMethodInitialState> -> {
                    status.data?.let {
                        binding.loadingView.stopShimmer()
                        binding.loadingView.visibility = View.GONE
                        binding.errorView.visibility = View.GONE
                        binding.contentView.visibility = View.VISIBLE
                        setupPaymentAdapter(it)
                    }
                }

                is UIState.Error -> {
                    binding.contentView.visibility = View.GONE
                    binding.loadingView.stopShimmer()
                    binding.loadingView.visibility = View.GONE
                    binding.errorView.visibility = View.VISIBLE
                }

                is UIState.Loading -> {
                    binding.contentView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }
            }
        })

        registrationViewModel.paymentSelectionCreateOrderState.observe(
            viewLifecycleOwner, Observer { status ->
                when (status) {
                    is UIState.Success<RegistrationPaymentMethodCreateOrderState> -> {
                        status.data?.let {
                            binding.registrationPaymentMethodNextBtn.isEnabled = false
                            binding.registrationErrorTextView.visibility = View.GONE
                            binding.registrationLoading.visibility = View.GONE
                            openDetailsScreen(it.order)
                        }
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
            })
    }

    private fun openDetailsScreen(order: Order) {
        Log.d("UEHARINHA", order.toString())
        
        val orderDetailsActivityIntent = Intent(
            requireContext(),
            OrderDetailsActivity::class.java
        )
        orderDetailsActivityIntent.putExtra("order", order)
        orderDetailsActivityIntent.putExtra("orderId", order.id)


        this.startActivity(orderDetailsActivityIntent)
        requireActivity().finish()
    }

    private fun setupPaymentAdapter(registrationPaymentMethodInitialState: RegistrationPaymentMethodInitialState) {
        if (adapter == null) {
            adapter = RegistrationPaymentMethodRecyclerViewAdapter(
                values = registrationPaymentMethodInitialState.payments.toMutableList(),
                paymentMethods = registrationPaymentMethodInitialState.paymentMethods,
                listener = this
            )
            val paymentsListView: RecyclerView = binding.rvPaymentMethods
            paymentsListView.layoutManager = LinearLayoutManager(this.activity)
            paymentsListView.adapter = adapter
        }
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
}