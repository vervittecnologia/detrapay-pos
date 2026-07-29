package com.detrapay.ui.home.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentMethod
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.orders.OrderPresentation
import com.detrapay.ui.home.orders.OrdersViewModel
import com.detrapay.ui.payment.PaymentDialogViewModel
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules

@Composable
fun OrdersRoute(
    homeViewModel: HomeViewModel,
    viewModel: OrdersViewModel,
    paymentViewModel: PaymentDialogViewModel,
    terminalSerial: String,
    defaultCompanyName: String,
    defaultCompanyDocument: String,
    ordersErrorMessage: String,
    installmentErrorMessage: String,
    addPaymentLoadErrorMessage: String,
    paymentSuccessMessage: String,
    invalidSimulatorAmountMessage: String,
    orderToOpen: Order?,
    onOrderOpened: () -> Unit,
    cameraAvailable: Boolean,
    cameraCaptureError: String?,
    onTakeOrderPhoto: (Int) -> Unit,
    onEffect: (OrderFlowEffect) -> Unit,
) {
    var localState by remember { mutableStateOf(OrderFlowLocalState()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethod>>(emptyList()) }
    var companyName by remember { mutableStateOf(defaultCompanyName) }
    var companyDocument by remember { mutableStateOf(defaultCompanyDocument) }

    val homeState by homeViewModel.homeState.observeAsState()
    val orderState by viewModel.orderListState.observeAsState()
    val paymentMethodsState by viewModel.paymentMethodsState.observeAsState()
    val feesState by viewModel.calculateFeesState.observeAsState()
    val paymentRecordState by viewModel.paymentRecordState.observeAsState()
    val deletePaymentState by viewModel.deletePaymentState.observeAsState()
    val orderDocumentsState by viewModel.orderDocumentsState.observeAsState(OrderDocumentsUiState())
    val inPagePaymentState by paymentViewModel.paymentState.observeAsState(UIState.Idle())

    LaunchedEffect(Unit) {
        paymentViewModel.init()
        viewModel.loadOrders(forceRefresh = false)
        viewModel.prefetchRegistrationData()
    }

    LaunchedEffect(homeState) {
        val data = (homeState as? UIState.Success)?.data ?: return@LaunchedEffect
        companyName = data.companyName.ifBlank { defaultCompanyName }
        companyDocument = formatCnpj(data.companyDocument).ifBlank { defaultCompanyDocument }
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is UIState.Loading -> {
                if (!isRefreshing) {
                    isLoading = true
                    errorMessage = null
                }
            }
            is UIState.Success -> {
                isLoading = false
                isRefreshing = false
                errorMessage = null
                orders = state.data.orEmpty()
            }
            is UIState.Error -> {
                isLoading = false
                isRefreshing = false
                orders = emptyList()
                errorMessage = state.message ?: ordersErrorMessage
                state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
            }
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(paymentMethodsState) {
        when (val state = paymentMethodsState) {
            is UIState.Success -> paymentMethods = state.data.orEmpty()
            is UIState.Error -> {
                onEffect(
                    OrderFlowEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage),
                )
                state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(feesState) {
        when (val state = feesState) {
            is UIState.Loading -> {
                localState = when (localState.feeRequestTarget) {
                    OrderFeeRequestTarget.CheckoutCredit -> {
                        localState.copy(feesLoading = true, feesError = null)
                    }
                    OrderFeeRequestTarget.Simulator -> {
                        localState.copy(simulatorLoading = true, simulatorError = null)
                    }
                    null -> localState
                }
            }
            is UIState.Success -> {
                val installments = state.data?.data.orEmpty().firstOrNull()?.installments.orEmpty()
                localState = when (localState.feeRequestTarget) {
                    OrderFeeRequestTarget.CheckoutCredit -> {
                        OrderFlowReducer.feesLoaded(localState, installments, installmentErrorMessage)
                    }
                    OrderFeeRequestTarget.Simulator -> {
                        OrderFlowReducer.simulatorLoaded(localState, installments, installmentErrorMessage)
                    }
                    null -> OrderFlowReducer.discardFeeResponse(localState)
                }
            }
            is UIState.Error -> {
                val message = state.message ?: installmentErrorMessage
                when (localState.feeRequestTarget) {
                    OrderFeeRequestTarget.CheckoutCredit -> {
                        localState = OrderFlowReducer.feesFailed(localState, message)
                        state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
                    }
                    OrderFeeRequestTarget.Simulator -> {
                        localState = OrderFlowReducer.simulatorFailed(localState, message)
                        state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
                    }
                    null -> {
                        if (OrderFlowReducer.isStaleFeeResponse(localState)) {
                            state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
                        }
                        localState = OrderFlowReducer.discardFeeResponse(localState)
                    }
                }
            }
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(inPagePaymentState) {
        when (val state = inPagePaymentState) {
            is UIState.Success -> {
                if (state.data?.pendingConfirmation == true) return@LaunchedEffect
                onEffect(OrderFlowEffect.ShowToast(paymentSuccessMessage, long = false))
                localState = localState.copy(
                    step = OrderFlowStep.Orders,
                    activePaymentRequest = null,
                    paymentSubmissionInFlight = false,
                )
                viewModel.clearPaymentState()
                isRefreshing = true
                viewModel.loadOrders(forceRefresh = true)
            }
            is UIState.Error -> {
                state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
            }
            is UIState.Loading,
            is UIState.Idle -> Unit
        }
    }

    LaunchedEffect(paymentRecordState) {
        when (val state = paymentRecordState) {
            is UIState.Success -> {
                onEffect(OrderFlowEffect.ShowToast(paymentSuccessMessage, long = false))
                localState = localState.copy(
                    step = OrderFlowStep.Orders,
                    activePaymentRequest = null,
                    paymentSubmissionInFlight = false,
                )
                viewModel.clearPaymentState()
                isRefreshing = true
                viewModel.loadOrders(forceRefresh = true)
            }
            is UIState.Error -> {
                localState = localState.copy(
                    step = OrderFlowStep.Review,
                    paymentSubmissionInFlight = false,
                )
                onEffect(OrderFlowEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage))
                state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
                viewModel.clearPaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(orderToOpen) {
        if (orderToOpen == null) return@LaunchedEffect
        localState = OrderFlowReducer.showDetail(localState, orderToOpen)
        orders = listOf(orderToOpen) + orders.filterNot { it.id == orderToOpen.id }
        onOrderOpened()
    }

    LaunchedEffect(deletePaymentState) {
        when (val state = deletePaymentState) {
            is UIState.Success -> {
                val updatedOrder = state.data ?: return@LaunchedEffect
                orders = orders.map { order ->
                    if (order.id == updatedOrder.id) updatedOrder else order
                }
                localState = localState.copy(selectedOrder = updatedOrder)
                onEffect(OrderFlowEffect.ShowToast("Pagamento excluido.", long = false))
                viewModel.clearDeletePaymentState()
            }
            is UIState.Error -> {
                onEffect(
                    OrderFlowEffect.ShowToast(
                        state.message ?: "Nao foi possivel excluir o pagamento.",
                    ),
                )
                state.exception?.let { onEffect(OrderFlowEffect.ShowSessionExpired(it)) }
                viewModel.clearDeletePaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    fun startConfirmedRequest() {
        if (localState.paymentSubmissionInFlight) return
        val order = localState.selectedOrder ?: return
        val paymentMethod = localState.selectedPaymentMethod ?: return
        val review = localState.paymentReview ?: return
        val selectedMethod = if (PaymentTypeRules.normalize(paymentMethod.paymentType) == "credito") {
            viewModel.resolvePaymentMethod(paymentMethod.paymentType.orEmpty(), review.installments)
        } else {
            paymentMethod
        }
        if (selectedMethod == null) {
            onEffect(OrderFlowEffect.ShowToast("Metodo de pagamento indisponivel para a condicao selecionada."))
            localState = localState.copy(step = OrderFlowStep.Installments)
            return
        }
        val request = localState.activePaymentRequest?.takeIf { existing ->
            existing.order.id == order.id &&
                existing.paymentMethod.id == selectedMethod.id &&
                existing.amount == review.amountOriginal &&
                existing.amountFinal == review.amountFinal &&
                existing.installments == review.installments
        } ?: OrderPaymentRequest(
            order = order,
            paymentMethod = selectedMethod,
            amount = review.amountOriginal,
            amountFinal = review.amountFinal,
            installments = review.installments,
        )
        localState = localState.copy(activePaymentRequest = request)
        when (val route = OrderPaymentRouter.routeFor(request)) {
            is OrderPaymentRoute.Online -> {
                localState = localState.copy(
                    selectedPaymentMethod = selectedMethod,
                    activePaymentRequest = route.request,
                    step = OrderFlowStep.Waiting,
                )
                paymentViewModel.payOrder(route.request, terminalSerial)
            }
            is OrderPaymentRoute.RecordOnly -> {
                localState = localState.copy(paymentSubmissionInFlight = true)
                viewModel.recordOfflinePayment(route.request)
            }
        }
    }

    var isFirstResume by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!isFirstResume) {
                    isRefreshing = true
                    viewModel.loadOrders(forceRefresh = true)
                }
                isFirstResume = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OrdersScreen(
        state = OrdersUiState(
            companyName = companyName,
            companyDocument = companyDocument,
            orders = orders,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            errorMessage = errorMessage,
            paymentMethods = paymentMethods,
            local = localState,
            inPagePaymentState = inPagePaymentState,
            orderDocuments = orderDocumentsState,
        ),
        cameraAvailable = cameraAvailable,
        cameraCaptureError = cameraCaptureError,
        onTakeOrderPhoto = onTakeOrderPhoto,
        onAction = { action ->
            when (action) {
                OrderFlowAction.Logout -> onEffect(OrderFlowEffect.ShowLogoutConfirmation)
                OrderFlowAction.Reload -> viewModel.loadOrders(forceRefresh = true)
                OrderFlowAction.NewOrder -> onEffect(OrderFlowEffect.NavigateToRegistration)
                is OrderFlowAction.OrderPay -> {
                    localState = OrderFlowReducer.startPayment(localState, action.order)
                    viewModel.clearFeesState()
                    viewModel.loadPaymentMethods()
                }
                is OrderFlowAction.OrderDetail -> {
                    localState = OrderFlowReducer.showDetail(localState, action.order)
                }
                is OrderFlowAction.DeletePayment -> {
                    localState.selectedOrder?.let { order ->
                        viewModel.deleteOfflinePayment(order.id, action.receivable)
                    }
                }
                OrderFlowAction.ReloadDocuments -> {
                    localState.selectedOrder?.let { viewModel.loadOrderDocuments(it.id) }
                }
                OrderFlowAction.RetryPhotoUpload -> {
                    localState.selectedOrder?.let { viewModel.retryOrderPhotoUpload(it.id) }
                }
                OrderFlowAction.DiscardPendingPhoto -> viewModel.discardPendingOrderPhoto()
                OrderFlowAction.Back -> {
                    if (localState.step == OrderFlowStep.Waiting) {
                        paymentViewModel.abortPayment()
                    }
                    localState = OrderFlowReducer.back(localState)
                }
                OrderFlowAction.ExitPayment -> {
                    if (localState.step == OrderFlowStep.Waiting) {
                        paymentViewModel.abortPayment()
                    }
                    localState = OrderFlowReducer.exitPayment(localState)
                    viewModel.clearFeesState()
                    viewModel.clearPaymentState()
                }
                is OrderFlowAction.Key -> {
                    localState = OrderFlowReducer.applyPaymentKey(localState, action.value)
                }
                OrderFlowAction.UsePendingAmount -> {
                    localState.selectedOrder?.let { order ->
                        localState = OrderFlowReducer.usePendingAmount(localState, order)
                    }
                }
                OrderFlowAction.OpenMethods -> {
                    localState = OrderFlowReducer.openMethods(localState)
                }
                is OrderFlowAction.SelectPaymentMethod -> {
                    localState = OrderFlowReducer.selectPaymentMethod(localState, action.paymentMethod)
                    viewModel.clearFeesState()
                }
                OrderFlowAction.ContinueAmount -> {
                    val method = localState.selectedPaymentMethod
                    val amount = currentPaymentAmount(localState)
                    when {
                        method == null -> Unit
                        amount <= 0.0 -> {
                            localState = localState.copy(feesError = "Informe um valor maior que zero.")
                        }
                        PaymentTypeRules.isDirectNoFeePaymentType(method.paymentType) -> {
                            localState = OrderFlowReducer.openDirectReview(localState)
                            viewModel.clearFeesState()
                        }
                        else -> {
                            val previous = localState
                            localState = OrderFlowReducer.startCheckoutQuote(localState)
                            if (!previous.feeRequestInFlight &&
                                localState.feeRequestTarget == OrderFeeRequestTarget.CheckoutCredit
                            ) {
                                viewModel.calculateFees(amount, method.paymentType.orEmpty())
                            }
                        }
                    }
                }
                is OrderFlowAction.SelectInstallment -> {
                    localState = OrderFlowReducer.selectInstallment(localState, action.installment)
                }
                OrderFlowAction.ContinueInstallments -> {
                    localState = OrderFlowReducer.openInstallmentReview(localState)
                }
                OrderFlowAction.ConfirmPayment -> {
                    startConfirmedRequest()
                }
                OrderFlowAction.RetryInPagePayment -> {
                    localState.activePaymentRequest?.let { request ->
                        paymentViewModel.payOrder(request, terminalSerial)
                    }
                }
                OrderFlowAction.FinishInPagePayment -> {
                    localState = localState.copy(
                        step = OrderFlowStep.Orders,
                        activePaymentRequest = null,
                        paymentSubmissionInFlight = false,
                    )
                    viewModel.clearPaymentState()
                    isRefreshing = true
                    viewModel.loadOrders(forceRefresh = true)
                }
                is OrderFlowAction.CopyPaymentCode -> {
                    onEffect(OrderFlowEffect.CopyPaymentText(action.text))
                }
                OrderFlowAction.OpenSimulator -> {
                    localState = OrderFlowReducer.openSimulator(localState)
                    viewModel.clearFeesState()
                }
                OrderFlowAction.CloseSimulator -> {
                    localState = OrderFlowReducer.closeSimulator(localState)
                    viewModel.clearFeesState()
                }
                is OrderFlowAction.SimulatorAmountChange -> {
                    localState = OrderFlowReducer.updateSimulatorAmount(localState, action.raw)
                    viewModel.clearFeesState()
                }
                OrderFlowAction.ConsultSimulator -> {
                    val amount = OrderPresentation.currencyInputAmount(localState.simulatorAmountDigits)
                    if (amount <= 0.0) {
                        localState = localState.copy(simulatorError = invalidSimulatorAmountMessage)
                    } else {
                        val previous = localState
                        val next = OrderFlowReducer.startSimulatorLoading(localState)
                        localState = next
                        if (!previous.feeRequestInFlight &&
                            next.feeRequestTarget == OrderFeeRequestTarget.Simulator
                        ) {
                            viewModel.calculateFees(amount, "credito")
                        }
                    }
                }
                is OrderFlowAction.SelectSimulatorInstallment -> {
                    localState = localState.copy(simulatorSelectedInstallment = action.installment)
                }
                is OrderFlowAction.CopySimulator -> {
                    onEffect(OrderFlowEffect.CopySimulatorText(action.text))
                }
                is OrderFlowAction.ShareSimulator -> {
                    onEffect(OrderFlowEffect.ShareSimulatorText(action.text))
                }
            }
        },
        onRefresh = {
            isRefreshing = true
            viewModel.loadOrders(forceRefresh = true)
        },
    )
}

private fun currentPaymentAmount(state: OrderFlowLocalState): Double {
    return OrderPresentation.paymentAmount(state.paymentDigits)
}

private fun formatCnpj(document: String): String {
    val numbers = document.filter(Char::isDigit)
    if (numbers.length != 14) return if (document.isBlank()) "-" else document

    return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
        "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
}
