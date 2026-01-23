package com.detrapay.ui.home.payment_history

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.data.UnauthorizedException
import com.detrapay.databinding.FragmentPaymentHistoryBinding
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentHistoryFragment : Fragment() {

    private lateinit var binding: FragmentPaymentHistoryBinding
    private val viewModel: PaymentHistoryViewModel by viewModels()
    private lateinit var orderRecyclerViewAdapter: PaymentHistoryRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        observeViewModel()
        setupErrorBtn()
        viewModel.loadScreenContent()
    }

    private fun initRecyclerView() {
        orderRecyclerViewAdapter = PaymentHistoryRecyclerViewAdapter(emptyList())
        val recyclerView: RecyclerView = binding.paymentHistoryRecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        recyclerView.adapter = orderRecyclerViewAdapter
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
                    status.data?.let {
                        if (it.isEmpty()) {
                            binding.emptyListTextView.visibility = View.VISIBLE
                            binding.paymentHistoryRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyListTextView.visibility = View.GONE
                            binding.paymentHistoryRecyclerView.visibility = View.VISIBLE
                            orderRecyclerViewAdapter.swapData(it)
                        }
                        binding.loadingView.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        binding.contentView.visibility = View.VISIBLE
                    }
                }

                is UIState.Error -> {
                    validateErrorType(status.exception)
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: "Não foi possível carregar o histórico de pagamentos."
                    binding.errorView.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupErrorBtn() {
        binding.reloadPaymentHistory.setOnClickListener {
            viewModel.loadScreenContent()
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) SessionExpiredDialog().show(
            requireActivity().supportFragmentManager,
            "SessionExpiredDialog"
        )
    }
}