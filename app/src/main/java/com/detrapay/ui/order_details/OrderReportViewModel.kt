package com.detrapay.ui.order_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPrinterData
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.ui.state.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderReportViewModel @Inject constructor(
    private val plugPag: IPlugPagWrapper
) : ViewModel() {

    private val _printState = MutableLiveData<UIState<String>>(UIState.Idle())
    val printState: LiveData<UIState<String>> = _printState

    fun printReport(path: String) {
        _printState.postValue(UIState.Loading())
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val result = plugPag.printFromFile(
                    PlugPagPrinterData(
                        path,
                        100,
                        PlugPag.MIN_PRINTER_STEPS
                    )
                )

                if (result.result == PlugPag.RET_OK) {
                    _printState.postValue(UIState.Success("Impressao realizada com sucesso!"))
                } else {
                    _printState.postValue(UIState.Error(result.errorCode.toString() + result.message))
                }
            } catch (e: PlugPagException) {
                _printState.postValue(UIState.Error(e.message ?: "Falha ao iniciar impressao"))
            }
        }
    }
}
