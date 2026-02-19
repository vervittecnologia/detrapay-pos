package com.detrapay.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.uol.pagseguro.plugpagservice.wrapper.IPlugPagWrapper
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPag
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagCustomPrinterLayout
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagEventListener
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagPaymentData
import br.com.uol.pagseguro.plugpagservice.wrapper.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpagservice.wrapper.exception.PlugPagException
import com.detrapay.data.Result
import com.detrapay.data.model.Order
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.model.PaymentData
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.Logger
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PaymentDialogViewModel @Inject constructor(
    private val plugPag: IPlugPagWrapper,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository
) : ViewModel(), PlugPagEventListener {

    private val _paymentState = MutableLiveData<UIState<PaymentData>>()
    val paymentState: LiveData<UIState<PaymentData>> = _paymentState

    fun init(){
        setupPrintLayout()
        setupPlugPagEventListener()
    }

    private fun setupPlugPagEventListener(){
        plugPag.setEventListener(this)
    }

    private fun setupPrintLayout() {
        //configura o popup de impressão da via do cliente
        plugPag.setPlugPagCustomPrinterLayout(
            PlugPagCustomPrinterLayout(
                "Imprimir via do cliente?",
                "#000000",
                "#FFFFFF",
                "#A0A0A0",
                "#0E5FB2",
                "#000000",
                "#808080",
                "#FFFFFF",
                60, // tempo de espera máximo do popup de impressão
            )
        )
    }

    private val loadingMessages = listOf(
        "Configurando maquininha...",
        "Processando pagamento...",
        "Comunicando com a operadora...",
        "Validando transação...",
        "Confirmando com o servidor...",
        "Quase lá...",
        "Finalizando..."
    )

    fun payOrder(orderId: Int, receivable: OrderReceivableItem, serial: String) {
        _paymentState.postValue(UIState.Loading(loadingMessages[0]))
        viewModelScope.launch(Dispatchers.Default) {
            // Inicia um job para rotacionar as mensagens de loading
            val messageRotationJob = launch {
                var index = 0
                while (true) {
                    kotlinx.coroutines.delay(3000)
                    index = (index + 1) % loadingMessages.size
                    val currentState = _paymentState.value
                    if (currentState is UIState.Loading) {
                        _paymentState.postValue(UIState.Loading(loadingMessages[index]))
                    } else {
                        break
                    }
                }
            }

            try {
                // Primeiro faz a configuração do split (pre-pay)
                val prePayResult = orderRepository.updateSplitConfig(receivable.id, serial)
                if (prePayResult is Result.Error) {
                    messageRotationJob.cancel()
                    _paymentState.postValue(UIState.Error("Não foi possível configurar a maquininha: ${prePayResult.exception.message}"))
                    return@launch
                }

                if (plugPag.isAuthenticated()) {
                    val amountInCents = receivable.amountFinal * 100
                    val roundedAmountInCents = amountInCents.roundToInt()

                    val paymentType = getPaymentType(receivable.paymentMethod.name)
                    val installmentType = getInstallmentType(receivable.max_installments)
                    val paymentData = PlugPagPaymentData(
                        paymentType,
                        roundedAmountInCents,
                        installmentType,
                        receivable.max_installments,
                        null,
                        printReceipt = true,
                        partialPay = false,
                        isCarne = false,
                    )
                    val plugPagResult: PlugPagTransactionResult = plugPag.doPayment(paymentData)
                    val transactionLog = Gson().toJson(plugPagResult)
                    
                    if (plugPagResult.result == PlugPag.RET_OK) {
                        val transactionResult = PaymentData(
                            transactionId = plugPagResult.transactionId!!,
                            transactionCode = plugPagResult.transactionCode!!,
                            date = plugPagResult.date!!,
                            time = plugPagResult.time!!,
                            cardBrand = plugPagResult.cardBrand,
                            cardLast4 = plugPagResult.holder,
                            cardHolder = plugPagResult.holderName,
                            pixTxIdCode = plugPagResult.pixTxIdCode,
                            transactionLog = transactionLog
                        )

                        paymentRepository.saveTransaction(
                            orderId = orderId,
                            amount = receivable.amountFinal,
                            installments = receivable.max_installments,
                            paymentType = receivable.paymentMethod.name,
                            transactionId = plugPagResult.transactionId,
                            transactionCode = plugPagResult.transactionCode,
                            date = plugPagResult.date,
                            result = plugPagResult.result,
                            cardBrand = plugPagResult.cardBrand,
                            cardLast4 = plugPagResult.holder,
                            cardHolder = plugPagResult.holderName,
                            pixTxIdCode = plugPagResult.pixTxIdCode,
                            message = plugPagResult.message,
                            errorCode = plugPagResult.errorCode
                        )

                        val apiResult = orderRepository.payOrder(orderId, receivable, transactionResult)
                        
                        messageRotationJob.cancel()
                        if (apiResult is Result.Success) {
                            _paymentState.postValue(UIState.Success(transactionResult))
                        } else {
                            val error = apiResult as Result.Error
                            _paymentState.postValue(UIState.Error(error.exception.message ?: "Erro ao confirmar pagamento no servidor"))
                        }
                    } else {
                        messageRotationJob.cancel()
                        val errorCode = plugPagResult.errorCode.toString()
                        val errorMessage = plugPagResult.message.toString()

                        paymentRepository.saveTransaction(
                            orderId = orderId,
                            amount = receivable.amountFinal,
                            installments = receivable.max_installments,
                            paymentType = receivable.paymentMethod.name,
                            transactionId = plugPagResult.transactionId,
                            transactionCode = plugPagResult.transactionCode,
                            date = plugPagResult.date,
                            result = plugPagResult.result,
                            cardBrand = plugPagResult.cardBrand,
                            cardLast4 = plugPagResult.holder,
                            cardHolder = plugPagResult.holderName,
                            pixTxIdCode = plugPagResult.pixTxIdCode,
                            message = plugPagResult.message,
                            errorCode = plugPagResult.errorCode
                        )

                        _paymentState.postValue(UIState.Error("$errorCode - $errorMessage"))
                    }
                } else {
                    messageRotationJob.cancel()
                    _paymentState.postValue(UIState.Error("Nenhum usuario autenticado, contate o suporte."))
                }
            } catch (e: PlugPagException) {
                messageRotationJob.cancel()
                _paymentState.postValue(UIState.Error("${e.errorCode} - ${e.message}"))
            } catch (e: Exception) {
                messageRotationJob.cancel()
                _paymentState.postValue(UIState.Error(e.message ?: "Erro inesperado"))
            }
        }
    }

    private fun getInstallmentType(installments: Int): Int {
        return if (installments > 1) {
            PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR
        } else {
            PlugPag.INSTALLMENT_TYPE_A_VISTA
        }
    }

    private fun getPaymentType(name: String): Int {
        val isCreditCard = name.contains("Crédito", true) || name.contains("Cartão de crédito", true) || name.contains("VISA", true) || name.contains("Mastercard", true)
        return if (isCreditCard) {
            PlugPag.TYPE_CREDITO
        } else if (name.contains("Pix", true)) {
            PlugPag.TYPE_PIX
        } else {
            PlugPag.TYPE_DEBITO
        }
    }

    fun abortPayment() {
        viewModelScope.launch(Dispatchers.Default) {
            plugPag.abort()
            plugPag.disposeSubscriber()
        }
    }

    override fun onEvent(data: PlugPagEventData) {
        if (data.eventCode == PlugPagEventData.EVENT_CODE_DIGIT_PASSWORD) {
            _paymentState.postValue(UIState.Loading(data.customMessage))
        } else {
            _paymentState.postValue(UIState.Loading(data.customMessage))
        }
    }
}