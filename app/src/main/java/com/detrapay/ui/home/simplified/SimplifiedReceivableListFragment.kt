package com.detrapay.ui.home.simplified

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.PaymentData
import com.detrapay.databinding.FragmentSimplifiedReceivableListBinding
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import com.detrapay.ui.util.afterTextChanged
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SimplifiedReceivableListFragment : Fragment(),
    SimplifiedReceivableRecyclerViewAdapter.OnPayClickListener {

    private lateinit var binding: FragmentSimplifiedReceivableListBinding
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: SimplifiedReceivableListViewModel by viewModels()
    private lateinit var adapter: SimplifiedReceivableRecyclerViewAdapter
    private var currentItems: List<OrderReceivable> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSimplifiedReceivableListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupSwipeToRefresh()
        setupLogout()
        setupReload()
        observeHomeState()
        observeReceivables()
        viewModel.loadScreenContent()
    }

    private fun setupRecyclerView() {
        adapter = SimplifiedReceivableRecyclerViewAdapter(emptyList(), this)
        binding.simplifiedResultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.simplifiedResultsList.adapter = adapter
    }

    private fun setupSearch() {
        binding.simplifiedSearchInput.afterTextChanged {
            adapter.filterData(binding.simplifiedSearchInput.text?.toString().orEmpty())
            updateEmptyState()
        }
    }

    private fun setupSwipeToRefresh() {
        binding.simplifiedSwipeRefresh.setOnRefreshListener {
            viewModel.loadScreenContent(forceRefresh = true)
        }
    }

    private fun setupLogout() {
        binding.logoutButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    homeViewModel.logout()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .show()
        }
    }

    private fun setupReload() {
        binding.reloadSimplifiedReceivables.setOnClickListener {
            viewModel.loadScreenContent()
        }
    }

    private fun observeHomeState() {
        homeViewModel.homeState.observe(viewLifecycleOwner, Observer { state ->
            val homeState = (state as? UIState.Success)?.data
            binding.operatorTitle.text = homeState?.companyName?.takeIf { it.isNotBlank() }
                ?: getString(R.string.home_default_company_name)
            binding.operatorDocument.text = getString(
                R.string.home_company_document,
                formatCnpj(homeState?.companyDocument.orEmpty())
            )
        })
    }

    private fun observeReceivables() {
        viewModel.receivableListState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is UIState.Loading -> {
                    binding.errorView.visibility = View.GONE
                    binding.contentContainer.visibility = View.GONE
                    binding.loadingView.visibility = View.VISIBLE
                    binding.loadingView.startShimmer()
                }
                is UIState.Success -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    binding.loadingView.stopShimmer()
                    binding.loadingView.visibility = View.GONE
                    binding.contentContainer.visibility = View.VISIBLE
                    currentItems = state.data.orEmpty()
                    adapter.swapData(currentItems)
                    updateEmptyState()
                }
                is UIState.Error -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    validateErrorType(state.exception)
                    binding.loadingView.stopShimmer()
                    binding.loadingView.visibility = View.GONE
                    binding.contentContainer.visibility = View.GONE
                    binding.errorTxtView.text = state.message ?: getString(R.string.simplified_receivables_error)
                    binding.errorView.visibility = View.VISIBLE
                }
                is UIState.Idle -> Unit
            }
        })
    }

    private fun updateEmptyState() {
        val showEmpty = currentItems.isEmpty() || adapter.itemCount == 0
        binding.emptyStateContainer.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.simplifiedResultsList.visibility = if (showEmpty) View.GONE else View.VISIBLE
    }

    override fun onPayClick(item: OrderReceivable) {
        PaymentDialogFragment(
            listener = object : PaymentDialogFragment.PaymentListener {
                override fun onResult(paymentData: PaymentData?) {
                    if (paymentData != null) {
                        viewModel.loadScreenContent(forceRefresh = true)
                    }
                }
            },
            orderId = item.order.id,
            receivableItem = item.receivable,
            serial = getSerialForPrePay()
        ).show(parentFragmentManager, "PaymentDialogFragment")
    }

    private fun getSerialForPrePay(): String {
        return if (BuildConfig.DEBUG) {
            DebugConstants.DEBUG_SPLIT_DEVICE_ID
        } else {
            DeviceUtils.getSerialNumber()
        }
    }

    private fun validateErrorType(error: Exception?) {
        if (error is UnauthorizedException) {
            SessionExpiredDialog.showIfNeeded(requireActivity().supportFragmentManager, error)
        }
    }

    private fun formatCnpj(document: String): String {
        val numbers = document.filter(Char::isDigit)
        if (numbers.length != 14) return if (document.isBlank()) "-" else document

        return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
            "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
    }
}
