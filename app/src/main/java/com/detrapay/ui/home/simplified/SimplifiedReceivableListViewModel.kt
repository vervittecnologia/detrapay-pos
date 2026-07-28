package com.detrapay.ui.home.simplified

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import com.detrapay.ui.util.PaymentTypeRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DirectCheckoutPendingPayment(
    val order: Order,
    val receivable: OrderReceivableItem,
    val paymentType: String,
    val amount: Double,
)

@HiltViewModel
class SimplifiedReceivableListViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val registrationRepository: RegistrationRepository,
    private val salesmanRepository: SalesmanRepository,
) : ViewModel() {

    private var paymentMethods: List<PaymentMethod> = emptyList()
    private val _receivableListState = MutableLiveData<UIState<List<OrderReceivable>>>()
    val receivableListState: LiveData<UIState<List<OrderReceivable>>> = _receivableListState
    private val _directOrderListState = MutableLiveData<UIState<List<Order>>>()
    val directOrderListState: LiveData<UIState<List<Order>>> = _directOrderListState
    private val _paymentMethodsState = MutableLiveData<UIState<List<PaymentMethod>>>(UIState.Idle())
    val paymentMethodsState: LiveData<UIState<List<PaymentMethod>>> = _paymentMethodsState
    private val _calculateFeesState = MutableLiveData<UIState<CalculateFeesResponse>>(UIState.Idle())
    val calculateFeesState: LiveData<UIState<CalculateFeesResponse>> = _calculateFeesState
    private val _pendingPaymentState = MutableLiveData<UIState<DirectCheckoutPendingPayment>>(UIState.Idle())
    val pendingPaymentState: LiveData<UIState<DirectCheckoutPendingPayment>> = _pendingPaymentState
    private val _manualPaymentState = MutableLiveData<UIState<Order>>(UIState.Idle())
    val manualPaymentState: LiveData<UIState<Order>> = _manualPaymentState

    fun loadScreenContent(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _receivableListState.postValue(UIState.Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.getReceivables(forceRefresh)) {
                is Result.Success -> {
                    _receivableListState.postValue(
                        UIState.Success(result.data.sortedWith(
                            compareByDescending<OrderReceivable> { it.receivable.status == OrderReceivableItemStatus.PENDING }
                                .thenByDescending { it.order.id }
                        ))
                    )
                }
                is Result.Error -> {
                    _receivableListState.postValue(
                        UIState.Error(
                            message = "Nao foi possivel carregar os pagamentos.",
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }

    fun loadDirectCheckoutOrders(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _directOrderListState.postValue(UIState.Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.getOrders(forceRefresh)) {
                is Result.Success -> {
                    val pending = DirectCheckoutOrderPresentation.pendingOrders(result.data)
                    _directOrderListState.postValue(
                        UIState.Success(pending.sortedByDescending { it.id })
                    )
                }
                is Result.Error -> {
                    _directOrderListState.postValue(
                        UIState.Error(
                            message = "Nao foi possivel carregar os pedidos.",
                            exception = result.exception
                        )
                    )
                }
            }
        }
    }

    fun loadPaymentMethods(forceRefresh: Boolean = false) {
        _paymentMethodsState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.loadPaymentMethods(forceRefresh)) {
                is Result.Success -> {
                    paymentMethods = result.data
                    _paymentMethodsState.postValue(UIState.Success(result.data))
                }
                is Result.Error -> _paymentMethodsState.postValue(
                    UIState.Error(
                        message = result.exception.message ?: "Nao foi possivel carregar os meios de pagamento.",
                        exception = result.exception,
                    )
                )
            }
        }
    }

    fun availablePaymentTypes(): List<String> {
        val desiredOrder = listOf("credito", "debito", "pix", "dinheiro", "store_credit")
        val available = paymentMethods
            .mapNotNull { it.paymentType }
            .map(::normalizePaymentType)
            .toSet()

        return desiredOrder.filter(available::contains)
    }

    fun calculateFees(value: Double, paymentType: String) {
        if (value <= 0.0) {
            _calculateFeesState.postValue(UIState.Error("Informe um valor maior que zero."))
            return
        }

        if (PaymentTypeRules.isDirectNoFeePaymentType(paymentType)) {
            _calculateFeesState.postValue(UIState.Success(PaymentTypeRules.zeroFeeQuote(value, paymentType)))
            return
        }

        _calculateFeesState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.calculateFees(value, paymentType)) {
                is Result.Success -> _calculateFeesState.postValue(UIState.Success(result.data))
                is Result.Error -> _calculateFeesState.postValue(
                    UIState.Error(
                        message = result.exception.message ?: "Nao foi possivel calcular as parcelas.",
                        exception = result.exception,
                    )
                )
            }
        }
    }

    fun clearFeesState() {
        _calculateFeesState.postValue(UIState.Idle())
    }

    fun addDirectCheckoutPendingPayment(
        order: Order,
        paymentType: String,
        amount: Double,
        installments: Int,
    ) {
        val paymentMethod = resolvePaymentMethod(paymentType, installments)
        if (paymentMethod == null) {
            _pendingPaymentState.postValue(UIState.Error("Metodo de pagamento indisponivel para a condicao selecionada."))
            return
        }

        _pendingPaymentState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.addPendingReceivable(order.id, paymentMethod, amount)) {
                is Result.Success -> {
                    val receivable = DirectCheckoutOrderPresentation.createdReceivable(
                        oldOrder = order,
                        newOrder = result.data,
                        paymentMethod = paymentMethod,
                        amount = amount,
                    )
                    if (receivable == null) {
                        _pendingPaymentState.postValue(
                            UIState.Error(
                                "Nao foi possivel identificar o pagamento criado. O backend precisa retornar o recebivel criado com identificador, metodo, valor e status.",
                            )
                        )
                    } else {
                        _pendingPaymentState.postValue(
                            UIState.Success(
                                DirectCheckoutPendingPayment(
                                    order = result.data,
                                    receivable = receivable,
                                    paymentType = normalizePaymentType(paymentType),
                                    amount = amount,
                                )
                            )
                        )
                    }
                }
                is Result.Error -> _pendingPaymentState.postValue(
                    UIState.Error(
                        message = result.exception.message
                            ?: "Algo deu errado ao adicionar o pagamento pendente, tente novamente.",
                        exception = result.exception,
                    )
                )
            }
        }
    }

    fun confirmDirectCheckoutManualPayment(pendingPayment: DirectCheckoutPendingPayment, paymentData: PaymentData) {
        _manualPaymentState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (
                val result = orderRepository.payOrder(
                    pendingPayment.order.id,
                    pendingPayment.receivable,
                    paymentData,
                )
            ) {
                is Result.Success -> _manualPaymentState.postValue(UIState.Success(result.data))
                is Result.Error -> _manualPaymentState.postValue(
                    UIState.Error(
                        message = result.exception.message
                            ?: "Algo deu errado na confirmacao do pagamento, tente novamente.",
                        exception = result.exception,
                    )
                )
            }
        }
    }

    fun clearDirectCheckoutPaymentState() {
        _pendingPaymentState.value = UIState.Idle()
        _manualPaymentState.value = UIState.Idle()
    }

    fun consumeDirectCheckoutPendingPayment(): DirectCheckoutPendingPayment? {
        val payment = (_pendingPaymentState.value as? UIState.Success)?.data
        _pendingPaymentState.value = UIState.Idle()
        return payment
    }

    fun prefetchRegistrationData() {
        viewModelScope.launch(Dispatchers.IO) {
            registrationRepository.loadVehicleTypes()
            salesmanRepository.getSalesmen()
        }
    }

    private fun resolvePaymentMethod(type: String, installments: Int): PaymentMethod? {
        return DirectCheckoutOrderPresentation.exactPaymentMethod(paymentMethods, type, installments)
    }

    private fun normalizePaymentType(rawType: String?): String {
        return PaymentTypeRules.normalize(rawType)
    }
}
