package com.detrapay.ui.home.payment_history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.detrapay.data.Result
import com.detrapay.data.model.local.Payment
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentHistoryViewModel @Inject constructor(private val paymentRepository: PaymentRepository) :
    ViewModel() {

    private val _paymentListState = MutableLiveData<UIState<List<Payment>>>()
    val paymentListState: LiveData<UIState<List<Payment>>> = _paymentListState

    fun loadScreenContent() {
        _paymentListState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            val result = paymentRepository.loadPaymentHistory()
            if (result is Result.Success) {
                _paymentListState.postValue(UIState.Success(result.data))
            } else {
                val error = result as Result.Error
                _paymentListState.postValue(UIState.Error(
                    message = "Ops! Algo deu errado, tente novamente.",
                    exception = error.exception))
            }
        }
    }
}