package com.detrapay.ui.order_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(private val orderRepository: OrderRepository) :
    ViewModel() {

    private val _orderState = MutableLiveData<UIState<Order>>()
    val orderState: LiveData<UIState<Order>> = _orderState

    fun loadScreenContent(orderId: Int) {
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
}