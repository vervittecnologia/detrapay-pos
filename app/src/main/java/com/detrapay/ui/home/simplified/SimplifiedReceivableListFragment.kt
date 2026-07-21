package com.detrapay.ui.home.simplified

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.detrapay.BuildConfig
import com.detrapay.R
import com.detrapay.data.UnauthorizedException
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.SellerAppMode
import com.detrapay.databinding.FragmentSimplifiedReceivableListBinding
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.login.LoginActivity
import com.detrapay.ui.payment.PaymentDialogFragment
import com.detrapay.ui.session_expired_dialog.SessionExpiredDialog
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.DebugConstants
import com.detrapay.ui.util.DeviceUtils
import com.detrapay.ui.util.PaymentTypeRules
import com.detrapay.ui.util.afterTextChanged
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SimplifiedReceivableListFragment : Fragment(),
    SimplifiedReceivableRecyclerViewAdapter.OnPayClickListener,
    DirectCheckoutOrderRecyclerViewAdapter.OnOrderActionListener {

    private lateinit var binding: FragmentSimplifiedReceivableListBinding
    private val homeViewModel: HomeViewModel by activityViewModels()
    private val viewModel: SimplifiedReceivableListViewModel by viewModels()
    private lateinit var receivableAdapter: SimplifiedReceivableRecyclerViewAdapter
    private lateinit var directOrderAdapter: DirectCheckoutOrderRecyclerViewAdapter
    private var isDirectCheckoutMode = false
    private var currentReceivables: List<OrderReceivable> = emptyList()
    private var currentOrders: List<Order> = emptyList()
    private var selectedDirectOrder: Order? = null
    private var shouldShowDirectOrderDetails = false

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
        observeDirectOrders()
        observeDirectPaymentMethods()
        observeDirectPendingPayment()
        observeDirectManualPayment()
        setupDirectCheckoutBottomSheetListeners()
    }

    private fun setupRecyclerView() {
        receivableAdapter = SimplifiedReceivableRecyclerViewAdapter(emptyList(), this)
        directOrderAdapter = DirectCheckoutOrderRecyclerViewAdapter(emptyList(), this)
        binding.simplifiedResultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.simplifiedResultsList.adapter = receivableAdapter
    }

    private fun setupSearch() {
        binding.simplifiedSearchInput.afterTextChanged {
            val query = binding.simplifiedSearchInput.text?.toString().orEmpty()
            if (isDirectCheckoutMode) {
                directOrderAdapter.filterData(query)
            } else {
                receivableAdapter.filterData(query)
            }
            updateEmptyState()
        }
    }

    private fun setupSwipeToRefresh() {
        binding.simplifiedSwipeRefresh.setOnRefreshListener {
            reloadCurrentMode(forceRefresh = true)
        }
    }

    private fun setupLogout() {
        val logoutClickListener = View.OnClickListener {
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
        binding.logoutButton.setOnClickListener(logoutClickListener)
        binding.directLogoutButton.setOnClickListener(logoutClickListener)
    }

    private fun setupReload() {
        binding.reloadSimplifiedReceivables.setOnClickListener {
            reloadCurrentMode()
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
            val nextDirectMode = homeState?.sellerAppMode == SellerAppMode.DIRECT_CHECKOUT
            if (nextDirectMode != isDirectCheckoutMode) {
                isDirectCheckoutMode = nextDirectMode
                applyModeUi()
            }
            reloadCurrentMode()
        })
    }

    private fun applyModeUi() {
        binding.simplifiedHeaderLayout.visibility = if (isDirectCheckoutMode) View.GONE else View.VISIBLE
        binding.operatorCard.visibility = if (isDirectCheckoutMode) View.GONE else View.VISIBLE
        binding.directModeHeader.visibility = if (isDirectCheckoutMode) View.VISIBLE else View.GONE
        binding.resultsHeader.visibility = View.GONE
        binding.simplifiedResultsList.adapter = if (isDirectCheckoutMode) directOrderAdapter else receivableAdapter
        binding.simplifiedSearchInputLayout.hint = getString(R.string.simplified_receivables_search_hint)
    }

    private fun reloadCurrentMode(forceRefresh: Boolean = false) {
        if (isDirectCheckoutMode) {
            viewModel.loadDirectCheckoutOrders(forceRefresh)
        } else {
            viewModel.loadScreenContent(forceRefresh)
        }
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
                    currentReceivables = state.data.orEmpty()
                    receivableAdapter.swapData(currentReceivables)
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

    private fun observeDirectOrders() {
        viewModel.directOrderListState.observe(viewLifecycleOwner, Observer { state ->
            if (!isDirectCheckoutMode) return@Observer
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
                    currentOrders = state.data.orEmpty()
                    directOrderAdapter.swapData(currentOrders)
                    updateResultsCount()
                    updateEmptyState()
                }
                is UIState.Error -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    validateErrorType(state.exception)
                    binding.loadingView.stopShimmer()
                    binding.loadingView.visibility = View.GONE
                    binding.contentContainer.visibility = View.GONE
                    binding.errorTxtView.text = state.message ?: getString(R.string.direct_checkout_error)
                    binding.errorView.visibility = View.VISIBLE
                }
                is UIState.Idle -> Unit
            }
        })
    }

    private fun updateEmptyState() {
        val itemCount = if (isDirectCheckoutMode) directOrderAdapter.itemCount else receivableAdapter.itemCount
        val hasBackingItems = if (isDirectCheckoutMode) currentOrders.isNotEmpty() else currentReceivables.isNotEmpty()
        val showEmpty = !hasBackingItems || itemCount == 0
        binding.emptyTitle.text = if (isDirectCheckoutMode) {
            getString(R.string.direct_checkout_empty_title)
        } else {
            getString(R.string.simplified_receivables_empty_title)
        }
        binding.emptySubtitle.text = if (isDirectCheckoutMode) {
            getString(R.string.direct_checkout_empty_subtitle)
        } else {
            getString(R.string.simplified_receivables_empty_subtitle)
        }
        binding.emptyStateContainer.visibility = if (showEmpty) View.VISIBLE else View.GONE
        binding.simplifiedResultsList.visibility = if (showEmpty) View.GONE else View.VISIBLE
        updateResultsCount()
    }

    private fun updateResultsCount() {
        if (!isDirectCheckoutMode) return
        val count = directOrderAdapter.itemCount
        binding.resultsCount.text = resources.getQuantityString(
            R.plurals.direct_checkout_order_count,
            count,
            count
        )
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

    override fun onOrderClick(item: Order) {
        selectedDirectOrder = item
        shouldShowDirectOrderDetails = true
        viewModel.loadPaymentMethods()
    }

    private fun observeDirectPaymentMethods() {
        viewModel.paymentMethodsState.observe(viewLifecycleOwner) { state ->
            if (!isDirectCheckoutMode) return@observe
            when (state) {
                is UIState.Loading -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = true
                }
                is UIState.Success -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    if (shouldShowDirectOrderDetails) {
                        shouldShowDirectOrderDetails = false
                        showDirectOrderDetails()
                    }
                }
                is UIState.Error -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    shouldShowDirectOrderDetails = false
                    Toast.makeText(
                        requireContext(),
                        state.message ?: getString(R.string.order_details_add_payment_load_error),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                is UIState.Idle -> Unit
            }
        }
    }

    private fun observeDirectPendingPayment() {
        viewModel.pendingPaymentState.observe(viewLifecycleOwner) { state ->
            if (!isDirectCheckoutMode) return@observe
            when (state) {
                is UIState.Loading -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = true
                }
                is UIState.Success -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    val pendingPayment = viewModel.consumeDirectCheckoutPendingPayment() ?: return@observe
                    if (PaymentTypeRules.isDirectNoFeePaymentType(pendingPayment.paymentType)) {
                        viewModel.confirmDirectCheckoutManualPayment(
                            pendingPayment = pendingPayment,
                            paymentData = buildManualPaymentData(pendingPayment.amount),
                        )
                    } else {
                        openPaymentDialog(pendingPayment)
                    }
                }
                is UIState.Error -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        state.message ?: getString(R.string.order_details_add_payment_load_error),
                        Toast.LENGTH_LONG,
                    ).show()
                    viewModel.clearDirectCheckoutPaymentState()
                }
                is UIState.Idle -> Unit
            }
        }
    }

    private fun observeDirectManualPayment() {
        viewModel.manualPaymentState.observe(viewLifecycleOwner) { state ->
            if (!isDirectCheckoutMode) return@observe
            when (state) {
                is UIState.Loading -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = true
                }
                is UIState.Success -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.order_details_payment_success_toast),
                        Toast.LENGTH_SHORT,
                    ).show()
                    viewModel.clearDirectCheckoutPaymentState()
                    reloadCurrentMode(forceRefresh = true)
                }
                is UIState.Error -> {
                    binding.simplifiedSwipeRefresh.isRefreshing = false
                    Toast.makeText(
                        requireContext(),
                        state.message ?: getString(R.string.default_error_message),
                        Toast.LENGTH_LONG,
                    ).show()
                    viewModel.clearDirectCheckoutPaymentState()
                }
                is UIState.Idle -> Unit
            }
        }
    }

    private fun setupDirectCheckoutBottomSheetListeners() {
        childFragmentManager.setFragmentResultListener(
            DirectCheckoutOrderDetailsBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val selectedType = bundle.getString(DirectCheckoutOrderDetailsBottomSheet.RESULT_PAYMENT_TYPE)
                ?: return@setFragmentResultListener
            openDirectPaymentConfig(selectedType)
        }

        childFragmentManager.setFragmentResultListener(
            DirectCheckoutPaymentConfigBottomSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val order = selectedDirectOrder ?: return@setFragmentResultListener
            viewModel.addDirectCheckoutPendingPayment(
                order = order,
                paymentType = bundle.getString(DirectCheckoutPaymentConfigBottomSheet.RESULT_PAYMENT_TYPE).orEmpty(),
                amount = bundle.getDouble(DirectCheckoutPaymentConfigBottomSheet.RESULT_AMOUNT),
                installments = bundle.getInt(DirectCheckoutPaymentConfigBottomSheet.RESULT_INSTALLMENTS, 1),
            )
        }
    }

    private fun showDirectOrderDetails() {
        val order = selectedDirectOrder ?: return
        val types = viewModel.availablePaymentTypes()
        if (types.isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.order_details_add_payment_empty_methods),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val summary = DirectCheckoutOrderPresentation.summary(order)
        DirectCheckoutOrderDetailsBottomSheet.newInstance(
            orderId = order.id,
            customerName = order.customer.name,
            pendingAmount = DirectCheckoutOrderPresentation.formatCurrency(summary.missingAmount),
            availableTypes = types,
        ).show(childFragmentManager, DIRECT_ORDER_DETAILS_TAG)
    }

    private fun openDirectPaymentConfig(paymentType: String) {
        val order = selectedDirectOrder ?: return
        val amount = DirectCheckoutOrderPresentation.summary(order).missingAmount
        DirectCheckoutPaymentConfigBottomSheet.newInstance(
            paymentType = paymentType,
            defaultAmount = amount,
        ).show(childFragmentManager, DIRECT_PAYMENT_CONFIG_TAG)
    }

    private fun openPaymentDialog(pendingPayment: DirectCheckoutPendingPayment) {
        viewModel.clearDirectCheckoutPaymentState()
        PaymentDialogFragment(
            listener = object : PaymentDialogFragment.PaymentListener {
                override fun onResult(paymentData: PaymentData?) {
                    if (paymentData != null) {
                        reloadCurrentMode(forceRefresh = true)
                    }
                }
            },
            orderId = pendingPayment.order.id,
            receivableItem = pendingPayment.receivable,
            serial = getSerialForPrePay()
        ).show(parentFragmentManager, "PaymentDialogFragment")
    }

    private fun buildManualPaymentData(amountFinal: Double): PaymentData {
        val now = Date()
        val locale = Locale("pt", "BR")
        return PaymentData(
            date = SimpleDateFormat("dd/MM/yyyy", locale).format(now),
            time = SimpleDateFormat("HH:mm:ss", locale).format(now),
            amountFinal = amountFinal,
        )
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

    companion object {
        private const val DIRECT_ORDER_DETAILS_TAG = "DirectCheckoutOrderDetails"
        private const val DIRECT_PAYMENT_CONFIG_TAG = "DirectCheckoutPaymentConfig"
    }
}
