package com.detrapay.ui.home.payment_history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.local.Payment
import com.detrapay.databinding.FragmentPaymentHistoryBinding
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentHistoryFragment : Fragment() {

    private lateinit var binding: FragmentPaymentHistoryBinding
    private val viewModel: PaymentHistoryViewModel by viewModels()
    private lateinit var orderRecyclerViewAdapter: PaymentHistoryRecyclerViewAdapter
    private var currentPayments: List<Payment> = emptyList()
    private var currentFilter: PaymentFilter = PaymentFilter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPaymentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        observeViewModel()
        setupErrorBtn()
        setupFilters()
        viewModel.loadScreenContent()
    }

    private fun initRecyclerView() {
        orderRecyclerViewAdapter = PaymentHistoryRecyclerViewAdapter(emptyList())
        val recyclerView: RecyclerView = binding.paymentHistoryRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = orderRecyclerViewAdapter
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.filterSuccessChip -> PaymentFilter.SUCCESS
                R.id.filterErrorChip -> PaymentFilter.ERROR
                else -> PaymentFilter.ALL
            }
            renderPayments()
        }
    }

    private fun observeViewModel() {
        viewModel.paymentListState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    binding.contentView.visibility = View.GONE
                    binding.errorView.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }

                is UIState.Success -> {
                    currentPayments = status.data.orEmpty()
                    binding.loadingView.apply {
                        stopShimmer()
                        visibility = View.GONE
                    }
                    binding.contentView.visibility = View.VISIBLE
                    renderPayments()
                }

                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: getString(R.string.payment_history_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }

                is UIState.Idle -> Unit
            }
        })
    }

    private fun renderPayments() {
        val filtered = when (currentFilter) {
            PaymentFilter.ALL -> currentPayments
            PaymentFilter.SUCCESS -> currentPayments.filter { it.result == 0 }
            PaymentFilter.ERROR -> currentPayments.filter { it.result != 0 }
        }

        if (filtered.isEmpty()) {
            binding.paymentHistoryRecyclerView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
            if (currentPayments.isEmpty()) {
                binding.emptyListTextView.text = getString(R.string.payment_history_empty_title)
                binding.emptyListSubtitle.text = getString(R.string.payment_history_empty_subtitle)
            } else {
                binding.emptyListTextView.text = getString(R.string.payment_history_no_results_title)
                binding.emptyListSubtitle.text = getString(R.string.payment_history_no_results_subtitle)
            }
        } else {
            binding.emptyStateContainer.visibility = View.GONE
            binding.paymentHistoryRecyclerView.visibility = View.VISIBLE
            orderRecyclerViewAdapter.swapData(filtered)
        }
    }

    private fun setupErrorBtn() {
        binding.reloadPaymentHistory.setOnClickListener {
            viewModel.loadScreenContent()
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager, error)
        }
    }

    private enum class PaymentFilter {
        ALL,
        SUCCESS,
        ERROR,
    }
}
