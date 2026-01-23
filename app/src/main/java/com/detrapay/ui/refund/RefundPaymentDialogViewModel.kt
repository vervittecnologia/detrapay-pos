package com.detrapay.ui.refund

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagVoidData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.RefundPaymentData
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RefundPaymentDialogViewModel @Inject constructor(
    private val plugPag: IPlugPagWrapper
) : ViewModel(), PlugPagEventListener {

    private val _paymentState = MutableLiveData<UIState<RefundPaymentData>>()
    val paymentState: LiveData<UIState<RefundPaymentData>> = _paymentState

    fun init(){
        setupPlugPagEventListener()
    }

    private fun setupPlugPagEventListener(){
        plugPag.setEventListener(this)
    }

    fun refundPayment(receivable: OrderReceivableItem) {
        _paymentState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (plugPag.isAuthenticated()) {
                    val voidPaymentData = PlugPagVoidData(
                        transactionId = receivable.authorizationId!!,
                        transactionCode = receivable.authorizationCode!!,
                        printReceipt = true,
                        voidType = PlugPag.VOID_PAYMENT
                    )

                    val plugPagResult: PlugPagTransactionResult = plugPag.voidPayment(voidPaymentData)

                    if (plugPagResult.result == PlugPag.RET_OK) {
                        val transactionResult = RefundPaymentData(
                            date = plugPagResult.date!!,
                            time = plugPagResult.time!!,
                        )
                        _paymentState.postValue(UIState.Success(transactionResult))
                    } else {
                        val errorCode = plugPagResult.errorCode.toString()
                        val errorMessage = plugPagResult.message.toString()
                        _paymentState.postValue(UIState.Error("$errorCode - $errorMessage"))
                    }
                } else {
                    _paymentState.postValue(UIState.Error("Nenhum usuario autenticado, contate o suporte."))
                }
            } catch (e: PlugPagException) {
                _paymentState.postValue(UIState.Error("${e.errorCode} - ${e.message}"))
            }
        }
    }

    fun abortPayment() {
        viewModelScope.launch(Dispatchers.Default) {
            plugPag.abort()
            plugPag.disposeSubscriber()
        }
    }

    override fun onEvent(data: PlugPagEventData) {
        _paymentState.postValue(UIState.Loading(data.customMessage))
    }
}