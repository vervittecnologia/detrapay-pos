package com.detrapay.ui.home.order_list

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
class OrderListViewModel @Inject constructor(private val orderRepository: OrderRepository) :
    ViewModel() {

    private val _orderListState = MutableLiveData<UIState<List<Order>>>()
    val orderListState: LiveData<UIState<List<Order>>> = _orderListState

    fun loadScreenContent(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _orderListState.postValue(UIState.Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = orderRepository.getOrders(forceRefresh)
            if (result is Result.Success) {
                _orderListState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _orderListState.postValue(UIState.Error(
                    message = "Ops! Algo deu errado, tente novamente.",
                    exception = error.exception))
            }
        }
    }

}