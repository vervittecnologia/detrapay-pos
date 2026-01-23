package com.detrapay.ui.order_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {

    private var orderId: Int = 0
    private val _orderState = MutableLiveData<UIState<Order>>()
    val orderState: LiveData<UIState<Order>> = _orderState

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
}