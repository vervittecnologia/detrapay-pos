package com.detrapay.ui.home.order_list

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.databinding.FragmentOrderListBinding
import com.detrapay.ui.order_details.OrderDetailsActivity
import com.detrapay.ui.registration.RegistrationActivity
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.afterTextChanged
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderListFragment : Fragment(), OrderRecyclerViewAdapter.OnItemClickListener {

    private lateinit var binding: FragmentOrderListBinding
    private val viewModel: OrderListViewModel by viewModels()
    private lateinit var orderRecyclerViewAdapter: OrderRecyclerViewAdapter
    private var currentOrders: List<Order> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        setupSearchBar()
        observeViewModel()
        setupErrorBtn()
        setupSwipeToRefresh()
        binding.emptyActionButton.setOnClickListener {
            startActivity(Intent(requireContext(), RegistrationActivity::class.java))
        }
        viewModel.loadScreenContent()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadScreenContent(true)
        }
    }

    private fun setupSearchBar() {
        binding.searchOrderTextInput.afterTextChanged {
            orderRecyclerViewAdapter.filterData(binding.searchOrderTextInput.text.toString())
            updateEmptyState()
        }
    }

    private fun initRecyclerView() {
        orderRecyclerViewAdapter = OrderRecyclerViewAdapter(emptyList(), this)
        val recyclerView: RecyclerView = binding.orderList
        recyclerView.layoutManager = LinearLayoutManager(this.activity)
        recyclerView.adapter = orderRecyclerViewAdapter
    }

    override fun onItemClick(item: Order) {
        val orderDetailsActivityIntent = Intent(
            requireContext(),
            OrderDetailsActivity::class.java
        )
        orderDetailsActivityIntent.putExtra("order", item)
        orderDetailsActivityIntent.putExtra("orderId", item.id)
        this.startActivity(orderDetailsActivityIntent)
    }

    private fun observeViewModel() {
        viewModel.orderListState.observe(viewLifecycleOwner, Observer { status ->
            when (status) {
                is UIState.Loading -> {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.contentView.visibility = View.GONE
                        binding.errorView.visibility = View.GONE
                        binding.loadingView.visibility = View.VISIBLE
                        binding.loadingView.startShimmer()
                    }
                }

                is UIState.Success -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    status.data?.let {
                        currentOrders = it
                        orderRecyclerViewAdapter.swapData(it)
                        updateEmptyState()
                        binding.loadingView.apply {
                            stopShimmer()
                            visibility = View.GONE
                        }
                        binding.contentView.visibility = View.VISIBLE
                    }
                }

                is UIState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    validateErrorType(status.exception)
                    binding.loadingView.visibility = View.GONE
                    binding.errorTxtView.text =
                        status.message ?: getString(R.string.orders_default_error_message)
                    binding.errorView.visibility = View.VISIBLE
                }
                is UIState.Idle -> {}
            }
        })
    }

    private fun setupErrorBtn() {
        binding.reloadOrders.setOnClickListener {
            viewModel.loadScreenContent()
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager, error)
        }
    }

    private fun updateEmptyState() {
        val filteredItemCount = orderRecyclerViewAdapter.itemCount
        val searchText = binding.searchOrderTextInput.text?.toString().orEmpty()
        val hasOrders = currentOrders.isNotEmpty()
        val showEmpty = !hasOrders || filteredItemCount == 0

        binding.emptyStateContainer.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.orderList.visibility = if (showEmpty) View.GONE else View.VISIBLE

        if (!showEmpty) {
            return
        }

        if (hasOrders && searchText.isNotBlank()) {
            binding.emptyListTextView.text = getString(R.string.home_orders_no_results)
            binding.emptyListSubtitle.text = getString(R.string.home_orders_no_results_subtitle)
            binding.emptyActionButton.visibility = View.GONE
        } else {
            binding.emptyListTextView.text = getString(R.string.home_orders_empty_title)
            binding.emptyListSubtitle.text = getString(R.string.home_orders_empty_subtitle)
            binding.emptyActionButton.visibility = View.VISIBLE
        }
    }
}
