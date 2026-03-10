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
    private val _orderState = MutableLiveData<UIState<Order>>()
    val orderState: LiveData<UIState<Order>> = _orderState

    private val _salesmenState = MutableLiveData<UIState<List<Salesman>>>()
    val salesmenState: LiveData<UIState<List<Salesman>>> = _salesmenState

    private val _paymentMethodsState = MutableLiveData<UIState<List<PaymentMethod>>>()
    val paymentMethodsState: LiveData<UIState<List<PaymentMethod>>> = _paymentMethodsState

    fun loadScreenContent(orderId: Int) {
        this.orderId = orderId
        _orderState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = orderRepository.getOrder(orderId)
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
                is Result.Success -> _paymentMethodsState.postValue(UIState.Success(result.data))
                is Result.Error -> _paymentMethodsState.postValue(
                    UIState.Error(
                        message = result.exception.message ?: "Nao foi possivel carregar os meios de pagamento.",
                        exception = result.exception
                    )
                )
            }
        }
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
                            ?: "Algo deu errado ao excluir o pagamento pendente, tente novamente.",
                        exception = error.exception,
                    )
                )
            }
        }
    }
}
