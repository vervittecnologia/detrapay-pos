package com.detrapay.ui.home.direct_checkout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.detrapay.data.model.Order
import com.detrapay.data.model.PaymentData
import com.detrapay.ui.home.HomeViewModel
import com.detrapay.ui.home.simplified.DirectCheckoutOrderPresentation
import com.detrapay.ui.home.simplified.SimplifiedReceivableListViewModel
import com.detrapay.ui.order_details.OrderDetailsPaymentMethodPickerBottomSheet
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectCheckoutRoute(
    homeViewModel: HomeViewModel,
    viewModel: SimplifiedReceivableListViewModel,
    defaultCompanyName: String,
    defaultCompanyDocument: String,
    directCheckoutErrorMessage: String,
    installmentErrorMessage: String,
    addPaymentLoadErrorMessage: String,
    paymentSuccessMessage: String,
    invalidSimulatorAmountMessage: String,
    onEffect: (DirectCheckoutEffect) -> Unit,
) {
    var localState by remember { mutableStateOf(DirectCheckoutLocalState()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var availableTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var companyName by remember { mutableStateOf(defaultCompanyName) }
    var companyDocument by remember { mutableStateOf(defaultCompanyDocument) }

    val homeState by homeViewModel.homeState.observeAsState()
    val orderState by viewModel.directOrderListState.observeAsState()
    val paymentMethodsState by viewModel.paymentMethodsState.observeAsState()
    val feesState by viewModel.calculateFeesState.observeAsState()
    val pendingPaymentState by viewModel.pendingPaymentState.observeAsState()
    val manualPaymentState by viewModel.manualPaymentState.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDirectCheckoutOrders(forceRefresh = false)
    }

    LaunchedEffect(homeState) {
        val data = (homeState as? UIState.Success)?.data ?: return@LaunchedEffect
        companyName = data.companyName.ifBlank { defaultCompanyName }
        companyDocument = formatCnpj(data.companyDocument).ifBlank { defaultCompanyDocument }
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is UIState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            is UIState.Success -> {
                isLoading = false
                errorMessage = null
                orders = state.data.orEmpty()
            }
            is UIState.Error -> {
                isLoading = false
                orders = emptyList()
                errorMessage = state.message ?: directCheckoutErrorMessage
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
            }
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(paymentMethodsState) {
        when (val state = paymentMethodsState) {
            is UIState.Success -> availableTypes = viewModel.availablePaymentTypes()
            is UIState.Error -> {
                onEffect(
                    DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage),
                )
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
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
                    DirectCheckoutFeeRequestTarget.CheckoutCredit -> {
                        localState.copy(feesLoading = true, feesError = null)
                    }
                    DirectCheckoutFeeRequestTarget.Simulator -> {
                        localState.copy(simulatorLoading = true, simulatorError = null)
                    }
                    null -> localState
                }
            }
            is UIState.Success -> {
                val installments = state.data?.data.orEmpty().firstOrNull()?.installments.orEmpty()
                localState = when (localState.feeRequestTarget) {
                    DirectCheckoutFeeRequestTarget.CheckoutCredit -> {
                        DirectCheckoutReducer.feesLoaded(localState, installments, installmentErrorMessage)
                    }
                    DirectCheckoutFeeRequestTarget.Simulator -> {
                        DirectCheckoutReducer.simulatorLoaded(localState, installments, installmentErrorMessage)
                    }
                    null -> localState
                }
            }
            is UIState.Error -> {
                val message = state.message ?: installmentErrorMessage
                localState = when (localState.feeRequestTarget) {
                    DirectCheckoutFeeRequestTarget.CheckoutCredit -> {
                        DirectCheckoutReducer.feesFailed(localState, message)
                    }
                    DirectCheckoutFeeRequestTarget.Simulator -> {
                        DirectCheckoutReducer.simulatorFailed(localState, message)
                    }
                    null -> localState
                }
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
            }
            is UIState.Idle,
            null -> localState = localState.copy(feesLoading = false, simulatorLoading = false)
        }
    }

    LaunchedEffect(pendingPaymentState) {
        when (val state = pendingPaymentState) {
            is UIState.Success -> {
                val pendingPayment = viewModel.consumeDirectCheckoutPendingPayment() ?: return@LaunchedEffect
                if (PaymentTypeRules.requiresTerminalApproval(pendingPayment.paymentType)) {
                    onEffect(
                        DirectCheckoutEffect.OpenPaymentDialog(pendingPayment) { paymentData ->
                            localState = localState.copy(
                                step = if (paymentData != null) {
                                    DirectCheckoutStep.Orders
                                } else {
                                    DirectCheckoutStep.Method
                                },
                            )
                            viewModel.loadDirectCheckoutOrders(forceRefresh = true)
                        },
                    )
                } else {
                    onEffect(
                        DirectCheckoutEffect.ConfirmManualPayment(
                            pendingPayment = pendingPayment,
                            paymentData = buildManualPaymentData(pendingPayment.amount),
                        ),
                    )
                }
            }
            is UIState.Error -> {
                localState = localState.copy(step = DirectCheckoutStep.Method)
                onEffect(DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage))
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
                viewModel.clearDirectCheckoutPaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    LaunchedEffect(manualPaymentState) {
        when (val state = manualPaymentState) {
            is UIState.Success -> {
                onEffect(DirectCheckoutEffect.ShowToast(paymentSuccessMessage, long = false))
                localState = localState.copy(step = DirectCheckoutStep.Orders)
                viewModel.clearDirectCheckoutPaymentState()
                viewModel.loadDirectCheckoutOrders(forceRefresh = true)
            }
            is UIState.Error -> {
                localState = localState.copy(step = DirectCheckoutStep.Method)
                onEffect(DirectCheckoutEffect.ShowToast(state.message ?: addPaymentLoadErrorMessage))
                state.exception?.let { onEffect(DirectCheckoutEffect.ShowSessionExpired(it)) }
                viewModel.clearDirectCheckoutPaymentState()
            }
            is UIState.Loading,
            is UIState.Idle,
            null -> Unit
        }
    }

    DirectCheckoutScreen(
        state = DirectCheckoutUiState(
            companyName = companyName,
            companyDocument = companyDocument,
            orders = orders,
            isLoading = isLoading,
            errorMessage = errorMessage,
            availablePaymentTypes = availableTypes,
            local = localState,
        ),
        onAction = { action ->
            when (action) {
                DirectCheckoutAction.Logout -> onEffect(DirectCheckoutEffect.ShowLogoutConfirmation)
                DirectCheckoutAction.Reload -> viewModel.loadDirectCheckoutOrders(forceRefresh = true)
                DirectCheckoutAction.NewOrder -> onEffect(DirectCheckoutEffect.NavigateToRegistration)
                is DirectCheckoutAction.OrderPay -> {
                    localState = DirectCheckoutReducer.startPayment(localState, action.order)
                    viewModel.clearFeesState()
                    viewModel.loadPaymentMethods()
                }
                is DirectCheckoutAction.OrderDetail -> {
                    localState = DirectCheckoutReducer.showDetail(localState, action.order)
                }
                DirectCheckoutAction.Back -> {
                    localState = DirectCheckoutReducer.back(localState)
                }
                is DirectCheckoutAction.Key -> {
                    localState = DirectCheckoutReducer.applyPaymentKey(localState, action.value)
                }
                DirectCheckoutAction.OpenMethods -> {
                    localState = DirectCheckoutReducer.openMethods(localState)
                }
                is DirectCheckoutAction.SelectPaymentType -> {
                    val previous = localState
                    val next = DirectCheckoutReducer.selectPaymentType(localState, action.paymentType)
                    localState = next
                    when (PaymentTypeRules.normalize(action.paymentType)) {
                        OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT -> {
                            viewModel.calculateFees(currentPaymentAmount(next), action.paymentType)
                        }
                        OrderDetailsPaymentMethodPickerBottomSheet.TYPE_DEBIT -> {
                            viewModel.clearFeesState()
                        }
                        else -> {
                            viewModel.clearFeesState()
                            previous.selectedOrder?.let { order ->
                                viewModel.addDirectCheckoutPendingPayment(
                                    order = order,
                                    paymentType = action.paymentType,
                                    amount = currentPaymentAmount(next),
                                    installments = 1,
                                )
                            }
                        }
                    }
                }
                is DirectCheckoutAction.SelectInstallment -> {
                    localState = DirectCheckoutReducer.selectInstallment(localState, action.installment)
                }
                DirectCheckoutAction.ContinueCredit -> {
                    localState.selectedOrder?.let { order ->
                        localState = localState.copy(step = DirectCheckoutStep.Waiting)
                        viewModel.addDirectCheckoutPendingPayment(
                            order = order,
                            paymentType = localState.selectedPaymentType,
                            amount = currentPaymentAmount(localState),
                            installments = localState.selectedInstallment,
                        )
                    }
                }
                DirectCheckoutAction.ContinueDebit -> {
                    localState.selectedOrder?.let { order ->
                        localState = localState.copy(step = DirectCheckoutStep.Waiting)
                        viewModel.addDirectCheckoutPendingPayment(
                            order = order,
                            paymentType = localState.selectedPaymentType,
                            amount = currentPaymentAmount(localState),
                            installments = 1,
                        )
                    }
                }
                DirectCheckoutAction.OpenSimulator -> {
                    localState = DirectCheckoutReducer.openSimulator(localState)
                    viewModel.clearFeesState()
                }
                DirectCheckoutAction.CloseSimulator -> {
                    localState = DirectCheckoutReducer.closeSimulator(localState)
                    viewModel.clearFeesState()
                }
                is DirectCheckoutAction.SimulatorAmountChange -> {
                    localState = DirectCheckoutReducer.updateSimulatorAmount(localState, action.raw)
                    viewModel.clearFeesState()
                }
                DirectCheckoutAction.ConsultSimulator -> {
                    val amount = DirectCheckoutOrderPresentation.currencyInputAmount(localState.simulatorAmountDigits)
                    if (amount <= 0.0) {
                        localState = localState.copy(simulatorError = invalidSimulatorAmountMessage)
                    } else {
                        localState = DirectCheckoutReducer.startSimulatorLoading(localState)
                        viewModel.calculateFees(amount, OrderDetailsPaymentMethodPickerBottomSheet.TYPE_CREDIT)
                    }
                }
                is DirectCheckoutAction.SelectSimulatorInstallment -> {
                    localState = localState.copy(simulatorSelectedInstallment = action.installment)
                }
                is DirectCheckoutAction.CopySimulator -> {
                    onEffect(DirectCheckoutEffect.CopySimulatorText(action.text))
                }
                is DirectCheckoutAction.ShareSimulator -> {
                    onEffect(DirectCheckoutEffect.ShareSimulatorText(action.text))
                }
            }
        },
    )
}

private fun currentPaymentAmount(state: DirectCheckoutLocalState): Double {
    val order = state.selectedOrder ?: return 0.0
    val pendingAmount = DirectCheckoutOrderPresentation.summary(order).missingAmount
    return DirectCheckoutOrderPresentation.paymentAmount(state.paymentDigits, pendingAmount)
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

private fun formatCnpj(document: String): String {
    val numbers = document.filter(Char::isDigit)
    if (numbers.length != 14) return if (document.isBlank()) "-" else document

    return "${numbers.substring(0, 2)}.${numbers.substring(2, 5)}.${numbers.substring(5, 8)}/" +
        "${numbers.substring(8, 12)}-${numbers.substring(12, 14)}"
}
