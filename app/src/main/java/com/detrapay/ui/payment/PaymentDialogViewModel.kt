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
import com.detrapay.data.model.PaymentData
import com.detrapay.data.model.OrderReceivableItem
import com.detrapay.data.repositories.OrderRepository
import com.detrapay.data.repositories.PaymentRepository
import com.detrapay.ui.home.orders.OrderPaymentRequest
import com.detrapay.ui.state.UIState
import com.detrapay.ui.util.PaymentTypeRules
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
    private val orderRepository: OrderRepository,
) : ViewModel(), PlugPagEventListener {

    private enum class PaymentStep(val message: String) {
        PREPARING("Aguarde, preparando a maquininha."),
        WAITING("Siga as instrucoes na maquininha."),
        PROCESSING("Processando pagamento..."),
        RECORDING("Pagamento aprovado. Registrando no pedido..."),
    }

    private data class PendingCompletion(
        val idempotencyKey: String,
        val attemptId: String,
        val paymentData: PaymentData,
    )

    private val _paymentState = MutableLiveData<UIState<PaymentData>>()
    val paymentState: LiveData<UIState<PaymentData>> = _paymentState

    @Volatile
    private var terminalPaymentActive = false
    private var pendingCompletion: PendingCompletion? = null

    fun init() {
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
                60,
            ),
        )
        plugPag.setEventListener(this)
    }

    fun payOrder(request: OrderPaymentRequest, serial: String) {
        if (!request.paymentMethod.isOnlinePayment) {
            _paymentState.postValue(UIState.Error("Este pagamento deve ser apenas registrado."))
            return
        }

        val confirmedAmountCents = if (request.amountFinal.isFinite()) {
            amountInCents(request.amountFinal)
        } else {
            0
        }
        if (confirmedAmountCents <= 0) {
            finishWithError("O valor confirmado para o pagamento e invalido.")
            return
        }

        val retry = pendingCompletion
        if (retry?.idempotencyKey == request.idempotencyKey) {
            retryApprovedPayment(retry)
            return
        }

        terminalPaymentActive = true
        postStep(PaymentStep.PREPARING)
        viewModelScope.launch(Dispatchers.IO) {
            when (
                val prepared = orderRepository.prepareOnlinePayment(
                    orderId = request.order.id,
                    paymentMethod = request.paymentMethod,
                    amountOriginal = request.amount,
                    installments = request.installments,
                    idempotencyKey = request.idempotencyKey,
                )
            ) {
                is Result.Error -> finishWithError(
                    prepared.exception.message ?: "Nao foi possivel preparar o pagamento online.",
                    prepared.exception,
                )
                is Result.Success -> {
                    val preparedAmount = prepared.data.amountFinal
                    if (!preparedAmount.isFinite() ||
                        amountInCents(preparedAmount) != confirmedAmountCents
                    ) {
                        finishWithError(
                            "O backend preparou um total diferente do exibido. " +
                                "Atualize a configuracao antes de tentar novamente.",
                        )
                    } else {
                        startPagBank(request, prepared.data.id, confirmedAmountCents, serial)
                    }
                }
            }
        }
    }

    @Deprecated("Use o fluxo atomico baseado em OrderPaymentRequest")
    fun payOrder(orderId: Int, receivable: OrderReceivableItem, serial: String) {
        _paymentState.postValue(
            UIState.Error(
                "Este fluxo antigo nao pode iniciar pagamentos. Volte para Pedidos e toque em Pagar.",
            ),
        )
    }

    private suspend fun startPagBank(
        request: OrderPaymentRequest,
        attemptId: String,
        amountFinalCents: Int,
        serial: String,
    ) {
        val amountFinal = amountFinalCents / 100.0
        when (val split = orderRepository.updatePaymentAttemptSplitConfig(attemptId, serial)) {
            is Result.Error -> {
                finishWithError("Nao foi possivel configurar a maquininha: ${split.exception.message}", split.exception)
                return
            }
            is Result.Success -> Unit
        }

        if (!plugPag.isAuthenticated()) {
            finishWithError("Nenhum usuario autenticado, contate o suporte.")
            return
        }

        try {
            postStep(PaymentStep.WAITING)
            val result = plugPag.doPayment(
                PlugPagPaymentData(
                    paymentType(request),
                    amountFinalCents,
                    installmentType(request.installments),
                    request.installments,
                    orderUserReference(request.order.id),
                    printReceipt = true,
                    partialPay = false,
                    isCarne = false,
                ),
            )
            if (result.result != PlugPag.RET_OK) {
                saveTransactionLog(request, result, amountFinal)
                finishWithError(terminalFailureMessage(result))
                return
            }

            val approval = PaymentData(
                transactionId = result.transactionId,
                transactionCode = result.transactionCode,
                date = result.date,
                time = result.time,
                cardBrand = result.cardBrand,
                cardLast4 = result.holder,
                cardHolder = result.holderName,
                pixTxIdCode = result.pixTxIdCode,
                transactionLog = Gson().toJson(result),
                amountOriginal = request.amount,
                amountFinal = amountFinal,
            )
            if (approval.transactionId.isNullOrBlank()) {
                finishWithError("A aprovacao PagBank nao retornou transaction_id.")
                return
            }
            saveTransactionLog(request, result, amountFinal)
            val completion = PendingCompletion(request.idempotencyKey, attemptId, approval)
            pendingCompletion = completion
            completeApprovedPayment(completion)
        } catch (_: PlugPagException) {
            finishWithError("Falha no pagamento.")
        } catch (e: Exception) {
            finishWithError(e.message ?: "Erro inesperado", e)
        }
    }

    private fun retryApprovedPayment(completion: PendingCompletion) {
        terminalPaymentActive = false
        postStep(PaymentStep.RECORDING)
        viewModelScope.launch(Dispatchers.IO) { completeApprovedPayment(completion) }
    }

    private suspend fun completeApprovedPayment(completion: PendingCompletion) {
        terminalPaymentActive = false
        postStep(PaymentStep.RECORDING)
        when (
            val result = orderRepository.recordApprovedOnlinePayment(
                completion.attemptId,
                completion.paymentData,
            )
        ) {
            is Result.Success -> {
                pendingCompletion = null
                _paymentState.postValue(UIState.Success(completion.paymentData))
            }
            is Result.Error -> finishWithError(
                result.exception.message ?: "Pagamento aprovado, mas ainda nao registrado. Tente novamente.",
                result.exception,
            )
        }
    }

    private suspend fun saveTransactionLog(
        request: OrderPaymentRequest,
        result: PlugPagTransactionResult,
        amountFinal: Double,
    ) {
        paymentRepository.saveTransaction(
            orderId = request.order.id,
            amount = amountFinal,
            installments = request.installments,
            paymentType = request.paymentMethod.name,
            transactionId = result.transactionId,
            transactionCode = result.transactionCode,
            date = result.date,
            result = result.result,
            cardBrand = result.cardBrand,
            cardLast4 = result.holder,
            cardHolder = result.holderName,
            pixTxIdCode = result.pixTxIdCode,
            message = result.message,
            errorCode = result.errorCode,
        )
    }

    private fun paymentType(request: OrderPaymentRequest): Int = when (
        PaymentTypeRules.normalize(request.paymentMethod.paymentType)
    ) {
        "credito" -> PlugPag.TYPE_CREDITO
        "pix" -> PlugPag.TYPE_PIX
        else -> PlugPag.TYPE_DEBITO
    }

    private fun installmentType(installments: Int): Int = if (installments > 1) {
        PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR
    } else {
        PlugPag.INSTALLMENT_TYPE_A_VISTA
    }

    private fun amountInCents(amount: Double): Int = (amount * 100).roundToInt()

    private fun orderUserReference(orderId: Int): String {
        val digits = orderId.toString().filter { it.isDigit() }
        val prefixed = "PED$digits"
        return if (prefixed.length <= 10) prefixed else digits.takeLast(10)
    }

    private fun terminalFailureMessage(result: PlugPagTransactionResult): String {
        val message = result.message?.trim().orEmpty()
        val errorCode = result.errorCode?.trim().orEmpty()
        return when {
            message.isNotBlank() && errorCode.isNotBlank() -> "$errorCode - $message"
            message.isNotBlank() -> message
            errorCode.isNotBlank() -> "Falha no pagamento. Codigo: $errorCode"
            else -> "Falha no pagamento."
        }
    }

    private fun postStep(step: PaymentStep) {
        _paymentState.postValue(UIState.Loading(step.message))
    }

    private fun finishWithError(message: String, exception: Exception? = null) {
        terminalPaymentActive = false
        _paymentState.postValue(UIState.Error(message, exception))
    }

    fun abortPayment() {
        terminalPaymentActive = false
        viewModelScope.launch(Dispatchers.Default) {
            plugPag.abort()
            plugPag.disposeSubscriber()
        }
    }

    override fun onEvent(data: PlugPagEventData) {
        if (!terminalPaymentActive) return
        val message = data.customMessage.orEmpty().trim()
        _paymentState.postValue(
            UIState.Loading(message.ifBlank { PaymentStep.PROCESSING.message }),
        )
    }
}
