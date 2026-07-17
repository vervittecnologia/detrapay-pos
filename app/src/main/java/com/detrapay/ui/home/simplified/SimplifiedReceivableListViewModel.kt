package com.detrapay.ui.home.simplified

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.OrderReceivable
import com.detrapay.data.model.OrderReceivableItemStatus
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimplifiedReceivableListViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _receivableListState = MutableLiveData<UIState<List<OrderReceivable>>>()
    val receivableListState: LiveData<UIState<List<OrderReceivable>>> = _receivableListState

    fun loadScreenContent(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            _receivableListState.postValue(UIState.Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = orderRepository.getReceivables(forceRefresh)) {
                is Result.Success -> {
                    _receivableListState.postValue(
                        UIState.Success(
                            result.data.filter { it.receivable.status == OrderReceivableItemStatus.PENDING }
                        )
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
}
