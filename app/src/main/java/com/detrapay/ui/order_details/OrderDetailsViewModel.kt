package com.detrapay.ui.order_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentMethod
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.data.model.Salesman
import com.detrapay.data.model.canBeDeleted
import com.detrapay.data.model.remote.CalculateFeesResponse
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.RegistrationRepository
import com.detrapay.data.repositories.SalesmanRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val registrationRepository: RegistrationRepository,
    private val salesmanRepository: SalesmanRepository
) : ViewModel() {

    private var orderId: Int = 0
    private var paymentMethods: List<PaymentMethod> = emptyList()
    private val _orderState = MutableLiveData<UIState<Order>>()
    val orderState: LiveData<UIState<Order>> = _orderState

    private val _salesmenState = MutableLiveData<UIState<List<Salesman>>>()
    val salesmenState: LiveData<UIState<List<Salesman>>> = _salesmenState

    private val _paymentMethodsState = MutableLiveData<UIState<List<PaymentMethod>>>()
    val paymentMethodsState: LiveData<UIState<List<PaymentMethod>>> = _paymentMethodsState

    private val _calculateFeesState = MutableLiveData<UIState<CalculateFeesResponse>>(UIState.Idle())
    val calculateFeesState: LiveData<UIState<CalculateFeesResponse>> = _calculateFeesState

    fun loadScreenContent(orderId: Int) {
        this.orderId = orderId
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = orderRepository.getOrder(orderId, forceRefresh = true)
            if (result is Result.Success) {
                _orderState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderState.postValue(
                    UIState.Error(
                        message = "Ops! Algo deu errado, tente novamente.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun loadSalesmen() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = salesmanRepository.getSalesmen()) {
                is Result.Success -> _salesmenState.postValue(UIState.Success(result.data))
                is Result.Error -> _salesmenState.postValue(
                    UIState.Error(result.exception.message ?: "Nenhum vendedor encontrado")
                )
            }
        }
    }

    fun updateOrderSalesman(salesman: Salesman) {
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            if (salesman.id == null) {
                _orderState.postValue(
                    UIState.Error(
                        message = "Ops! O vendedor selecionado não possui um ID."
                    )
                )
                return@launch
            }
            val result = orderRepository.updateOrderSalesman(orderId, salesman.id)
            if (result is Result.Success) {
                _orderState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderState.postValue(
                    UIState.Error(
                        message = "Ops! Algo deu errado, tente novamente.",
                        exception = error.exception
                    )
                )
            }
        }
    }

    fun loadPaymentMethods() {
        _paymentMethodsState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.loadPaymentMethods()) {
                is Result.Success -> {
                    paymentMethods = result.data
                    _paymentMethodsState.postValue(UIState.Success(result.data))
                }
                is Result.Error -> _paymentMethodsState.postValue(
                    UIState.Error(
                        message = result.exception.message ?: "Nao foi possivel carregar os meios de pagamento.",
                        exception = result.exception
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

    fun getPaymentMethodsByType(type: String): List<PaymentMethod> {
        val normalizedType = normalizePaymentType(type)
        return paymentMethods.filter { method ->
            normalizePaymentType(method.paymentType) == normalizedType
        }
    }

    fun resolvePaymentMethod(type: String, installments: Int): PaymentMethod? {
        val methods = getPaymentMethodsByType(type)
        return methods.firstOrNull { it.installments == installments }
            ?: methods.firstOrNull { it.installments == 1 }
            ?: methods.firstOrNull()
    }

    fun calculateFees(value: Double, paymentType: String) {
        if (value <= 0.0) {
            _calculateFeesState.postValue(UIState.Error("Informe um valor maior que zero."))
            return
        }

        _calculateFeesState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = registrationRepository.calculateFees(value, paymentType)) {
                is Result.Success -> _calculateFeesState.postValue(UIState.Success(result.data))
                is Result.Error -> _calculateFeesState.postValue(
                    UIState.Error(
                        message = result.exception.message ?: "Nao foi possivel calcular as parcelas.",
                        exception = result.exception
                    )
                )
            }
        }
    }

    fun clearFeesState() {
        _calculateFeesState.postValue(UIState.Idle())
    }

    fun addPendingReceivable(paymentMethod: PaymentMethod, amountOriginal: Double) {
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = orderRepository.addPendingReceivable(
                orderId = orderId,
                paymentMethod = paymentMethod,
                amountOriginal = amountOriginal
            )
            if (result is Result.Success) {
                _orderState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderState.postValue(
                    UIState.Error(
                        message = error.exception.message
                            ?: "Algo deu errado ao adicionar o pagamento pendente, tente novamente.",
                        exception = error.exception,
                    )
                )
            }
        }
    }

    fun payOrder(receivable: OrderReceivableItem, paymentData: PaymentData) {
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = orderRepository.payOrder(orderId, receivable, paymentData)
                if (result is Result.Success) {
                    _orderState.postValue(UIState.Success(result.data))
                } else {
                    val error = result as Result.Error
                    _orderState.postValue(
                        UIState.Error(
                            message = error.exception.message ?: "Algo deu errado na atualização do pagamento do pedido, tente novamente.",
                            exception = error.exception,
                            retryData = RetryDataModel(receivable, paymentData)
                        )
                    )
                }
            } catch (e:Exception) {
                _orderState.postValue(
                    UIState.Error(
                        message = e.message ?:  "Algo deu errado na atualização do pagamento do pedido, tente novamente.",
                        exception = e,
                        retryData = RetryDataModel(receivable, paymentData)
                    )
                )
            }
        }
    }

    fun refundItem(receivable: OrderReceivableItem, refundPaymentData: RefundPaymentData) {
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.Default) {
            val result = orderRepository.refundOrderPayment(
                orderId,
                receivable,
                refundPaymentData
            )
            if (result is Result.Success) {
                _orderState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderState.postValue(
                    UIState.Error(
                        message = "Algo deu errado no estorno do pedido, tente novamente.",
                        exception = error.exception,
                    )
                )
            }
        }
    }

    fun cancelPendingItem(receivable: OrderReceivableItem) {
        if (!receivable.canBeDeleted()) {
            _orderState.postValue(
                UIState.Error(message = "Este pagamento nao pode ser excluido no status atual.")
            )
            return
        }

        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = orderRepository.cancelPendingReceivable(orderId, receivable)
            if (result is Result.Success) {
                _orderState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderState.postValue(
                    UIState.Error(
                        message = error.exception.message
                            ?: "Algo deu errado ao excluir o pagamento, tente novamente.",
                        exception = error.exception,
                    )
                )
            }
        }
    }

    private fun normalizePaymentType(rawType: String?): String {
        return when (rawType.orEmpty().trim().lowercase()) {
            "credito", "credit", "cartao_credito", "cartao de credito" -> "credito"
            "debito", "debit", "cartao_debito", "cartao de debito" -> "debito"
            "pix" -> "pix"
            "dinheiro", "cash" -> "dinheiro"
            "store_credit", "credito_loja", "credito loja", "storecredit" -> "store_credit"
            else -> rawType.orEmpty().trim().lowercase()
        }
    }
}
